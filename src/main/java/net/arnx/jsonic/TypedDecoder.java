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

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.parse.CompactNumber;
import net.arnx.jsonic.util.ClassUtil;

/**
 * A flat token buffer followed by typed binding. No user constructor or setter
 * runs until the entire document is validated. Container end offsets let binding
 * skip subtrees and resolve duplicate keys before converting their final values.
 */
final class TypedDecoder {
    private static final ClassValue<Boolean> BEAN_TYPES = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            return JSON.isBeanType(type);
        }
    };

    private static final ClassValue<BeanConstructor> CONSTRUCTORS = new ClassValue<>() {
        @Override protected BeanConstructor computeValue(Class<?> type) {
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers())
                    || ((type.isMemberClass() || type.isAnonymousClass())
                        && !Modifier.isStatic(type.getModifiers()))) {
                return new BeanConstructor(null);
            }
            try {
                Constructor<?> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                return new BeanConstructor(constructor);
            } catch (NoSuchMethodException e) {
                // Let the original create method report the original exception.
                return new BeanConstructor(null);
            }
        }
    };

    private static final class BeanConstructor {
        final Constructor<?> constructor;
        BeanConstructor(Constructor<?> constructor) { this.constructor = constructor; }
    }

    private Object[] tokens;
    private int[] ends;
    private int size;
    private int open = -1;
    // Learn at most one layout per document. This retains no input across calls
    // and bounds extra work for heterogeneous or adversarial object layouts.
    private BeanLayout layout;
    private Class<?> repeatingType;

    private static final class BeanLayout {
        final Class<?> type;
        final BeanProperties.WritePlan plan;
        final Constructor<?> constructor;
        final String[] names;
        final BeanProperties.WriteProperty[] properties;

        BeanLayout(Class<?> type, BeanProperties.WritePlan plan, Constructor<?> constructor,
                String[] names, BeanProperties.WriteProperty[] properties) {
            this.type = type;
            this.plan = plan;
            this.constructor = constructor;
            this.names = names;
            this.properties = properties;
        }
    }

    private boolean matches(int at, BeanLayout layout) {
        int i = at + 1;
        for (String name : layout.names) {
            if (i >= ends[at] || !name.equals(tokens[i])) return false;
            i = ends[i + 1];
        }
        return i == ends[at];
    }

    TypedDecoder(int inputLength) { this(inputLength, false); }

    TypedDecoder(int inputLength, Class<?> target) {
        this(inputLength, target == double[].class || target == float[].class
                || target == int[].class || target == long[].class || target == boolean[].class
                || target == String[].class);
    }

    private TypedDecoder(int inputLength, boolean flatScalars) {
        // Estimate only for character input; cap slack for long string values.
        int capacity = Math.min(4096, Math.max(32, inputLength / 4));
        tokens = new Object[capacity];
        ends = flatScalars ? null : new int[capacity];
    }

    // Flat scalar values omit the root slot; restore it before tree binding.
    private void materializeEnds() {
        if (ends != null) return;
        if (size == tokens.length) tokens = Arrays.copyOf(tokens, size * 2);
        System.arraycopy(tokens, 0, tokens, 1, size);
        tokens[0] = JSONEventType.START_ARRAY;
        size++;
        ends = new int[tokens.length];
        ends[0] = open < 0 ? size : -1;
        for (int i = 1; i < size; i++) ends[i] = i + 1;
    }

    static boolean supports(Context context, Class<?> type) {
        // The legacy tree builder is iterative; retain it for unbounded depth.
        if (context.getMaxDepth() > 64 || !BeanProperties.isShared(context)) return false;
        Class<?> element = type;
        while (element.isArray()) element = element.getComponentType();
        // These targets need raw trees, so buffering them would add work.
        if (element.isAssignableFrom(LinkedHashMap.class) || Collection.class.isAssignableFrom(element)) {
            return false;
        }
        return type.isArray() || BEAN_TYPES.get(type);
    }

    boolean isFlat() { return ends == null; }

    void readBufferedScalars(net.arnx.jsonic.parse.JSONParser parser, boolean strings) {
        if (ends != null || size == tokens.length) return;
        size = parser.readBufferedScalars(tokens, size, strings);
    }

    void addFlat(JSONEventType event, Object value) {
        if (ends != null) {
            add(event, value);
            return;
        }
        switch (event) {
        case END_ARRAY:
            open = -1;
            return;
        case WHITESPACE:
        case COMMENT:
            return;
        case START_OBJECT:
        case START_ARRAY:
            if (open >= 0) {
                materializeEnds();
                add(event, value);
                return;
            }
            open = 0;
            return;
        default:
            break;
        }
        if (size == tokens.length) tokens = Arrays.copyOf(tokens, size * 2);
        tokens[size++] = value;
    }

    void add(JSONEventType event, Object value) {
        switch (event) {
        case END_OBJECT:
        case END_ARRAY:
            int parent = ends[open];
            ends[open] = size;
            open = parent;
            return;
        case WHITESPACE:
        case COMMENT:
            return;
        default:
            if (size == tokens.length) {
                tokens = Arrays.copyOf(tokens, size * 2);
                ends = Arrays.copyOf(ends, size * 2);
            }
            if (event == JSONEventType.START_OBJECT || event == JSONEventType.START_ARRAY) {
                tokens[size] = event;
                ends[size] = open;
                open = size;
            } else {
                tokens[size] = value;
                ends[size] = size + 1;
            }
            size++;
        }
    }

    Object convert(Context context, Class<?> type, Type genericType) throws Exception {
        if (ends == null) return array(context, -1, type, genericType);
        return (size == 0) ? context.postparseInternal(null, type, genericType)
                : convert(context, 0, type, genericType);
    }

    private Object convert(Context context, int at, Class<?> type, Type genericType) throws Exception {
        if (context.getHint() == null) {
            // Built-in typed binding only: avoid converter lookup for the common
            // scalar cases while retaining exact integer overflow checks.
            Object scalar = tokens[at];
            if (type == String.class && scalar instanceof String) return scalar;
            if ((type == boolean.class || type == Boolean.class) && scalar instanceof Boolean) return scalar;
            if (scalar instanceof CompactNumber) {
                CompactNumber number = (CompactNumber)scalar;
                if (type == int.class || type == Integer.class) return number.intValueExact();
                if (type == long.class || type == Long.class) return number.longValueExact();
                if (type == double.class || type == Double.class) return number.doubleValue();
                if (type == float.class || type == Float.class) return number.floatValue();
            }
            if (scalar instanceof BigDecimal) {
                if (type == int.class || type == Integer.class) return ((BigDecimal)scalar).intValueExact();
                if (type == double.class || type == Double.class) return ((BigDecimal)scalar).doubleValue();
                if (type == float.class || type == Float.class) return ((BigDecimal)scalar).floatValue();
            }
            if (tokens[at] == JSONEventType.START_ARRAY && type.isArray()) {
                return array(context, at, type, genericType);
            }
            if (tokens[at] == JSONEventType.START_OBJECT && BEAN_TYPES.get(type)) {
                return bean(context, at, type, genericType);
            }
        }
        // Includes untyped values, JSONHint, dates, maps and collection conversions.
        return context.postparseInternal(raw(at), type, genericType);
    }

    private Object array(Context context, int at, Class<?> type, Type genericType) throws Exception {
        int count = 0;
        if (ends == null) count = size;
        else for (int i = at + 1; i < ends[at]; i = ends[i]) count++;
        Class<?> component = type.getComponentType();
        Type elementType = (genericType instanceof GenericArrayType)
                ? ((GenericArrayType)genericType).getGenericComponentType() : component;
        if (component == String.class && elementType == String.class) {
            String[] result = new String[count];
            int index = 0;
            int limit = ends == null ? size : ends[at];
            for (int i = at + 1; i < limit; i = ends == null ? i + 1 : ends[i]) {
                Object value = tokens[i];
                if (value == null || value instanceof String) {
                    // No conversion or user code can fail here, so path tracking
                    // is unnecessary. Other values retain the full conversion path.
                    result[index] = (String)value;
                } else {
                    context.enter(index, null);
                    result[index] = (String)convert(context, i, String.class, String.class);
                    context.exit();
                }
                index++;
            }
            return result;
        }
        if (component == double.class) return doubles(context, at, count);
        if (component == float.class) return floats(context, at, count);
        if (component == int.class) return integers(context, at, count);
        if (component == long.class) return longs(context, at, count);
        if (component == boolean.class) return booleans(context, at, count);
        boolean beanElement = BEAN_TYPES.get(component);
        if (repeatingType == null && count > 1 && beanElement) repeatingType = component;
        Object result = Array.newInstance(component, count);
        Object[] references = (result instanceof Object[]) ? (Object[])result : null;
        int index = 0;
        int limit = ends == null ? size : ends[at];
        for (int i = at + 1; i < limit; i = ends == null ? i + 1 : ends[i]) {
            context.enter(index, null);
            Object value = beanElement && tokens[i] == JSONEventType.START_OBJECT
                    ? bean(context, i, component, elementType) : convert(context, i, component, elementType);
            if (references != null) {
                references[index] = value;
            } else {
                Array.set(result, index, value);
            }
            context.exit();
            index++;
        }
        return result;
    }

    // Direct numeric binding runs no user code. Add the index only on failure
    // or before fallback conversion so diagnostics retain the same path.
    private double[] doubles(Context context, int at, int count) throws Exception {
        double[] result = new double[count];
        int index = 0;
        int limit = ends == null ? size : ends[at];
        for (int i = at + 1; i < limit; i = ends == null ? i + 1 : ends[i]) {
            Object value = tokens[i];
            if (value instanceof CompactNumber || value instanceof BigDecimal) {
                try {
                    result[index] = value instanceof CompactNumber ? ((CompactNumber)value).doubleValue()
                            : ((BigDecimal)value).doubleValue();
                } catch (Exception e) {
                    context.enter(index, null);
                    throw e;
                }
            } else if (value != null) {
                context.enter(index, null);
                if (value instanceof String && context.getNumberFormat() == null) {
                    String text = ((String)value).trim();
                    result[index] = text.isEmpty() ? 0d : Double.parseDouble(text);
                } else {
                    result[index] = (Double)convert(context, i, double.class, double.class);
                }
                context.exit();
            }
            index++;
        }
        return result;
    }

    private float[] floats(Context context, int at, int count) throws Exception {
        float[] result = new float[count];
        int index = 0;
        int limit = ends == null ? size : ends[at];
        for (int i = at + 1; i < limit; i = ends == null ? i + 1 : ends[i]) {
            Object value = tokens[i];
            if (value instanceof CompactNumber || value instanceof BigDecimal) {
                try {
                    result[index] = value instanceof CompactNumber ? ((CompactNumber)value).floatValue()
                            : ((BigDecimal)value).floatValue();
                } catch (Exception e) {
                    context.enter(index, null);
                    throw e;
                }
            } else if (value != null) {
                context.enter(index, null);
                if (value instanceof String && context.getNumberFormat() == null) {
                    String text = ((String)value).trim();
                    result[index] = text.isEmpty() ? 0f : Float.parseFloat(text);
                } else {
                    result[index] = (Float)convert(context, i, float.class, float.class);
                }
                context.exit();
            }
            index++;
        }
        return result;
    }

    private int[] integers(Context context, int at, int count) throws Exception {
        int[] result = new int[count];
        int index = 0;
        int limit = ends == null ? size : ends[at];
        for (int i = at + 1; i < limit; i = ends == null ? i + 1 : ends[i]) {
            Object value = tokens[i];
            if (value instanceof CompactNumber || value instanceof BigDecimal) {
                try {
                    result[index] = value instanceof CompactNumber ? ((CompactNumber)value).intValueExact()
                            : ((BigDecimal)value).intValueExact();
                } catch (Exception e) {
                    context.enter(index, null);
                    throw e;
                }
            } else if (value != null) {
                context.enter(index, null);
                result[index] = (Integer)convert(context, i, int.class, int.class);
                context.exit();
            }
            index++;
        }
        return result;
    }

    private long[] longs(Context context, int at, int count) throws Exception {
        long[] result = new long[count];
        int index = 0;
        int limit = ends == null ? size : ends[at];
        for (int i = at + 1; i < limit; i = ends == null ? i + 1 : ends[i]) {
            Object value = tokens[i];
            if (value instanceof CompactNumber || value instanceof BigDecimal) {
                try {
                    result[index] = value instanceof CompactNumber ? ((CompactNumber)value).longValueExact()
                            : ((BigDecimal)value).longValueExact();
                } catch (Exception e) {
                    context.enter(index, null);
                    throw e;
                }
            } else if (value != null) {
                context.enter(index, null);
                result[index] = (Long)convert(context, i, long.class, long.class);
                context.exit();
            }
            index++;
        }
        return result;
    }

    private boolean[] booleans(Context context, int at, int count) throws Exception {
        boolean[] result = new boolean[count];
        int index = 0;
        int limit = ends == null ? size : ends[at];
        for (int i = at + 1; i < limit; i = ends == null ? i + 1 : ends[i]) {
            Object value = tokens[i];
            if (value instanceof Boolean) {
                result[index] = (Boolean)value;
            } else if (value != null) {
                context.enter(index, null);
                result[index] = (Boolean)convert(context, i, boolean.class, boolean.class);
                context.exit();
            }
            index++;
        }
        return result;
    }

    private Object bean(Context context, int at, Class<?> type, Type genericType) throws Exception {
        BeanLayout known = layout;
        if (known != null && known.type == type && matches(at, known)) {
            Object result = known.constructor != null ? known.constructor.newInstance() : context.createInternal(type);
            if (result == null) return null;
            int token = at + 1;
            for (BeanProperties.WriteProperty property : known.properties) {
                assign(context, result, property, token + 1, type, genericType);
                token = ends[token + 1];
            }
            return result;
        }
        BeanProperties.WritePlan plan = known != null && known.type == type
                ? known.plan : BeanProperties.writePlan(context, type);
        int length = plan.indexed.length;
        // Bound scratch space for very wide beans with sparse input.
        if (length > 64) return context.postparseInternal(raw(at), type, genericType);
        int[] slots = new int[length * 2];
        int count = 0;
        boolean learn = layout == null && type == repeatingType;
        for (int i = at + 1; i < ends[at]; i = ends[i + 1]) {
            BeanProperties.WriteProperty property = plan.properties.get(tokens[i]);
            if (property == null) {
                learn = false;
                // Aliases may assign the same property more than once. Keep their
                // exact-key duplicate semantics in the original converter.
                if (!(tokens[i] instanceof String) || plan.properties.containsKey(
                        ObjectConverter.toLowerCamel(context, (String)tokens[i]))) {
                    return context.postparseInternal(raw(at), type, genericType);
                }
                // Syntax has already been validated, and this name is ignored.
                continue;
            }
            if (slots[property.index] == 0) slots[length + count++] = property.index;
            else learn = false;
            slots[property.index] = i + 1;
        }
        Constructor<?> constructor = CONSTRUCTORS.get(type).constructor;
        if (learn && count > 0) {
            String[] names = new String[count];
            BeanProperties.WriteProperty[] ordered = new BeanProperties.WriteProperty[count];
            for (int i = 0; i < count; i++) {
                ordered[i] = plan.indexed[slots[length + i]];
                names[i] = (String)tokens[slots[ordered[i].index] - 1];
            }
            layout = new BeanLayout(type, plan, constructor, names, ordered);
        }
        Object result = (constructor != null) ? constructor.newInstance() : context.createInternal(type);
        if (result == null) return null;
        for (int i = 0; i < count; i++) {
            BeanProperties.WriteProperty property = plan.indexed[slots[length + i]];
            assign(context, result, property, slots[property.index], type, genericType);
        }
        return result;
    }

    private void assign(Context context, Object result, BeanProperties.WriteProperty property,
            int at, Class<?> type, Type genericType) throws Exception {
        context.enter(property.property.getName(), property.hint);
        if (property.primitiveField != null && assignPrimitive(result, property, tokens[at])) {
            context.exit();
            return;
        }
        Type targetType = property.genericType;
        Class<?> targetClass = property.type;
        if (targetType != targetClass && genericType instanceof ParameterizedType) {
            targetType = context.getResolvedType(genericType, type, targetType);
            targetClass = ClassUtil.getRawType(targetType);
        }
        property.property.set(result, convert(context, at, targetClass, targetType));
        context.exit();
    }

    // Use primitive reflection only for actual fields, never in place of a
    // setter or a hinted conversion. Keep the path active if conversion fails.
    private boolean assignPrimitive(Object result, BeanProperties.WriteProperty property, Object value) {
        try {
            if (value instanceof CompactNumber) {
                CompactNumber number = (CompactNumber)value;
                if (property.type == int.class) property.primitiveField.setInt(result, number.intValueExact());
                else if (property.type == long.class) property.primitiveField.setLong(result, number.longValueExact());
                else if (property.type == double.class) property.primitiveField.setDouble(result, number.doubleValue());
                else if (property.type == float.class) property.primitiveField.setFloat(result, number.floatValue());
                else return false;
            } else if (value instanceof BigDecimal) {
                BigDecimal number = (BigDecimal)value;
                if (property.type == int.class) property.primitiveField.setInt(result, number.intValueExact());
                else if (property.type == double.class) property.primitiveField.setDouble(result, number.doubleValue());
                else if (property.type == float.class) property.primitiveField.setFloat(result, number.floatValue());
                else return false;
            } else if (value instanceof Boolean && property.type == boolean.class) {
                property.primitiveField.setBoolean(result, (Boolean)value);
            } else {
                return false;
            }
            return true;
        } catch (IllegalAccessException e) {
            // Match PropertyInfo.set's wrapping of reflective access failures.
            throw new IllegalStateException(e);
        }
    }

    Object raw() {
        materializeEnds();
        return (size == 0) ? null : raw(0);
    }

    private Object raw(int at) {
        if (tokens[at] == JSONEventType.START_OBJECT || tokens[at] == JSONEventType.START_ARRAY) materializeEnds();
        if (tokens[at] == JSONEventType.START_OBJECT) {
            Map<Object, Object> result = new LinkedHashMap<>();
            for (int i = at + 1; i < ends[at]; i = ends[i + 1]) {
                result.put(tokens[i], raw(i + 1));
            }
            return result;
        } else if (tokens[at] == JSONEventType.START_ARRAY) {
            List<Object> result = new ArrayList<>();
            for (int i = at + 1; i < ends[at]; i = ends[i]) result.add(raw(i));
            return result;
        }
        return tokens[at] instanceof CompactNumber ? ((CompactNumber)tokens[at]).decimalValue() : tokens[at];
    }
}
