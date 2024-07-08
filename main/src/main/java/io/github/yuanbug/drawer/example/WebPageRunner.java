package io.github.yuanbug.drawer.example;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * @author yuanbug
 */
@Slf4j
@Component
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
        // 休眠一下，让java服务先启动，避免vite启动过快连接不到java报代理错误 TODO 改用事件监听器启动
        log.info("3秒后启动前端");
        sleep(3000);
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

    private static void stop(Process process) {
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        sleep(1000);
    }

    @SneakyThrows
    private static void sleep(long ms) {
        Thread.sleep(ms);
    }

}
