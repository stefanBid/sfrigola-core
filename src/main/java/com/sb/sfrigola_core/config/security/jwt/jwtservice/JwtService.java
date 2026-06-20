package com.sb.sfrigola_core.config.security.jwt.jwtservice;

import com.sb.sfrigola_core.common.constant.SCGeneralConstants;
import com.sb.sfrigola_core.common.util.SCAuthenticationUtils;
import com.sb.sfrigola_core.config.security.exception.ex.SCAuthenticatedUserNotFoundException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService implements IJWTService {

    private final Environment env;

    @Override
    public String generateJWTToken(Authentication authentication) {
        String jwtToken;
        String secret = env.getProperty(SCGeneralConstants.JWT_SECRET_KEY, SCGeneralConstants.JWT_SECRET_KEY_DEFAULT);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        var fetchedUser = SCAuthenticationUtils.getAuthUserByAuthentication(authentication).orElseThrow(
            () -> new SCAuthenticatedUserNotFoundException("Authenticated user not found")
        );
        var tokenDates = getTokenDates();

        jwtToken = Jwts.builder().issuer("Sfrigola Core").subject("JWT Token")
                .claim("username", fetchedUser.username())
                .claim("email", fetchedUser.email())
                .claim("id", fetchedUser.publicId())
                .claim("preferredLang", fetchedUser.preferredLang())
                .claim("roles", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(",")))
                .issuedAt(tokenDates.issuedAt())
                .expiration(tokenDates.expiration())
                .signWith(key)
                .compact();
        return jwtToken;
    }

    private record TokenDates(Date issuedAt, Date expiration) {}
    private TokenDates getTokenDates() {
        Date now = new Date();
        return new TokenDates(now, new Date(now.getTime() + env.getProperty(SCGeneralConstants.JWT_EXPIRATION_MS, Long.class, 3600000L))); // 1 hour expiration
    }


}
