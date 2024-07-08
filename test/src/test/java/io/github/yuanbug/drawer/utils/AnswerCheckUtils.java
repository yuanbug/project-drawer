package io.github.yuanbug.drawer.utils;

import io.github.yuanbug.drawer.domain.info.MethodCalling;
import io.github.yuanbug.drawer.domain.info.MethodInfo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yuanbug
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnswerCheckUtils {

    public static void check(MethodInfo methodInfo, String fileName) {
        check(methodInfo, loadAnswer(fileName));
    }

    @SneakyThrows
    private static MethodInfo loadAnswer(String fileName) {
        try (InputStream inputStream = getInputStream(fileName)) {
            return JacksonUtils.OBJECT_MAPPER.readValue(inputStream, MethodInfo.class);
        }
    }

    private static InputStream getInputStream(String fileName) {
        return Optional.ofNullable(AnswerCheckUtils.class.getClassLoader())
                .map(classLoader -> classLoader.getResourceAsStream(fileName))
                .orElseThrow();
    }

    private static void check(MethodInfo methodInfo, MethodInfo expected) {
        assertNotNull(methodInfo);
        assertEquals(expected.getId(), methodInfo.getId());
        checkList(expected.getDependencies(), methodInfo.getDependencies(), AnswerCheckUtils::getId, AnswerCheckUtils::check);
        checkList(expected.getOverrides(), methodInfo.getOverrides(), info -> info.getId().toString(), AnswerCheckUtils::check);
    }

    private static String getId(MethodCalling calling) {
        if (StringUtils.isNotBlank(calling.getRecursiveAt())) {
            return calling.getRecursiveAt();
        }
        return Objects.requireNonNull(calling.getCallee()).getId().toString();
    }

    private static void check(MethodCalling expected, MethodCalling actual) {
        assertNotNull(actual);
        assertEquals(expected.getCallingType(), actual.getCallingType());
        if (StringUtils.isNotBlank(expected.getRecursiveAt())) {
            assertEquals(expected.getRecursiveAt(), actual.getRecursiveAt());
        } else {
            assertNotNull(actual.getCallee());
            check(expected.getCallee(), actual.getCallee());
        }
    }

    private static <T> void checkList(List<T> expectedElements, List<T> actualElements, Function<T, String> idGetter, BiConsumer<T, T> checker) {
        if (CollectionUtils.isEmpty(expectedElements)) {
            assertTrue(CollectionUtils.isEmpty(actualElements));
            return;
        }
        assertEquals(expectedElements.size(), actualElements.size());

        Map<String, T> actualElementsById = actualElements.stream().collect(Collectors.toMap(idGetter, Function.identity()));
        for (T expectedElement : expectedElements) {
            checker.accept(expectedElement, actualElementsById.get(idGetter.apply(expectedElement)));
        }
    }

}
