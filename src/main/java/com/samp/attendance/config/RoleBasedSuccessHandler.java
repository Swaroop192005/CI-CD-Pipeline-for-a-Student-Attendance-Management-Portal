package com.samp.attendance.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sends each role to a page it is actually allowed to open after signing in.
 *
 * <p>A single fixed success URL cannot work here, because the three roles have
 * disjoint landing pages. Sending everyone to {@code /attendance} meant a STUDENT
 * authenticated successfully and was then immediately refused with HTTP 403 by the
 * very next request — a working login that looks completely broken to the user.
 */
@Component
public class RoleBasedSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    static final String ADMIN_LANDING   = "/dashboard";
    static final String FACULTY_LANDING = "/attendance";
    static final String STUDENT_LANDING = "/my-attendance";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        setDefaultTargetUrl(landingPageFor(authentication));
        super.onAuthenticationSuccess(request, response, authentication);
    }

    static String landingPageFor(Authentication authentication) {
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (roles.contains("ROLE_ADMIN"))   return ADMIN_LANDING;
        if (roles.contains("ROLE_FACULTY")) return FACULTY_LANDING;
        if (roles.contains("ROLE_STUDENT")) return STUDENT_LANDING;
        // An authenticated user with no known role still needs somewhere to land
        // that will not 403; the home page is public.
        return "/";
    }
}
