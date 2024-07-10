package io.github.yuanbug.drawer.example;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * @author yuanbug
 */
@Slf4j
//@Component
public class WebPageRunner {

    @PostConstruct
    protected void init() {
        run();
    }

    public void run() {
        new Thread(WebPageRunner::runVue).start();
    }

    @SneakyThrows
    private static void runVue() {
        log.info("正在启动前端");
        Path vueProjectPath = Path.of(System.getProperty("user.dir"), "main", "src/main/web/graph-viewer");
        Process process = new ProcessBuilder().command("cmd", "/C", "cd \"%s\" && npm install && npm run dev".formatted(vueProjectPath)).start();

        Thread standard = new Thread(() -> {
            try (Scanner scanner = new Scanner(process.getInputStream())) {
                while (scanner.hasNextLine()) {
                    System.out.printf("[🔵|nodejs] %s%n", scanner.nextLine());
                }
            }
        });
        Thread error = new Thread(() -> {
            try (Scanner scanner = new Scanner(process.getErrorStream())) {
                while (scanner.hasNextLine()) {
                    System.err.printf("[🔴|nodejs] %s%n", scanner.nextLine());
                }
            }
        });

        standard.start();
        error.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            standard.interrupt();
            error.interrupt();
            stop(process);
            System.out.println("[⚪|nodejs] exit");
        }));
    }

    @SneakyThrows
    private static void stop(Process process) {
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        Thread.sleep(1000);
    }

}
