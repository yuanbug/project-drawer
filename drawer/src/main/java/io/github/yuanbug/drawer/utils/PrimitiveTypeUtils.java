package io.github.yuanbug.drawer.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author yuanbug
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PrimitiveTypeUtils {

    private static final BiMap<Class<?>, Class<?>> WRAPPER_CLASS_MAP = ImmutableBiMap.<Class<?>, Class<?>>builder()
            .put(int.class, Integer.class)
            .put(long.class, Long.class)
            .put(short.class, Short.class)
            .put(byte.class, Byte.class)
            .put(char.class, Character.class)
            .put(double.class, Double.class)
            .put(float.class, Float.class)
            .put(boolean.class, Boolean.class)
            .build();

    private static final BiMap<Class<?>, Class<?>> PRIMITIVE_CLASS_MAP = WRAPPER_CLASS_MAP.inverse();

    private static final Map<String, Class<?>> PRIMITIVE_NAME_TO_CLASS = WRAPPER_CLASS_MAP.keySet().stream().collect(Collectors.toUnmodifiableMap(
            Class::getName, Function.identity()
    ));

    private static final Map<Class<?>, Set<Class<?>>> COULD_AUTO_CAST_FROM = ImmutableMap.<Class<?>, Set<Class<?>>>builder()
            .put(long.class, Set.of(int.class, short.class, byte.class, char.class))
            .put(int.class, Set.of(short.class, byte.class, char.class))
            .put(short.class, Set.of(byte.class))
            .put(float.class, Set.of(long.class, int.class, short.class, byte.class, char.class))
            .put(double.class, Set.of(long.class, float.class, int.class, short.class, byte.class, char.class))
            .build();

    private static final Map<Class<?>, Object> ZERO_VALUE_MAP = ImmutableMap.<Class<?>, Object>builder()
            .put(int.class, 0)
            .put(long.class, 0L)
            .put(short.class, (short) 0)
            .put(byte.class, (byte) 0)
            .put(char.class, (char) 0)
            .put(double.class, 0.0)
            .put(float.class, 0.0f)
            .put(boolean.class, false)
            .build();

    private static final Map<Class<?>, Function<String, Object>> STRING_PARSER_MAP = ImmutableMap.<Class<?>, Function<String, Object>>builder()
            .put(int.class, Integer::parseInt)
            .put(long.class, Long::parseLong)
            .put(short.class, Short::parseShort)
            .put(byte.class, Byte::parseByte)
            .put(char.class, value -> {
                if (StringUtils.isBlank(value)) {
                    return (char) 0;
                }
                if (value.length() == 1) {
                    return value.toCharArray()[0];
                }
                throw new IllegalArgumentException(String.format("%s无法转换为字符", value));
            })
            .put(double.class, Double::parseDouble)
            .put(float.class, Float::parseFloat)
            .put(boolean.class, Boolean::parseBoolean)
            .build();

    private static final Set<Class<?>> INTEGER_LIKE = Set.of(
            Long.class, Integer.class, Short.class, Byte.class, Character.class,
            long.class, int.class, short.class, byte.class, char.class
    );

    private static final Set<Class<?>> FLOAT_LIKE = Set.of(Float.class, Double.class, float.class, double.class);

    private static final Set<Class<?>> NUMBER_LIKE = Sets.union(INTEGER_LIKE, FLOAT_LIKE);

    public static void assertIsPrimitive(Class<?> primitiveClass) {
        if (!primitiveClass.isPrimitive()) {
            throw new IllegalArgumentException(String.format("%s不是基本类型", primitiveClass.getName()));
        }
    }

    public static boolean isPrimitiveWrapper(Class<?> javaClass) {
        return WRAPPER_CLASS_MAP.containsValue(javaClass);
    }

    public static boolean isPrimitiveTypeName(String className) {
        return PRIMITIVE_NAME_TO_CLASS.containsKey(className);
    }

    public static Class<?> getPrimitiveTypeByName(String className) {
        return Optional.ofNullable(className)
                .map(PRIMITIVE_NAME_TO_CLASS::get)
                .orElseThrow(() -> new IllegalArgumentException("不存在基本类型" + className));
    }

    public static Class<?> getWrapperClassByPrimitiveName(String primitiveTypeName) {
        return getWrapperClass(getPrimitiveTypeByName(primitiveTypeName));
    }

    public static Class<?> getPrimitiveClass(Class<?> wrapperClass) {
        return PRIMITIVE_CLASS_MAP.get(wrapperClass);
    }

    public static Class<?> getWrapperClass(Class<?> primitiveClass) {
        assertIsPrimitive(primitiveClass);
        return WRAPPER_CLASS_MAP.get(primitiveClass);
    }

    public static Object getZeroValue(Class<?> primitiveClass) {
        assertIsPrimitive(primitiveClass);
        return ZERO_VALUE_MAP.get(primitiveClass);
    }

    public static Object parseString(String value, Class<?> primitiveClass) {
        assertIsPrimitive(primitiveClass);
        return STRING_PARSER_MAP.get(primitiveClass).apply(value);
    }

    public static boolean isCompatiblePrimitiveType(Class<?> expected, Class<?> given) {
        if (!expected.isPrimitive() || !given.isPrimitive()) {
            return false;
        }
        if (expected.equals(given)) {
            return true;
        }
        Set<Class<?>> compatibles = COULD_AUTO_CAST_FROM.get(expected);
        if (null == compatibles) {
            return false;
        }
        return compatibles.contains(given);
    }

    public static boolean isIntegerLike(Class<?> type) {
        if (null == type) {
            return false;
        }
        return INTEGER_LIKE.contains(type);
    }

    public static boolean isFloatLike(Class<?> type) {
        if (null == type) {
            return false;
        }
        return FLOAT_LIKE.contains(type);
    }

    public static boolean isNumberLike(Class<?> type) {
        if (null == type) {
            return false;
        }
        return NUMBER_LIKE.contains(type);
    }

}
