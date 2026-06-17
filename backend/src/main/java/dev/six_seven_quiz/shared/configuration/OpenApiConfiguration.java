package dev.six_seven_quiz.shared.configuration;

import dev.six_seven_quiz.shared.dto.Failure;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
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
    public OpenApiCustomizer failureResponseCustomizer() {
        return openApi -> {
            ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                    .resolveAsResolvedSchema(new AnnotatedType(Failure.class));

            // register Failure plus anything it references (e.g. ApiError)
            openApi.schema("Failure", resolvedSchema.schema);
            resolvedSchema.referencedSchemas.forEach(openApi::schema);

            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation ->
                            operation.getResponses().addApiResponse("default",
                                    new ApiResponse()
                                            .description("Error")
                                            .content(new Content().addMediaType(
                                                    "application/json",
                                                    new MediaType().schema(resolvedSchema.schema))))
                    )
            );
        };
    }

    @Bean
    public OpenApiCustomizer dropDefaultServers() {
        return openApi -> openApi.setServers(List.of());
    }
}

