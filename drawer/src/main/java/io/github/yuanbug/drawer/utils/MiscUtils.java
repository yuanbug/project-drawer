package io.github.yuanbug.drawer.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author yuanbug
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("unchecked")
public final class MiscUtils {

    public static String getSimpleName(String typeName) {
        int docPosition = typeName.lastIndexOf(".");
        if (docPosition == -1) {
            return typeName;
        }
        return typeName.substring(docPosition + 1);
    }

    public static <T> Class<T> castClass(Class<?> type) {
        return (Class<T>) type;
    }

    public static <T> T castClass(Object object) {
        return (T) object;
    }

}
