package io.github.yuanbug.drawer.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Predicate;

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

}
