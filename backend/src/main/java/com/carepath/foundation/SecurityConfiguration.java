package com.carepath.foundation;

import java.util.List;
import com.carepath.identity.*;
import com.carepath.security.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthProperties properties, AuthRateLimiter limits,
            ObjectMapper mapper, JwtDecoder decoder, SessionAuthenticationConverter converter) throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            .logout(logout -> logout.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .cors(cors -> {})
            // No cookie authenticates normal APIs. All auth POSTs enforce Origin + custom header below.
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(new AuthBoundaryFilter(properties,limits,mapper), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET,"/actuator/health","/actuator/health/**","/api/v1/system/info").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/v1/auth/register","/api/v1/auth/login","/api/v1/auth/refresh","/api/v1/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/assistant/config","/api/v1/assistant/questions","/api/v1/assistant/questions/*").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/v1/assistant/ask","/api/v1/assistant/questions").authenticated()
                .requestMatchers(HttpMethod.PUT,"/api/v1/assistant/questions/*").authenticated()
                .requestMatchers(HttpMethod.DELETE,"/api/v1/assistant/questions/*").authenticated()
                .requestMatchers(HttpMethod.GET,"/api/v1/history/events","/api/v1/history/concepts","/api/v1/history/concepts/*","/api/v1/history/observations/*/evidence","/api/v1/history/compare","/api/v1/history/changes").authenticated()
                .requestMatchers(HttpMethod.GET,"/api/v1/terminology/concepts","/api/v1/review/candidates","/api/v1/review/candidates/*","/api/v1/observations/*").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/v1/review/candidates/*/preview","/api/v1/review/candidates/*/confirm","/api/v1/review/candidates/*/correct","/api/v1/review/candidates/*/reject").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/v1/public/share/access").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/shares").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/v1/shares","/api/v1/shares/*/revoke").authenticated()
                .requestMatchers(HttpMethod.GET,"/api/v1/nearby-care/config").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/v1/nearby-care/search").authenticated()
                .requestMatchers(HttpMethod.GET,"/api/v1/activity").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/v1/search").authenticated()
                .requestMatchers(HttpMethod.GET,"/api/v1/auth/me").authenticated()
                .requestMatchers(HttpMethod.GET,"/api/v1/documents","/api/v1/documents/config","/api/v1/documents/*","/api/v1/documents/*/download","/api/v1/documents/*/preview").authenticated()
                .requestMatchers(HttpMethod.GET,"/api/v1/documents/*/processing-status","/api/v1/documents/*/extraction","/api/v1/documents/*/extraction/evidence/*").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/v1/documents/*/process","/api/v1/documents/*/retry").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/v1/documents").authenticated()
                .requestMatchers(HttpMethod.PUT,"/api/v1/documents/*").authenticated()
                .requestMatchers(HttpMethod.DELETE,"/api/v1/documents/*").authenticated()
                .requestMatchers(HttpMethod.GET,"/api/v1/care/symptoms","/api/v1/care/symptoms/*","/api/v1/care/appointments","/api/v1/care/appointments/*","/api/v1/care/follow-ups","/api/v1/care/follow-ups/*","/api/v1/care/reminders","/api/v1/care/notifications","/api/v1/care/notifications/unread").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/v1/care/symptoms","/api/v1/care/appointments","/api/v1/care/appointments/*/status","/api/v1/care/documents/*/detect-follow-ups","/api/v1/care/follow-ups/*/decision","/api/v1/care/notifications/*/read","/api/v1/care/notifications/read-all").authenticated()
                .requestMatchers(HttpMethod.PUT,"/api/v1/care/symptoms/*","/api/v1/care/appointments/*").authenticated()
                .requestMatchers(HttpMethod.DELETE,"/api/v1/care/symptoms/*","/api/v1/care/appointments/*").authenticated()
                .requestMatchers(HttpMethod.GET,"/api/v1/visit-packs","/api/v1/visit-packs/*","/api/v1/visit-packs/*/preview","/api/v1/visit-packs/*/pdf").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/v1/visit-packs","/api/v1/visit-packs/*/generate","/api/v1/visit-packs/*/revise").authenticated()
                .requestMatchers(HttpMethod.PUT,"/api/v1/visit-packs/*").authenticated()
                .requestMatchers(HttpMethod.DELETE,"/api/v1/visit-packs/*").authenticated()
                .anyRequest().denyAll())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(converter))
                .authenticationEntryPoint((req,res,e) -> ApiErrors.write(res,mapper,401,"SESSION_INVALID","Your session is no longer valid. Please sign in again.")))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((req,res,e) -> ApiErrors.write(res,mapper,401,"AUTH_REQUIRED","Please sign in to continue."))
                .accessDeniedHandler((req,res,e) -> ApiErrors.write(res,mapper,403,"ACCESS_DENIED","This action is not permitted.")))
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                .referrerPolicy(ref -> ref.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .frameOptions(frame -> frame.deny()));
        return http.build();
    }

    @Bean
    @Profile("local")
    @Order(1)
    SecurityFilterChain localDocumentation(HttpSecurity http) throws Exception {
        http.securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/**").permitAll().anyRequest().denyAll())
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'")));
        return http.build();
    }

    @Bean
    UserDetailsService noGeneratedDevelopmentUser() {
        return username -> { throw new UsernameNotFoundException("Password authentication is handled only by the auth service"); };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${carepath.frontend-origin}") String origin) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(origin));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Accept", "Content-Type", "X-Request-ID", "Authorization", "X-CarePath-Client"));
        cors.setExposedHeaders(List.of("X-Request-ID"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
