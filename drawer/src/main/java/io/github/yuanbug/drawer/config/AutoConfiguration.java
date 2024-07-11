package io.github.yuanbug.drawer.config;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.github.yuanbug.drawer.domain.ast.AstIndex;
import io.github.yuanbug.drawer.domain.ast.JavaFileAstInfo;
import io.github.yuanbug.drawer.utils.SearchUtils;
import io.github.yuanbug.drawer.utils.StopwatchTimer;
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
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        return new JavaParser(configuration);
    }

    @Bean
    @ConditionalOnMissingBean(AstIndex.class)
    public AstIndex astIndexContext(AstParsingConfig parsingConfig, JavaParser javaParser, TypeSolver javaParserTypeSolver) {
        AstIndex astIndex = new AstIndex(javaParser, javaParserTypeSolver);
        log.info("开始构建AST索引");
        StopwatchTimer timer = StopwatchTimer.start();
        parsingConfig.getModules().forEach(
                module -> SearchUtils.bfsAll(
                                module.srcMainJavaPath.toFile(),
                                file -> !file.isDirectory() && file.getName().endsWith(".java"),
                                file -> file.isDirectory()
                                        ? Optional.ofNullable(file.listFiles()).map(Arrays::asList).orElseGet(Collections::emptyList)
                                        : Collections.emptyList()
                        )
                        .forEach(javaFile -> astIndex.addFileToIndex(javaFile, module))
        );
        astIndex.seal();
        var classNameToFileInfo = astIndex.getClassNameToFileInfo();
        int moduleNum = parsingConfig.getModules().size();
        long fileNum = classNameToFileInfo.values().stream()
                .map(JavaFileAstInfo::getFile)
                .distinct()
                .count();
        int typeNum = classNameToFileInfo.keySet().size();
        log.info("AST索引构建完成，共计从{}个模块的{}个文件中扫描得到{}个类型，耗时{}ms", moduleNum, fileNum, typeNum, timer.next());
        return astIndex;
    }

}
