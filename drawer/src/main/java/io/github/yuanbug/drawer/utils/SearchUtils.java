package io.github.yuanbug.drawer.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.*;

/**
 * @author yuanbug
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SearchUtils {

    public static <T, R> R bfs(T root,
                               BiFunction<T, R, R> accumulator,
                               Function<T, List<T>> subNodeGetter,
                               Supplier<R> defaultResult,
                               BiPredicate<T, R> continuePredicate) {
        R result = defaultResult.get();
        Queue<T> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            T node = queue.poll();
            result = accumulator.apply(node, result);
            if (!continuePredicate.test(node, result)) {
                break;
            }
            subNodeGetter.apply(node).forEach(queue::offer);
        }
        return result;
    }

    public static <T> List<T> bfsAll(T root, Predicate<T> predicate, Function<T, List<T>> subNodeGetter) {
        return bfs(
                root,
                (node, list) -> {
                    if (predicate.test(node)) {
                        list.add(node);
                    }
                    return list;
                },
                subNodeGetter,
                ArrayList::new,
                (node, list) -> true
        );
    }

}
