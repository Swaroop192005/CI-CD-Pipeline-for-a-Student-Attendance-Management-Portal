package com.samp.attendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline web security.
 *
 * <p>At this stage (Task 3) the portal has no user-facing features yet, so only
 * the landing page, the health endpoint and static assets are opened up and
 * everything else already requires authentication. Real authentication and the
 * three roles (ADMIN, FACULTY, STUDENT) arrive with US-01 and US-02 in Task 5,
 * which replaces the placeholder form login configured here.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/actuator/health/**", "/actuator/info",
                                 "/css/**", "/js/**", "/images/**", "/h2-console/**").permitAll()
                .anyRequest().authenticated())
            // The H2 console renders in a frameset; permitted only because the
            // console itself is disabled outside the dev profile.
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .formLogin(form -> form.permitAll())
            .logout(logout -> logout.permitAll());
        return http.build();
    }
}
