package io.github.yuanbug.drawer.config;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.github.yuanbug.drawer.domain.ast.AstIndexContext;
import io.github.yuanbug.drawer.utils.SearchUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

/**
 * @author yuanbug
 */
@Slf4j
@Configuration
public class AutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AstParsingConfig.class)
    public AstParsingConfig defaultProjectDrawerConfig() {
        return new DefaultAstParsingConfig(System.getProperty("user.dir"));
    }

    @Bean
    @ConditionalOnMissingBean(TypeSolver.class)
    public TypeSolver javaParserTypeSolver(AstParsingConfig parsingConfig) {
        CombinedTypeSolver typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver(true));
        parsingConfig.getModules().forEach(module -> typeSolver.add(new JavaParserTypeSolver(module.getSrcMainJavaPath())));
        parsingConfig.getJarPaths().forEach(jarPath -> {
            try {
                typeSolver.add(new JarTypeSolver(jarPath));
            } catch (Exception e) {
                log.error("加载jar包出错 {}", jarPath, e);
            }
        });
        return typeSolver;
    }

    @Bean
    @ConditionalOnMissingBean(JavaParser.class)
    public JavaParser javaParser(TypeSolver javaParserTypeSolver) {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setSymbolResolver(new JavaSymbolSolver(javaParserTypeSolver));
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        return new JavaParser(configuration);
    }

    @Bean
    @ConditionalOnMissingBean(AstIndexContext.class)
    public AstIndexContext astIndexContext(AstParsingConfig parsingConfig, JavaParser javaParser, TypeSolver javaParserTypeSolver) {
        AstIndexContext context = new AstIndexContext(javaParser, javaParserTypeSolver);
        parsingConfig.getModules().forEach(
                module -> SearchUtils.bfsAll(
                                module.srcMainJavaPath.toFile(),
                                file -> !file.isDirectory() && file.getName().endsWith(".java"),
                                file -> file.isDirectory()
                                        ? Optional.ofNullable(file.listFiles()).map(Arrays::asList).orElseGet(Collections::emptyList)
                                        : Collections.emptyList()
                        )
                        .forEach(javaFile -> context.addFileToIndex(javaFile, module))
        );
        context.seal();
        return context;
    }

}
