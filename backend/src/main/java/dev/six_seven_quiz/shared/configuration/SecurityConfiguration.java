package dev.six_seven_quiz.shared.configuration;

import dev.six_seven_quiz.authentication.exception.UserNotAuthenticatedException;
import dev.six_seven_quiz.shared.component.Utilities;
import dev.six_seven_quiz.shared.dto.Failure;
import dev.six_seven_quiz.user.ApplicationUser;
import dev.six_seven_quiz.user.ApplicationUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Utilities utilities, CorsConfigurationSource corsConfigurationSource) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> {
                    authorize
                            // CORS preflight always permitted.
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            // API surface — explicit allow/deny based on the resource.
                            .requestMatchers("/api/authentication/**").permitAll()
                            .requestMatchers("/api/**").authenticated()
                            // Everything else (Swagger UI, OpenAPI docs, the bundled
                            // SPA's static files and the index.html fallback) is
                            // public — the static resource handler decides what
                            // actually gets returned. We don't enumerate page paths
                            // here; that's the SPA's job.
                            .anyRequest().permitAll();
                })
//                .formLogin(AbstractAuthenticationFilterConfigurer::permitAll)
                .logout((logout) -> logout.logoutUrl("/authentication/logout").logoutSuccessHandler((request, response, authentication) -> {
                    ResponseEntity<?> responseEntity = ResponseEntity.ok().build();
                    utilities.writeResponseEntityToResponse(response, responseEntity);
                }))
                .exceptionHandling(exceptions -> {
                    exceptions
                            .authenticationEntryPoint(
                                    (_, response, _) -> {
                                        utilities.writeResponseEntityToResponse(response, Failure.status(HttpStatus.UNAUTHORIZED).toResponseEntity());
                                    }
                            )
                            .accessDeniedHandler(
                                    (_, response, _) -> {
                                        utilities.writeResponseEntityToResponse(response, Failure.status(HttpStatus.FORBIDDEN).toResponseEntity());
                                    }
                            );
                });
        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @org.springframework.beans.factory.annotation.Value(
                    "${app.cors.allowed-origins:http://localhost:5173,http://192.168.*.*:5173}"
            ) List<String> allowedOrigins
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        // CORS rejects any request whose Origin doesn't appear here. In prod,
        // reverse proxies and HTTPS termination can make a same-origin browser
        // request *look* cross-origin to Spring (mismatched Host vs Origin),
        // and the only reliable fix is to list the public origin(s) explicitly.
        //
        // Configurable via APP_CORS_ALLOWED_ORIGINS (comma-separated). Defaults
        // cover the local Vite dev server and LAN access.
        configuration.setAllowedOriginPatterns(allowedOrigins);

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService(ApplicationUserRepository applicationUserRepository) {
        return username -> {
            Optional<ApplicationUser> userOptional = applicationUserRepository.findByUsername(username);
            if (userOptional.isEmpty()) throw new UserNotAuthenticatedException();
            ApplicationUser applicationUser = userOptional.get();

            return new User(
                    applicationUser.getUsername(),
                    applicationUser.getPassword(),
                    applicationUser.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName())).toList()
            );
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
