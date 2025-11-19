package org.dcm4che.ris.rest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * REST API configuration.
 * Configures CORS, pagination, and OpenAPI documentation.
 *
 * @author dcm4che-ris
 */
@Configuration
@ConfigurationProperties(prefix = "ris.rest")
@Getter
@Setter
public class RestConfiguration {

    private CorsConfig cors = new CorsConfig();
    private PaginationConfig pagination = new PaginationConfig();
    private SecurityConfig security = new SecurityConfig();
    private String version = "v1";

    @Getter
    @Setter
    public static class CorsConfig {
        private boolean enabled = true;
        private String allowedOrigins = "*";
        private String allowedMethods = "GET,POST,PUT,DELETE,PATCH,OPTIONS";
        private String allowedHeaders = "*";
        private long maxAge = 3600;
    }

    @Getter
    @Setter
    public static class PaginationConfig {
        private int defaultPageSize = 20;
        private int maxPageSize = 100;
    }

    @Getter
    @Setter
    public static class SecurityConfig {
        private boolean enabled = false;
        private boolean basicAuthEnabled = true;
        private boolean jwtEnabled = false;
    }

    /**
     * Configure CORS.
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                if (cors.isEnabled()) {
                    registry.addMapping("/**")
                            .allowedOrigins(cors.getAllowedOrigins().split(","))
                            .allowedMethods(cors.getAllowedMethods().split(","))
                            .allowedHeaders(cors.getAllowedHeaders().split(","))
                            .maxAge(cors.getMaxAge());
                }
            }
        };
    }

    /**
     * Configure OpenAPI documentation.
     */
    @Bean
    public OpenAPI risOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("dcm4che RIS REST API")
                        .description("Radiology Information System REST API")
                        .version(version)
                        .license(new License()
                                .name("MPL 1.1 / GPL 2.0 / LGPL 2.1")
                                .url("https://github.com/dcm4che/dcm4che")));
    }
}
