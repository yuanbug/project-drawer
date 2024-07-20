package io.github.yuanbug.drawer.maven;

import io.github.yuanbug.drawer.domain.DependencyJarFile;
import io.github.yuanbug.drawer.parser.maven.MavenCommandInvoker;
import io.github.yuanbug.drawer.utils.JacksonUtils;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static io.github.yuanbug.drawer.TestConstants.POM_PATH;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author yuanbug
 */
public class MavenCommandInvokerTest {

    private final MavenCommandInvoker invoker = new MavenCommandInvoker();

    @Test
    void testDependencyParse() {
        var jars = invoker.parseCompileDependencies(POM_PATH.toFile())
                .stream()
                .collect(Collectors.groupingBy(DependencyJarFile::getArtifactId));
        JacksonUtils.println(jars);
        assertFalse(jars.isEmpty());
        assertTrue(jars.containsKey("tomcat-embed-core"));
        assertTrue(jars.containsKey("jackson-databind"));
        assertTrue(jars.containsKey("jackson-core"));
    }

}
