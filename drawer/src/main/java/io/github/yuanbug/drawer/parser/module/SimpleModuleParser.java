package io.github.yuanbug.drawer.parser.module;

import io.github.yuanbug.drawer.domain.CodeModule;
import io.github.yuanbug.drawer.utils.MiscUtils;
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
public class SimpleModuleParser implements ModuleParser {

    @Override
    public List<CodeModule> parseModules(@Nonnull File root, boolean parseDependency, @Nonnull Predicate<CodeModule> predicate) {
        if (!root.exists() || !root.isDirectory()) {
            return Collections.emptyList();
        }
        List<File> moduleDirs = SearchUtils.bfsAll(
                root,
                this::isModuleDir,
                dir -> Optional.ofNullable(dir.listFiles()).map(Arrays::asList).orElseGet(Collections::emptyList)
        );
        return moduleDirs.stream()
                .map(dir -> parseModule(dir, parseDependency))
                .filter(Objects::nonNull)
                .filter(predicate)
                .toList();
    }

    @Override
    public boolean isModuleDir(File file) {
        if (!file.isDirectory()) {
            return false;
        }
        return MiscUtils.isPresentAnd(getSrcPath(file), path -> path.toFile().exists());
    }

    @SneakyThrows
    protected CodeModule parseModule(File dir, boolean parseDependency) {
        String moduleName = dir.getName();
        Path srcPath = getSrcPath(dir);
        if (null == srcPath) {
            return null;
        }
        return CodeModule.builder()
                .name(moduleName)
                .srcMainJavaPath(srcPath)
                .compileDependencyJars(Collections.emptyList())
                .build();
    }

    protected Path getSrcPath(File dir) {
        Path path = dir.toPath().resolve(Path.of("src/main/java"));
        if (!path.toFile().exists()) {
            return null;
        }
        return path;
    }

}
