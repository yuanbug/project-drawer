package io.github.yuanbug.drawer.config;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.github.yuanbug.drawer.domain.CodeModule;
import io.github.yuanbug.drawer.domain.DependencyJarFile;
import io.github.yuanbug.drawer.domain.ast.AstIndexContext;
import io.github.yuanbug.drawer.parser.ParserConstants;
import io.github.yuanbug.drawer.parser.maven.MavenModuleParser;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * @author yuanbug
 */
@Slf4j
public class DefaultAstParsingConfig implements AstParsingConfig {

    private static final Set<String> MODULES_TO_IGNORE = Set.of("drawer", "test-cases", "test", "main");

    protected final String workPath;
    protected final MavenModuleParser moduleParser;
    protected final List<CodeModule> codeModules;

    public DefaultAstParsingConfig(String workPath) {
        this.workPath = workPath;
        this.moduleParser = new MavenModuleParser();
        this.codeModules = moduleParser.parseModules(new File(workPath), shouldParseDependency(), module -> !MODULES_TO_IGNORE.contains(module.getName()));
    }

    @Override
    public List<CodeModule> getModules() {
        return codeModules;
    }

    @Override
    public List<Path> getJarPaths() {
        return getModules().stream()
                .map(CodeModule::getCompileDependencyJars)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(DependencyJarFile::getId))
                .values().stream()
                .filter(jars -> {
                    if (jars.size() == 1) {
                        return true;
                    }
                    log.warn("jar包存在多个版本 {}", jars.stream().map(DependencyJarFile::getJarFile).map(File::getName).toList());
                    return false;
                })
                .flatMap(Collection::stream)
                .map(DependencyJarFile::getJarFile)
                .map(File::toPath)
                .toList();
    }

    @Override
    public BiFunction<ClassOrInterfaceDeclaration, AstIndexContext, List<ClassOrInterfaceDeclaration>> getDirectlySubTypeParser() {
        return ParserConstants.SINGLE_DIRECTLY_SUB_TYPE_PARSER;
    }

}
