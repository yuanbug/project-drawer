package io.github.yuanbug.drawer.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.File;

/**
 * @author yuanbug
 */
@Data
@Builder
@AllArgsConstructor
public class DependencyJarFile {

    public final String groupId;

    public final String artifactId;

    public final String version;

    public final File jarFile;

    public String getId() {
        return groupId + ":" + artifactId;
    }

}
