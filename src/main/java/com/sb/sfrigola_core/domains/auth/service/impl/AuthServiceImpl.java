package com.sb.sfrigola_core.domains.auth.service.impl;

import com.sb.sfrigola_core.common.util.SCAuthenticationUtils;
import com.sb.sfrigola_core.config.security.jwt.jwtservice.JwtService;
import com.sb.sfrigola_core.domains.auth.dto.LoginResponseDto;
import com.sb.sfrigola_core.domains.auth.exception.SCAuthSecuritySystemException;
import com.sb.sfrigola_core.domains.auth.exception.SCCompromisedPasswordException;
import com.sb.sfrigola_core.domains.auth.exception.SCUserAlreadyExistsException;
import com.sb.sfrigola_core.domains.auth.service.IAuthService;
import com.sb.sfrigola_core.domains.languages.service.ILanguageService;
import com.sb.sfrigola_core.domains.users.dto.CreateSCUserRequestDto;
import com.sb.sfrigola_core.domains.users.dto.SCUserExternalDto;
import com.sb.sfrigola_core.domains.users.dto.SCUserInternalDto;
import com.sb.sfrigola_core.domains.users.service.ISCUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final ISCUserService userService;
    private final ILanguageService languageService;
    private final JwtService jwtService;

    @Qualifier("scAuthenticationManager")
    private final AuthenticationManager authenticationManager;

    @Qualifier("scPasswordEncoder")
    private final PasswordEncoder passwordEncoder;

    @Qualifier("scCompromisedPasswordChecker")
    private final CompromisedPasswordChecker compromisedPasswordChecker;

    @Override
    public LoginResponseDto login(String username, String password) {

        var authResult = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

        String token = jwtService.generateJWTToken(authResult);

        var userAuth = SCAuthenticationUtils.getAuthUserByAuthentication(authResult).orElseThrow(
                () -> new SCAuthSecuritySystemException("Authenticated user not found in security context")
        );


        return new LoginResponseDto(toMinimalDto(userAuth), userAuth.role().getAuthority(), token );
    }

    @Override
    @Transactional
    public boolean registerUser(CreateSCUserRequestDto userToCreate) {
        // Check language code is correct
        if(!languageService.existsByCode(userToCreate.preferredLang())) {
            throw new SCAuthSecuritySystemException("Language code " + userToCreate.preferredLang() + " does not exist");
        }

        if(userService.checkUserExistByEmail(userToCreate.email())){
            throw new SCUserAlreadyExistsException("User with email " + userToCreate.email() + " already exists");
        }

        CompromisedPasswordDecision decision = compromisedPasswordChecker.check(userToCreate.password());
        if(decision.isCompromised()) {
            throw new SCCompromisedPasswordException("Please chose a strong password");
        }
        String hashedPass = passwordEncoder.encode(userToCreate.password());
        return userService.createUser(userToCreate, hashedPass);

    }

    private SCUserExternalDto toMinimalDto(SCUserInternalDto internal) {
        return SCUserExternalDto.minimalInfo(
                internal.publicId(),
                internal.username(),
                internal.email(),
                internal.preferredLang(),
                internal.isActive(),
                internal.firstName(),
                internal.lastName()
        );
    }
}
