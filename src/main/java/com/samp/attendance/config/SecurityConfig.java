package com.samp.attendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web security and the role rules for every URL (US-01, US-02).
 *
 * <p>Method-level security is enabled as well, because URL rules alone are not
 * enough: AC-14.1 requires an illegal action to be refused by the service layer,
 * so the service can annotate the actions only an ADMIN may perform.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final RoleBasedSuccessHandler successHandler;

    public SecurityConfig(RoleBasedSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/actuator/health/**", "/actuator/info",
                                 "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/dashboard", "/admin/**").hasRole("ADMIN")
                .requestMatchers("/my-attendance").hasRole("STUDENT")
                .requestMatchers("/attendance/**").hasAnyRole("FACULTY", "ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                // Role-based landing page. A single fixed URL cannot serve all
                // three roles: sending a STUDENT to /attendance authenticated them
                // and then immediately refused them with 403.
                .successHandler(successHandler)
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?loggedOut")
                .permitAll())
            // The H2 console renders in a frameset; it is only reachable when the
            // dev profile enables it.
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
        return http.build();
    }
}
