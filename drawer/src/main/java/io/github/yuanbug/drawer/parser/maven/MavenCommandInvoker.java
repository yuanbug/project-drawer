package io.github.yuanbug.drawer.parser.maven;

import io.github.yuanbug.drawer.domain.DependencyJarFile;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author yuanbug
 */
@Slf4j
public class MavenCommandInvoker {

    private static final Pattern DEPENDENCY_JAR_PATTERN = Pattern.compile("\\[INFO]\\s+(.*?):(.*?):jar:(.*?):(?:compile|provided)");
    private static final String MVN_REPOSITORY_LOCATION_QUERY_CMD
            = "mvn help:evaluate"
            + " " + "-D" + "expression=settings.localRepository"
            + " " + "-q"
            + " " + "-D" + "forceStdout";

    private final Path repositoryPath;
    private final boolean invokable;

    public MavenCommandInvoker() {
        this.invokable = checkWhetherInvokable();
        this.repositoryPath = getRepositoryPath();
    }

    public List<DependencyJarFile> parseCompileDependencies(File pomFile) {
        if (!invokable || null == repositoryPath) {
            return Collections.emptyList();
        }
        try {
            File dir = pomFile.getParentFile();
            log.info("开始解析依赖 {}", pomFile);
            Process process = new ProcessBuilder()
                    .directory(dir)
                    .command("cmd", "/C", "mvn dependency:list").start();
            if (0 != process.waitFor()) {
                // 错误信息：getResult(process.getErrorStream());
                log.warn("解析依赖失败 {}", pomFile);
                return Collections.emptyList();
            }
            return parseCompileDependencies(getResult(process.getInputStream()));
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    private List<DependencyJarFile> parseCompileDependencies(String dependencyItemsText) {
        Matcher itemMatcher = DEPENDENCY_JAR_PATTERN.matcher(dependencyItemsText);
        List<DependencyJarFile> result = new ArrayList<>(8);
        while (itemMatcher.find()) {
            String groupId = itemMatcher.group(1);
            String artifactId = itemMatcher.group(2);
            String version = itemMatcher.group(3);
            Path jarDirPath = repositoryPath.resolve(groupId.replace(".", "/"))
                    .resolve(artifactId.replace(".", "/"))
                    .resolve(version);
            File jar = jarDirPath.resolve(artifactId + "-" + version + ".jar").toFile();
            if (!jar.exists()) {
                continue;
            }
            // javaparser只处理字节码jar，不处理源码包，所以这里不用设置sources包
            result.add(DependencyJarFile.builder()
                    .groupId(groupId)
                    .artifactId(artifactId)
                    .version(version)
                    .jarFile(jar)
                    .build());
        }
        return result;
    }

    private boolean checkWhetherInvokable() {
        try {
            return new ProcessBuilder("cmd", "/C", "mvn -v").start().waitFor() == 0;
        } catch (Exception ignored) {}
        return false;
    }

    private Path getRepositoryPath() {
        try {
            Process process = new ProcessBuilder("cmd", "/C", MVN_REPOSITORY_LOCATION_QUERY_CMD).start();
            if (0 != process.waitFor()) {
                return null;
            }
            String result = getResult(process.getInputStream());
            File repository = new File(result);
            if (repository.exists()) {
                return repository.toPath();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getResult(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
    }

}
