package org.openbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;

@SpringBootApplication
public class OpenbankSecurityBackendApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(OpenbankSecurityBackendApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(OpenbankSecurityBackendApplication.class, args);
    }
}
