package io.github.yuanbug.drawer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.File;
import java.nio.file.Path;

/**
 * @author yuanbug
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestConstants {

    public static final Path WORK_PATH = new File(System.getProperty("user.dir")).toPath().resolve(Path.of("../test-cases"));

    public static final Path CODE_PATH = WORK_PATH.resolve(Path.of("src/main/java"));

    public static final Path POM_PATH = WORK_PATH.resolve("pom.xml");

}
