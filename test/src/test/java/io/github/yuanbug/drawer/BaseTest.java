package io.github.yuanbug.drawer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.TypeSolver;
import io.github.yuanbug.drawer.config.AstParsingConfig;
import io.github.yuanbug.drawer.config.AutoConfiguration;
import io.github.yuanbug.drawer.config.DefaultAstParsingConfig;
import io.github.yuanbug.drawer.domain.CodeModule;
import io.github.yuanbug.drawer.domain.ast.AstIndexContext;
import io.github.yuanbug.drawer.parser.MethodParser;
import io.github.yuanbug.drawer.parser.ParserConstants;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static io.github.yuanbug.drawer.TestConstants.CODE_PATH;

/**
 * @author yuanbug
 */
public abstract class BaseTest extends AutoConfiguration {

    protected final AstParsingConfig config;

    protected final JavaParser javaParser;

    protected final TypeSolver typeSolver;

    protected final AstIndexContext context;

    protected final MethodParser methodParser;

    protected BaseTest() {
        this.config = defaultProjectDrawerConfig();
        this.typeSolver = javaParserTypeSolver(this.config);
        this.javaParser = javaParser(typeSolver);
        this.context = astIndexContext(this.config, this.javaParser, this.typeSolver);
        this.methodParser = new MethodParser(this.context, this.config);
    }

    @Override
    public AstParsingConfig defaultProjectDrawerConfig() {
        return new TestAstParsingConfig();
    }

    protected static class TestAstParsingConfig extends DefaultAstParsingConfig {

        protected final CodeModule testCaseModule = buildTestCaseModule();

        public TestAstParsingConfig() {
            super(System.getProperty("user.dir"));
        }

        @Override
        public List<CodeModule> getModules() {
            return List.of(testCaseModule);
        }

        @Override
        public BiFunction<ClassOrInterfaceDeclaration, AstIndexContext, List<ClassOrInterfaceDeclaration>> getDirectlySubTypeParser() {
            return ParserConstants.ALL_DIRECTLY_SUB_TYPE_PARSER;
        }

        protected CodeModule buildTestCaseModule() {
            return CodeModule.builder()
                    .name("test-cases")
                    .srcMainJavaPath(CODE_PATH)
                    .compileDependencyJars(Collections.emptyList())
                    .build();
        }

    }

}
