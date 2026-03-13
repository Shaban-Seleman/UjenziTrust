package com.uzenjitrust.common.config;

import com.uzenjitrust.common.security.JsonAccessDeniedHandler;
import com.uzenjitrust.common.security.JsonAuthenticationEntryPoint;
import com.uzenjitrust.common.security.JwtAuthenticationFilter;
import com.uzenjitrust.common.security.JwtProperties;
import com.uzenjitrust.common.security.SecurityProperties;
import com.uzenjitrust.common.web.CorrelationIdFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, SecurityProperties.class})
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   DocsProperties docsProperties,
                                                   CorrelationIdFilter correlationIdFilter,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   JsonAuthenticationEntryPoint authenticationEntryPoint,
                                                   JsonAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicMatchers(docsProperties))
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, CorrelationIdFilter.class);

        return http.build();
    }

    private String[] publicMatchers(DocsProperties docsProperties) {
        if (docsProperties.isEnabled()) {
            return new String[]{
                    "/auth/login",
                    "/actuator/health",
                    "/ops/webhooks/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
            };
        }
        return new String[]{
                "/auth/login",
                "/actuator/health",
                "/ops/webhooks/**"
        };
    }
}
