package com.sb.sfrigola_core.common.util;

import com.sb.sfrigola_core.domains.users.dto.SCUserInternalDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class SCAuthenticationUtils {

    private SCAuthenticationUtils(){
        throw new AssertionError("Cannot instantiate SCAuthenticationUtils");
    }

    public static SCUserInternalDto getAuthUserByContextHolder() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || Objects.equals(authentication.getPrincipal(), "anonymousUser")) {
            return SCUserInternalDto.anonymous();
        }
        Object principal = authentication.getPrincipal();

        if (principal instanceof SCUserInternalDto scAuthUser) {
            return scAuthUser;
        }
        return SCUserInternalDto.anonymous();
    }

    public static String getAuthUser() {
        return getAuthUserByContextHolder().username();
    }

    public static Optional<SCUserInternalDto> getAuthUserByAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof SCUserInternalDto scAuthUser) {
            return Optional.of(scAuthUser);
        }
        return Optional.empty();
    }

}
