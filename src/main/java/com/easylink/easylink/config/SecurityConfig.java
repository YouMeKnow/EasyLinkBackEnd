package com.easylink.easylink.config;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration; // ← добавили
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import java.time.Duration;

import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth

                        // ======= 🔓 React frontend routes + static files =======
                        .requestMatchers(
                                "/", "/index.html", "/static/**", "/assets/**",
                                "/favicon.ico", "/email-verified",
                                "/*.png", "/**/*.png", "/*.svg", "/**/*.svg",
                                "/*.jpg", "/**/*.jpg", "/*.jpeg", "/**/*.jpeg",
                                "/*.css", "/**/*.css", "/*.js", "/**/*.js",
                                "/clearviewblue.png", "/uploads/**",
                                "/view/**", "view/**",
                                "/view-offer-form/**", "view-offer-form/**",
                                "/vibes/**", "vibes/**",
                                "/profile/**", "profile/**",
                                "/signup", "/login", "/register",
                                "/**/{path:[^\\.]*}"
                        ).permitAll()

                        // ===== SSE stream (JWT в query) =====
                        .requestMatchers("/api/notifications/stream").permitAll()

                        // ===== notifications API (JWT header) =====
                        .requestMatchers("/api/notifications/**").authenticated()

                        // ===== Public API endpoints =====
                        .requestMatchers(HttpMethod.GET,
                                "/api/v3/catalog/**",
                                "/api/v3/reviews/**",
                                "/api/v3/vibes/**",
                                "/api/v3/offers/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v3/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v3/auth/**").permitAll()

                        // ===== health / jwks / options =====
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/.well-known/jwks.json").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ===== everything else =====
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));

        return http.build();
    }

    // ======= Beans =======
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var source = new UrlBasedCorsConfigurationSource();
        var config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CorsFilter corsFilter(CorsConfigurationSource corsConfigurationSource) {
        return new CorsFilter(corsConfigurationSource);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public RSAPublicKey publicKey() throws Exception {
        var resource = new ClassPathResource("keys/public.pem");
        try (InputStream is = resource.getInputStream()) {
            String key = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(key);
            var spec = new X509EncodedKeySpec(decoded);
            var kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        }
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO)
                // , JwtValidators.createDefault()
        );

        decoder.setJwtValidator(validator);
        return decoder;
    }
}
