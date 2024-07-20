package io.github.yuanbug.drawer.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author yuanbug
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unchecked")
public final class MiscUtils {

    /**
     * <li> "xxx.yyyy.A" -> "A"
     * <li> "xxx.yyyy.A$B" -> "A$B"
     */
    public static String getSimpleName(String typeName) {
        return typeName.substring(typeName.lastIndexOf(".") + 1);
    }

    public static <T> Class<T> castClass(Class<?> type) {
        return (Class<T>) type;
    }

    public static <T> T castClass(Object object) {
        return (T) object;
    }

    public static <T> boolean isPresentAnd(T data, Predicate<T> filter) {
        if (null == data) {
            return false;
        }
        return filter.test(data);
    }

    public static <K, V, M extends Map<K, V>> M merge(Supplier<? extends M> supplier, Map<K, V>... maps) {
        M map = supplier.get();
        for (Map<K, V> source : maps) {
            map.putAll(source);
        }
        return map;
    }

}
