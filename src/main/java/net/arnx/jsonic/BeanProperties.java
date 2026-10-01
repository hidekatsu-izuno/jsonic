/*
 * Copyright 2014 Hidekatsu Izuno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.arnx.jsonic;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.io.StringBuilderOutputSource;
import net.arnx.jsonic.util.PropertyInfo;

/**
 * Immutable property plans for the built-in JSON behavior. The cache holds no
 * Context or JSON instance. ClassValue ties each plan to its bean class lifetime.
 * Custom JSON subclasses and stateful naming styles keep per-call discovery.
 */
final class BeanProperties {
    private static final NamingStyle[] STYLES = {
        NamingStyle.NOOP, NamingStyle.LOWER_CASE, NamingStyle.LOWER_CAMEL,
        NamingStyle.LOWER_SPACE, NamingStyle.LOWER_HYPHEN, NamingStyle.LOWER_UNDERSCORE,
        NamingStyle.UPPER_CASE, NamingStyle.UPPER_CAMEL, NamingStyle.UPPER_SPACE,
        NamingStyle.UPPER_HYPHEN, NamingStyle.UPPER_UNDERSCORE
    };

    private static final ClassValue<Plans> CACHE = new ClassValue<Plans>() {
        @Override protected Plans computeValue(Class<?> type) {
            return new Plans();
        }
    };

    private BeanProperties() {
    }

    private static int styleIndex(Context context) {
        if (!context.hasDefaultBeanBehavior()) return -1;
        NamingStyle style = context.getPropertyStyle();
        if (style == null) return 0;
        for (int i = 0; i < STYLES.length; i++) {
            if (style == STYLES[i]) return i;
        }
        return -1;
    }

    static ReadProperty[] readable(Context context, Class<?> type) throws IOException {
        int index = styleIndex(context);
        Plans plans = (index >= 0) ? CACHE.get(type) : null;
        ReadProperty[] result = (plans != null) ? plans.read.get(index) : null;
        if (result == null) {
            PropertyInfo[] properties = ObjectFormatter.getGetProperties(context, type);
            result = new ReadProperty[properties.length];
            for (int i = 0; i < properties.length; i++) {
                result[i] = new ReadProperty(context, properties[i]);
            }
            if (plans != null && !plans.read.compareAndSet(index, null, result)) {
                result = plans.read.get(index);
            }
        }
        return result;
    }

    static boolean isShared(Context context) {
        return styleIndex(context) >= 0;
    }

    static Map<String, WriteProperty> writable(Context context, Class<?> type) {
        return writePlan(context, type).properties;
    }

    static WritePlan writePlan(Context context, Class<?> type) {
        int index = styleIndex(context);
        Plans plans = (index >= 0) ? CACHE.get(type) : null;
        WritePlan result = (plans != null) ? plans.write.get(index) : null;
        if (result == null) {
            Map<String, PropertyInfo> properties = ObjectConverter.getSetProperties(context, type);
            Map<String, WriteProperty> values = new HashMap<String, WriteProperty>();
            WriteProperty[] indexed = new WriteProperty[properties.size()];
            int i = 0;
            for (Map.Entry<String, PropertyInfo> entry : properties.entrySet()) {
                WriteProperty property = new WriteProperty(entry.getValue(), i);
                values.put(entry.getKey(), property);
                indexed[i++] = property;
            }
            result = new WritePlan(Collections.unmodifiableMap(values), indexed);
            if (plans != null && !plans.write.compareAndSet(index, null, result)) {
                result = plans.write.get(index);
            }
        }
        return result;
    }

    static final class WritePlan {
        final Map<String, WriteProperty> properties;
        final WriteProperty[] indexed;

        WritePlan(Map<String, WriteProperty> properties, WriteProperty[] indexed) {
            this.properties = properties;
            this.indexed = indexed;
        }
    }

    private static final class Plans {
        final AtomicReferenceArray<ReadProperty[]> read =
                new AtomicReferenceArray<ReadProperty[]>(STYLES.length);
        final AtomicReferenceArray<WritePlan> write =
                new AtomicReferenceArray<WritePlan>(STYLES.length);
    }

    static final class ReadProperty {
        final PropertyInfo property;
        final String name;
        final String quotedName;
        final JSONHint hint;
        final Type genericType;

        ReadProperty(Context context, PropertyInfo property) throws IOException {
            this.property = property;
            name = property.getName();
            hint = property.getReadAnnotation(JSONHint.class);
            genericType = property.getReadGenericType();
            StringBuilderOutputSource out = new StringBuilderOutputSource(name.length() + 2);
            StringFormatter.serialize(context, name, out);
            quotedName = out.toString();
        }
    }

    static final class WriteProperty {
        final PropertyInfo property;
        final JSONHint hint;
        final Class<?> type;
        final Type genericType;

        final int index;
        final Field primitiveField;

        WriteProperty(PropertyInfo property, int index) {
            this.index = index;
            this.property = property;
            hint = property.getWriteAnnotation(JSONHint.class);
            type = property.getWriteType();
            genericType = property.getWriteGenericType();
            Field field = property.getField();
            primitiveField = hint == null && property.getWriteMethod() == null
                    && field != null && !Modifier.isFinal(field.getModifiers())
                    && (type == int.class || type == long.class || type == double.class || type == boolean.class)
                    ? field : null;
        }
    }
}
