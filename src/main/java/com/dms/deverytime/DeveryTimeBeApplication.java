package com.dms.deverytime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
@ConfigurationPropertiesScan
public class DeveryTimeBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeveryTimeBeApplication.class, args);
    }

}
