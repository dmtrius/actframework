package act.inject.param;

/*-
 * #%L
 * ACT Framework
 * %%
 * Copyright (C) 2014 - 2017 ActFramework
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
import act.data.Sensitive;
import act.util.ActContext;
import org.osgl.$;
import org.osgl.inject.InjectException;

import java.lang.reflect.Field;

/**
 * Load instance loaded by {@link ParamValueLoader} into {@link java.lang.reflect.Field}
 */
class FieldLoader {
    private final Field field;
    private final ParamValueLoader loader;
    private boolean sensitive;

    FieldLoader(Field field, ParamValueLoader loader) {
        this.sensitive = String.class == field.getType() && null != field.getAnnotation(Sensitive.class);
        this.field = field;
        this.loader = $.requireNotNull(loader);
    }

    public void applyTo($.Func0<Object> beanSource, ActContext context) {
        Object fieldValue = loader.load(null, context, true);
        // #429 ensure POJO instance get initialized
        Object bean = beanSource.apply();
        if (null == fieldValue) {
            beanSource.apply();
            return;
        }
        try {
            field.set(bean, sensitive ? Act.crypto().encrypt((String)fieldValue) : fieldValue);
        } catch (Exception e) {
            throw new InjectException(e);
        }
    }
}
