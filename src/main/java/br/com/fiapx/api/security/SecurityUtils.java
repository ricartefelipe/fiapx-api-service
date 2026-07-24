package br.com.fiapx.api.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof FiapxUserDetails details)) {
            throw new IllegalStateException("Usuário não autenticado");
        }
        return details.getUserId();
    }
}
