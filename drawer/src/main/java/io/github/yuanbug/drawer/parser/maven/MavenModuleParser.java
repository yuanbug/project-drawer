package io.github.yuanbug.drawer.parser.maven;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.yuanbug.drawer.domain.CodeModule;
import io.github.yuanbug.drawer.utils.JacksonUtils;
import io.github.yuanbug.drawer.utils.SearchUtils;
import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author yuanbug
 */
public class MavenModuleParser {

    private final XmlMapper xmlMapper = new XmlMapper();
    private final MavenCommandInvoker invoker = new MavenCommandInvoker();

    public List<CodeModule> parseModules(@Nonnull File root) {
        return parseModules(root, false, module -> true);
    }

    public List<CodeModule> parseModules(@Nonnull File root, boolean parseDependency, @Nonnull Predicate<CodeModule> predicate) {
        if (!root.exists() || !root.isDirectory()) {
            return Collections.emptyList();
        }
        List<File> moduleDirs = SearchUtils.bfsAll(
                root,
                this::isDirWithPom,
                dir -> Optional.ofNullable(dir.listFiles()).map(Arrays::asList).orElseGet(Collections::emptyList)
        );
        return moduleDirs.stream()
                .map(dir -> parseModule(dir, parseDependency))
                .filter(Objects::nonNull)
                .filter(module -> null != module.getSrcMainJavaPath())
                .filter(predicate)
                .toList();
    }

    private boolean isDirWithPom(File file) {
        if (!file.isDirectory()) {
            return false;
        }
        return getPom(file).isPresent();
    }

    private Optional<File> getPom(File dir) {
        return Optional.ofNullable(dir.listFiles((d, name) -> "pom.xml".equals(name)))
                .filter(files -> files.length > 0)
                .map(files -> files[0]);
    }

    @SneakyThrows
    private CodeModule parseModule(File dir, boolean parseDependency) {
        File pomXmlFile = getPom(dir).orElseThrow();
        JsonNode pom = xmlMapper.readTree(pomXmlFile);
        String moduleName = JacksonUtils.getInTurns(pom, dir::getName, JsonNode::asText, "name", "artifactId");
        Path srcPath = getSrcPath(dir);
        return CodeModule.builder()
                .name(moduleName)
                .srcMainJavaPath(srcPath)
                // TODO 独立出去做异步解析
                .compileDependencyJars(parseDependency ? invoker.parseCompileDependencies(pomXmlFile) : Collections.emptyList())
                .build();
    }

    private Path getSrcPath(File dir) {
        Path path = dir.toPath().resolve(Path.of("src/main/java"));
        if (!path.toFile().exists()) {
            return null;
        }
        return path;
    }

}
