package dev.six_seven_quiz.shared.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfiguration {
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("67-quiz API").version("1.0"))
                .components(new Components().addSecuritySchemes("session",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("SESSION")
                ))
                .addSecurityItem(new SecurityRequirement().addList("session"));
    }

    @Bean
    public OpenApiCustomizer dropDefaultServers() {
        return openApi -> openApi.setServers(List.of());
    }
}

