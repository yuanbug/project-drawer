package io.github.yuanbug.drawer.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author yuanbug
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectUtils {

    public static Field getFieldByName(String name, Class<?> type) {
        if (StringUtils.isBlank(name) || null == type) {
            return null;
        }
        return findInInheritLink(
                type,
                false,
                field -> name.equals(field.getName()),
                currentType -> Arrays.asList(currentType.getDeclaredFields())
        );
    }

    public static Set<Class<?>> getAllSuperType(Class<?> type) {
        Set<Class<?>> result = new HashSet<>(4);
        addAllSuperType(type, result);
        return Collections.unmodifiableSet(result);
    }

    private static void addAllSuperType(Class<?> type, Set<Class<?>> result) {
        if (null == type || result.contains(type)) {
            return;
        }
        Class<?> superclass = type.getSuperclass();
        if (null != superclass) {
            result.add(superclass);
            addAllSuperType(superclass, result);
        }
        for (Class<?> interfaceType : type.getInterfaces()) {
            result.add(interfaceType);
            addAllSuperType(interfaceType, result);
        }
    }

    public static <T> T findInInheritLink(Class<?> type, boolean superOnly, Predicate<T> filter, Function<Class<?>, List<T>> getter) {
        if (null == type) {
            return null;
        }
        // 先在类和父类里面找
        Class<?> current = superOnly ? type.getSuperclass() : type;
        while (null != current) {
            List<T> elements = getter.apply(current);
            for (T element : elements) {
                if (filter.test(element)) {
                    return element;
                }
            }
            current = current.getSuperclass();
        }
        // 再找接口
        var interfaces = findAllInterfaces(type);
        for (Class<?> interfaceType : interfaces) {
            List<T> elements = getter.apply(interfaceType);
            for (T element : elements) {
                if (filter.test(element)) {
                    return element;
                }
            }
        }
        return null;
    }

    public static List<Class<?>> findAllInterfaces(Class<?> type) {
        Set<Class<?>> checked = new HashSet<>(4);
        Set<Class<?>> result = new HashSet<>(4);
        Queue<Class<?>> queue = new LinkedList<>();
        queue.add(type);
        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            if (checked.contains(current)) {
                continue;
            }
            checked.add(current);
            List<Class<?>> interfaces = Arrays.asList(current.getInterfaces());
            result.addAll(interfaces);
            queue.addAll(interfaces);
            Optional.ofNullable(current.getSuperclass()).ifPresent(queue::add);
        }
        return new ArrayList<>(result);
    }

}
