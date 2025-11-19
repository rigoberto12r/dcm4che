package org.dcm4che.ris.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Spring Boot application for RIS REST API.
 * This can be used to run the REST API as a standalone service.
 *
 * @author dcm4che-ris
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "org.dcm4che.ris.rest",
        "org.dcm4che.ris.core",
        "org.dcm4che.ris.persistence"
})
@EnableJpaRepositories(basePackages = "org.dcm4che.ris.persistence.repository")
@EntityScan(basePackages = "org.dcm4che.ris.api.entity")
public class RisRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(RisRestApplication.class, args);
    }
}
