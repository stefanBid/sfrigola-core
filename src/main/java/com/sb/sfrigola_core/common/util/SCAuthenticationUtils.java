package com.sb.sfrigola_core.common.util;

import com.sb.sfrigola_core.common.constant.SCGeneralConstants;
import com.sb.sfrigola_core.domains.users.dto.SCUserInternalDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.Optional;

public class SCAuthenticationUtils {

    private SCAuthenticationUtils(){
        throw new AssertionError("Cannot instantiate SCAuthenticationUtils");
    }

    public static String getAuthUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                Objects.equals(authentication.getPrincipal(), "anonymousUser")) {
            return SCGeneralConstants.SYSTEM_USERNAME;
        }
        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof SCUserInternalDto scAuthUser) {
            username = scAuthUser.email();
        } else {
            username = principal.toString(); // fallback
        }
        return username;
    }

    public static Optional<SCUserInternalDto> getAuthUserByAuthentication(Authentication authentication)  {
        Object principal = authentication.getPrincipal();
        if (principal instanceof SCUserInternalDto scAuthUser) {
            return Optional.of(scAuthUser);
        }
        return Optional.empty();
    }

}
