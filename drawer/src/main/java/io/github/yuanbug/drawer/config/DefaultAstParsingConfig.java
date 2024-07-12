package io.github.yuanbug.drawer.config;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.github.yuanbug.drawer.domain.CodeModule;
import io.github.yuanbug.drawer.domain.DependencyJarFile;
import io.github.yuanbug.drawer.domain.ast.AstIndex;
import io.github.yuanbug.drawer.parser.ParserConstants;
import io.github.yuanbug.drawer.parser.module.ModuleParser;
import io.github.yuanbug.drawer.parser.module.ModuleParserDelegate;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author yuanbug
 */
@Slf4j
public class DefaultAstParsingConfig implements AstParsingConfig {

    private static final Set<String> MODULES_TO_IGNORE = Set.of("drawer", "test-cases", "test", "main");

    protected final String workPath;
    protected final ModuleParser moduleParser;
    protected final List<CodeModule> codeModules;

    public DefaultAstParsingConfig(String workPath) {
        this.workPath = workPath;
        this.moduleParser = new ModuleParserDelegate();
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
                    Map<String, DependencyJarFile> jarsByVersion = distinctByVersion(jars);
                    if (jarsByVersion.size() == 1) {
                        return true;
                    }
                    DependencyJarFile first = jars.getFirst();
                    log.warn("jar包{}存在多个版本 {}", first.getGroupId() + ":" + first.getArtifactId(), jarsByVersion.keySet());
                    return false;
                })
                .flatMap(Collection::stream)
                .map(DependencyJarFile::getJarFile)
                .map(File::toPath)
                .toList();
    }

    @Override
    public BiFunction<ClassOrInterfaceDeclaration, AstIndex, List<ClassOrInterfaceDeclaration>> getDirectlySubTypeParser() {
        return ParserConstants.SINGLE_DIRECTLY_SUB_TYPE_PARSER;
    }

    private Map<String, DependencyJarFile> distinctByVersion(List<DependencyJarFile> files) {
        return files.stream().collect(Collectors.toUnmodifiableMap(
                DependencyJarFile::getVersion,
                Function.identity(),
                (one, another) -> one
        ));
    }

}
