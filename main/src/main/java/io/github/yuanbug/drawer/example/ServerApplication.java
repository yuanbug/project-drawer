package io.github.yuanbug.drawer.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author yuanbug
 */
@Slf4j
@SpringBootApplication(scanBasePackages = {"io.github.yuanbug.drawer"})
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

}
