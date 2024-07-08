package io.github.yuanbug.drawer.domain;

import io.github.yuanbug.drawer.parser.maven.DependencyJarFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.List;

/**
 * @author yuanbug
 */
@Data
@Builder
@AllArgsConstructor
public class CodeModule {

    public final String name;

    public final Path srcMainJavaPath;

    public final List<DependencyJarFile> compileDependencyJars;

}
