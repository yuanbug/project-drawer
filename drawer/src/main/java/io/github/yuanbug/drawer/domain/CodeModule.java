package io.github.yuanbug.drawer.domain;

import io.github.yuanbug.drawer.parser.maven.DependencyJarFile;
import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author yuanbug
 */
@Data
public class CodeModule {

    public final String name;

    public final Path srcMainJavaPath;

    public final List<DependencyJarFile> compileDependencyJars;

    @Builder
    public CodeModule(String name, Path srcMainJavaPath, List<DependencyJarFile> compileDependencyJars) {
        this.name = Objects.requireNonNull(name);
        this.srcMainJavaPath = Objects.requireNonNull(srcMainJavaPath);
        this.compileDependencyJars = Objects.requireNonNullElseGet(compileDependencyJars, Collections::emptyList);
    }

}
