package io.github.yuanbug.drawer.parser;

import io.github.yuanbug.drawer.BaseTest;
import io.github.yuanbug.drawer.config.AstParsingConfig;
import io.github.yuanbug.drawer.domain.CodeModule;
import io.github.yuanbug.drawer.domain.info.MethodId;
import io.github.yuanbug.drawer.domain.info.MethodInfo;
import io.github.yuanbug.drawer.parser.maven.MavenCommandInvoker;
import io.github.yuanbug.drawer.test.simple.SimpleUtils;
import io.github.yuanbug.drawer.utils.AnswerCheckUtils;
import io.github.yuanbug.drawer.utils.JacksonUtils;
import org.junit.jupiter.api.Test;

import static io.github.yuanbug.drawer.TestConstants.CODE_PATH;
import static io.github.yuanbug.drawer.TestConstants.POM_PATH;

/**
 * @author yuanbug
 */
public class JarLoadTest extends BaseTest {

    @Override
    public AstParsingConfig defaultProjectDrawerConfig() {
        return new TestAstParsingConfig() {
            @Override
            protected CodeModule buildTestCaseModule() {
                return CodeModule.builder()
                        .name("test-cases")
                        .srcMainJavaPath(CODE_PATH)
                        .compileDependencyJars(new MavenCommandInvoker().parseCompileDependencies(POM_PATH.toFile()))
                        .build();
            }
        };
    }

    @Test
    void testParseWithJar() throws NoSuchMethodException {
        MethodInfo methodInfo = methodParser.parseMethod(MethodId.from(SimpleUtils.class.getMethod("getRandomString")).toString());
        JacksonUtils.println(methodInfo);
        AnswerCheckUtils.check(methodInfo, "answers/SimpleUtils#getRandomString().json");
    }


}
