package br.com.fiapx.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FiapxApiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FiapxApiServiceApplication.class, args);
    }
}
