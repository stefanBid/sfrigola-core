package com.sb.sfrigola_core.common.util;

import com.sb.sfrigola_core.common.models.context.SCAuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.Optional;

public class SCAuthenticationUtils {

    private SCAuthenticationUtils(){
        throw new AssertionError("Cannot instantiate SCAuthenticationUtils");
    }

    public static SCAuthUser getAuthUserByContextHolder() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || Objects.equals(authentication.getPrincipal(), "anonymousUser")) {
            return SCAuthUser.anonymous();
        }
        Object principal = authentication.getPrincipal();

        if (principal instanceof SCAuthUser scAuthUser) {
            return scAuthUser;
        }
        return SCAuthUser.anonymous();
    }

    public static String getAuthUser() {
        return getAuthUserByContextHolder().username();
    }

    public static Optional<SCAuthUser> getAuthUserByAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof SCAuthUser scAuthUser) {
            return Optional.of(scAuthUser);
        }
        return Optional.empty();
    }

}
