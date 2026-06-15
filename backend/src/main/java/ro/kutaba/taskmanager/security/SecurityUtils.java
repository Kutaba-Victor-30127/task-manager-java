package ro.kutaba.taskmanager.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static String getCurrentUsername() {
        Authentication authentication = requireAuthentication();
        if (authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("No authenticated user is available");
        }
        return authentication.getName();
    }

    public static String getCurrentRole() {
        return requireAuthentication()
                .getAuthorities()
                .iterator()
                .next()
                .getAuthority();
    }

    private static Authentication requireAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) {
            throw new IllegalStateException("No authenticated user is available");
        }
        return authentication;
    }
}
