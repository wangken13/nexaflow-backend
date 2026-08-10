package com.vebcoding.trade.aigc;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication(scanBasePackages = "com.vebcoding.trade")
public class AigcApplication {
    private static final Logger log = LoggerFactory.getLogger(AigcApplication.class);

    public static void main(String[] args) throws UnknownHostException {
        Environment env = SpringApplication.run(AigcApplication.class, args).getEnvironment();
        String protocol = env.getProperty("server.ssl.key-store") == null ? "http" : "https";
        String port = env.getProperty("server.port");
        String appName = env.getProperty("spring.application.name");

        log.info("""

                ----------------------------------------------------------
                Application '{}' is running.
                Local:    {}://localhost:{}
                External: {}://{}:{}
                Profiles: {}
                ----------------------------------------------------------
                """,
                appName,
                protocol,
                port,
                protocol,
                InetAddress.getLocalHost().getHostAddress(),
                port,
                env.getActiveProfiles());
    }
}
