/*
 * Copyright 2026 Hidekatsu Izuno
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.util.ClassUtil;
import net.arnx.jsonic.util.PropertyInfo;

/** Component access and canonical construction for immutable records. */
final class RecordSupport {
    private static final ClassValue<Metadata> CACHE = new ClassValue<>() {
        @Override
        protected Metadata computeValue(Class<?> type) {
            try {
                RecordComponent[] components = type.getRecordComponents();
                Class<?>[] types = new Class<?>[components.length];
                for (int i = 0; i < components.length; i++) {
                    types[i] = components[i].getType();
                }
                return new Metadata(components, type.getDeclaredConstructor(types));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    private record Metadata(RecordComponent[] components, Constructor<?> constructor) {}

    private RecordSupport() {
    }

    private static Type resolve(Context context, Type parent, Class<?> owner, Type type) {
        Type resolved = context.getResolvedType(parent, owner, type);
        if (resolved instanceof ParameterizedType parameterized) {
            Type[] args = parameterized.getActualTypeArguments().clone();
            for (int i = 0; i < args.length; i++) args[i] = resolve(context, parent, owner, args[i]);
            return new ParameterizedType() {
                @Override public Type[] getActualTypeArguments() { return args.clone(); }
                @Override public Type getRawType() { return parameterized.getRawType(); }
                @Override public Type getOwnerType() { return parameterized.getOwnerType(); }
                @Override public boolean equals(Object other) {
                    return other instanceof ParameterizedType p
                            && getRawType().equals(p.getRawType())
                            && java.util.Objects.equals(getOwnerType(), p.getOwnerType())
                            && Arrays.equals(args, p.getActualTypeArguments());
                }
                @Override public int hashCode() {
                    return Arrays.hashCode(args) ^ getRawType().hashCode()
                            ^ java.util.Objects.hashCode(getOwnerType());
                }
            };
        } else if (resolved instanceof GenericArrayType array) {
            Type component = resolve(context, parent, owner, array.getGenericComponentType());
            if (component instanceof Class<?> cls) return Array.newInstance(cls, 0).getClass();
            return new GenericArrayType() {
                @Override public Type getGenericComponentType() { return component; }
                @Override public boolean equals(Object other) {
                    return other instanceof GenericArrayType a
                            && component.equals(a.getGenericComponentType());
                }
                @Override public int hashCode() { return component.hashCode(); }
            };
        }
        return resolved;
    }

    private static PropertyInfo property(Context context, Class<?> type, RecordComponent component) {
        Method accessor = component.getAccessor();
        Field field;
        try {
            field = type.getDeclaredField(component.getName());
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
        if (context.ignoreInternal(type, accessor) || context.ignoreInternal(type, field)) return null;
        JSONHint hint = accessor.getAnnotation(JSONHint.class);
        if (hint == null) hint = field.getAnnotation(JSONHint.class);
        if (hint != null && hint.ignore()) return null;
        String name;
        if (hint != null && !hint.name().isEmpty()) {
            name = hint.name();
        } else {
            name = context.normalizeInternal(component.getName());
            if (context.getPropertyStyle() != null) name = context.getPropertyStyle().to(name);
        }
        accessor.setAccessible(true);
        return new PropertyInfo(type, name, field, accessor, null, false,
                hint != null ? hint.ordinal() : -1) {
            @Override public <T extends Annotation> T getReadAnnotation(Class<T> annotation) {
                T result = super.getReadAnnotation(annotation);
                return result != null ? result : getField().getAnnotation(annotation);
            }
        };
    }

    static PropertyInfo[] readable(Context context, Class<?> type) {
        ArrayList<PropertyInfo> properties = new ArrayList<>();
        for (RecordComponent component : CACHE.get(type).components) {
            PropertyInfo property = property(context, type, component);
            if (property != null) properties.add(property);
        }
        PropertyInfo[] result = properties.toArray(PropertyInfo[]::new);
        Arrays.sort(result);
        return result;
    }

    static Object convert(Context context, Object value, Class<?> type, Type genericType) throws Exception {
        if (value == null) return null;
        Map<?, ?> source;
        if (value instanceof Map<?, ?> map) {
            source = map;
        } else if (!(value instanceof java.util.List<?>) && context.getHint() != null
                && !context.getHint().anonym().isEmpty()) {
            source = java.util.Collections.singletonMap(context.getHint().anonym(), value);
        } else {
            throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + genericType);
        }
        Metadata metadata = CACHE.get(type);
        PropertyInfo[] properties = new PropertyInfo[metadata.components.length];
        Map<String, Integer> indices = new HashMap<>();
        for (int i = 0; i < properties.length; i++) {
            properties[i] = property(context, type, metadata.components[i]);
            if (properties[i] != null) indices.put(properties[i].getName(), i);
        }
        Object[] values = new Object[properties.length];
        boolean[] supplied = new boolean[values.length];
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String name = entry.getKey().toString();
            Integer index = indices.get(name);
            if (index == null) index = indices.get(ObjectConverter.toLowerCamel(context, name));
            if (index == null) continue;
            values[index] = entry.getValue();
            supplied[index] = true;
        }
        for (int i = 0; i < values.length; i++) {
            RecordComponent component = metadata.components[i];
            PropertyInfo property = properties[i];
            // Missing and ignored components receive Java defaults. Explicit null
            // still goes through the normal converters (including Optional).
            if (!supplied[i] && !component.getType().isPrimitive()) continue;
            JSONHint hint = supplied[i] ? property.getReadAnnotation(JSONHint.class) : null;
            context.enter(property != null ? property.getName() : component.getName(), hint);
            Type resolved = resolve(context, genericType, type, component.getGenericType());
            values[i] = context.postparseInternal(values[i], ClassUtil.getRawType(resolved), resolved);
            context.exit();
        }
        metadata.constructor.setAccessible(true);
        return metadata.constructor.newInstance(values);
    }
}
