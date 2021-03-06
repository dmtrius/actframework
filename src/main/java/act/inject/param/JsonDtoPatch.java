package act.inject.param;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2018 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import act.Act;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.inject.ValueLoader;
import org.osgl.inject.annotation.LoadValue;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.util.*;
import javax.persistence.Transient;

/**
 * Patch JSON DTO bean object in case there are
 * value loader annotation on certain field, and the
 * value of the field on the bean is null, then it
 * shall inject the value loader result into the field
 * on the bean.
 *
 * See https://github.com/actframework/actframework/issues/1016
 */
public class JsonDtoPatch {
    private List<JsonDtoPatch> fieldsPatches = new ArrayList<>();
    private String name;
    private ValueLoader loader;
    private JsonDtoPatch(String name, BeanSpec spec) {
        this.name = S.requireNotBlank(name);
        this.loader = valueLoaderOf(spec);
        if (null == loader) {
            for (BeanSpec fieldSpec : spec.nonStaticFields()) {
                Class fieldType = fieldSpec.rawType();
                if (fieldSpec.isTransient() || fieldSpec.hasAnnotation(Transient.class)) {
                    continue;
                }
                if (Collection.class.isAssignableFrom(fieldType) || Map.class.isAssignableFrom(fieldType)) {
                    continue;
                }
                String fieldName = fieldSpec.name();
                JsonDtoPatch child = new JsonDtoPatch(fieldName, fieldSpec);
                if (!child.isEmpty()) {
                    fieldsPatches.add(child);
                }
            }
        }
    }

    public String name() {
        return name;
    }

    public void applyChildren(Object host) {
        for (JsonDtoPatch child : fieldsPatches) {
            child.apply(host);
        }
    }

    public void apply(Object host) {
        if (null != loader) {
            Object o = loader.get();
            $.setProperty(host, o, name);
        } else {
            Object o = $.getProperty(host, name);
            applyChildren(o);
        }
    }

    private boolean isEmpty() {
        return null == loader && fieldsPatches.isEmpty();
    }

    private ValueLoader valueLoaderOf(BeanSpec spec) {
        LoadValue loadValue = spec.getAnnotation(LoadValue.class);
        if (null == loadValue) {
            Annotation[] aa = spec.taggedAnnotations(LoadValue.class);
            if (aa.length > 0) {
                Annotation a = aa[0];
                loadValue = a.annotationType().getAnnotation(LoadValue.class);
            }
        }
        return null == loadValue ? null : Act.getInstance(loadValue.value());
    }

    public static JsonDtoPatch of(BeanSpec spec) {
        JsonDtoPatch patch = new JsonDtoPatch(spec.name(), spec);
        return patch.isEmpty() ? null : patch;
    }
}
