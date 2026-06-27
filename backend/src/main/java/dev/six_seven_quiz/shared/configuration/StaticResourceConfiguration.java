package dev.six_seven_quiz.shared.configuration;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Single source of truth for serving the bundled SPA.
 * <p>
 * All requests not handled by Spring's other dispatchers (controllers, the
 * built-in {@code /v3/api-docs} / Swagger UI, etc.) fall through to here.
 * We try to resolve them against the {@code classpath:/static/} dir; if
 * nothing matches we return {@code index.html} so the Vue router can take
 * over. No path-aware controllers, no per-route permitAll lists.
 */
@Configuration
public class StaticResourceConfiguration implements WebMvcConfigurer {

    private static final ClassPathResource INDEX_HTML = new ClassPathResource("static/index.html");

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaFallbackResolver());
    }

    /**
     * Resolves any unknown path to index.html. Spring's built-in
     * PathResourceResolver already handles real assets; we only get called
     * when the lookup misses (a route like /register, /app/profile, …).
     * <p>
     * One blunt rule: if the path looks like an API call, refuse and let
     * the framework return 404. Otherwise serve the SPA shell.
     */
    private static final class SpaFallbackResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location) throws IOException {
            Resource resolved = super.getResource(resourcePath, location);
            if (resolved != null && resolved.exists() && resolved.isReadable()) {
                return resolved;
            }
            // The API has its own controllers — don't shadow them with the SPA.
            if (resourcePath.startsWith("api/") || resourcePath.startsWith("v3/") || resourcePath.startsWith("swagger-ui")) {
                return null;
            }
            // Anything else: serve the SPA shell, let the Vue router decide.
            return INDEX_HTML.exists() ? INDEX_HTML : null;
        }
    }
}
