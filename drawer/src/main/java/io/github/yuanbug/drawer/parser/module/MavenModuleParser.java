package io.github.yuanbug.drawer.parser.module;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.yuanbug.drawer.domain.CodeModule;
import io.github.yuanbug.drawer.parser.maven.MavenCommandInvoker;
import io.github.yuanbug.drawer.utils.JacksonUtils;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

/**
 * @author yuanbug
 */
public class MavenModuleParser extends SimpleModuleParser {

    private final XmlMapper xmlMapper = new XmlMapper();
    private final MavenCommandInvoker invoker = new MavenCommandInvoker();

    @Override
    public boolean isModuleDir(File file) {
        if (!super.isModuleDir(file)) {
            return false;
        }
        return getPom(file).isPresent();
    }

    public Optional<File> getPom(File dir) {
        return Optional.ofNullable(dir.listFiles((d, name) -> "pom.xml".equals(name)))
                .filter(files -> files.length > 0)
                .map(files -> files[0]);
    }

    @SneakyThrows
    protected CodeModule parseModule(File dir, boolean parseDependency) {
        File pomXmlFile = getPom(dir).orElseThrow();
        JsonNode pom = xmlMapper.readTree(pomXmlFile);
        String moduleName = JacksonUtils.getInTurns(pom, dir::getName, JsonNode::asText, "name", "artifactId");
        Path srcPath = getSrcPath(dir);
        if (null == srcPath) {
            return null;
        }
        return CodeModule.builder()
                .name(moduleName)
                .srcMainJavaPath(srcPath)
                // TODO 独立出去做异步解析
                .compileDependencyJars(parseDependency ? invoker.parseCompileDependencies(pomXmlFile) : Collections.emptyList())
                .build();
    }

}
