package com.sb.sfrigola_core.config.security.authprovider;

import com.sb.sfrigola_core.config.security.exception.ex.SCBadCredentialException;
import com.sb.sfrigola_core.config.security.exception.ex.SCUserInactiveException;
import com.sb.sfrigola_core.domains.users.service.ISCUserDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component("scUsernamePwAuthenticationProvider")
@RequiredArgsConstructor
public class UsernamePwAuthenticationProvider implements AuthenticationProvider {

    @Qualifier("scPasswordEncoder")
    private final PasswordEncoder passwordEncoder;

    private final ISCUserDomainBridgeService userDomainBridgeService;

    @Override
    public @Nullable Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // STEP 1: Extract credential from authenticate request
        String username = authentication.getName();
        String pwd = Objects.requireNonNull(authentication.getCredentials()).toString();

        // STEP 2: Load UserDetails from DB (or any other source) using the username (email FILED)
        var user = userDomainBridgeService.findByEmailWithRole(username).orElseThrow(
                () -> new SCBadCredentialException("Invalid credentials")
        );

        // STEP 3: Check if user is active
        if(!user.isActive()) {
            throw new SCUserInactiveException("User is inactive");
        }

        // STEP 4: Extract User role and convert to Spring Security GrantedAuthority
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(user.role().getAuthority()));

        // STEP 5: Verify password using personal Password encoder Bean
        if (passwordEncoder.matches(pwd, user.pHash())) {
            return new UsernamePasswordAuthenticationToken(user, null, authorities);
        } else {
            throw new SCBadCredentialException("Invalid credentials");
        }



    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
