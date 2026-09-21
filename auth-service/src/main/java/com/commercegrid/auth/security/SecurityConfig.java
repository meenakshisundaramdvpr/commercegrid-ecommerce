package com.commercegrid.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${spring.security.user.name}")
    private String username;

    @Value("${spring.security.user.password}")
    private String password;

    @Value("${spring.security.user.roles}")
    private String role;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {

        UserDetails user = User.builder()
                .username(username)
                .password(passwordEncoder().encode(password))
                .roles(role)
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    /**
     * Handles requests that fail authentication/authorization -- returns a clean
     * JSON 401 matching our standard ErrorResponse shape, instead of Spring's
     * default HTML login-page redirect or bare 403 page.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());

            String body = String.format(
                    "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication is required to access this resource\"}",
                    LocalDateTime.now()
            );

            response.getWriter().write(body);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                . cors(cors -> {})
                .csrf(csrf -> csrf.disable())

                // Point 7: no HTTP sessions -- every request must carry its own proof of identity
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Point 3: login and creation are explicitly public entry points --
                        // called out individually rather than a blanket wildcard
                        .requestMatchers(
                                "/api/v1/auth/admin/login",
                                "/api/v1/auth/admin"
                        ).permitAll()
                        // Point 4: everything else under this path -- including status
                        // management -- now genuinely requires authentication
                        .requestMatchers("/api/v1/auth/admin/**").authenticated()
                        .anyRequest().authenticated()
                )

                // Point 8: unauthorized requests get a clean JSON response, not a redirect
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                )

                // Point 5: disable default form-login and HTTP Basic -- neither belongs
                // in a stateless JSON API; JWT will be the only authentication mechanism
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // Point 10 -- now fulfilled: JwtAuthFilter runs before Spring's default
                // auth filter, reads the Authorization header, validates the token via
                // JwtUtil, and populates the SecurityContext so .authenticated() routes
                // can recognize a legitimate bearer token
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}