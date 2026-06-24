package com.sb.sfrigola_core.domains.auth.service.impl;

import com.sb.sfrigola_core.common.util.SCAuthenticationUtils;
import com.sb.sfrigola_core.config.security.jwt.jwtservice.JwtService;
import com.sb.sfrigola_core.domains.auth.dto.ChangeEmailDto;
import com.sb.sfrigola_core.domains.auth.dto.ChangePasswordDto;
import com.sb.sfrigola_core.domains.auth.dto.LoginResponseDto;
import com.sb.sfrigola_core.domains.auth.dto.RegisterUserDto;
import com.sb.sfrigola_core.domains.auth.exception.*;
import com.sb.sfrigola_core.domains.auth.service.IAuthService;
import com.sb.sfrigola_core.domains.languages.service.ILanguageService;
import com.sb.sfrigola_core.domains.users.dto.SCUserDto;
import com.sb.sfrigola_core.common.models.context.SCAuthUser;
import com.sb.sfrigola_core.domains.users.service.ISCUserDomainBridgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AuthServiceImpl implements IAuthService {

    private final ISCUserDomainBridgeService userDomainBridgeService;
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
    public boolean registerUser(RegisterUserDto registerUserDto) {
        // Check language code is correct
        languageService.existsByCodeOrThrow(registerUserDto.preferredLang());

        // Check if user already exists by email
        if(userDomainBridgeService.checkUserExistByEmail(registerUserDto.email())){
            throw new SCUserAlreadyExistsException("User with email " + registerUserDto.email() + " already exists");
        }

        // Check if the user already exist by username
        if(userDomainBridgeService.checkUserExistByUsername(registerUserDto.username())){
            throw new SCUserAlreadyExistsException("User with username " + registerUserDto.username() + " already exists");
        }

        // Check if the password is compromised
        CompromisedPasswordDecision decision = compromisedPasswordChecker.check(registerUserDto.password());
        if(decision.isCompromised()) {
            throw new SCCompromisedPasswordException("Please chose a strong password");
        }

        // Check if password and confirmPassword are the same
        if(!registerUserDto.password().equals(registerUserDto.confirmPassword())) {
            throw new SCPasswordAndConfirmationPasswordDoesntMatchException("Password and confirmation password doesn't match");
        }

        String hashedPass = passwordEncoder.encode(registerUserDto.password());
         userDomainBridgeService.createUserOrThrow(
                registerUserDto.username(),
                registerUserDto.email(),
                hashedPass,
                registerUserDto.preferredLang(),
                registerUserDto.firstName(),
                registerUserDto.lastName()
        );
        return true;
    }

    @Override
    @Transactional
    public boolean changeRegistrationEmail(ChangeEmailDto changeEmailDto) {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();
        var newEmail = changeEmailDto.newEmail();

        // Check1: Email is the same as current for the user
        if(newEmail.equals(authUser.email())){
            throw new IllegalArgumentException("New email cannot be the same as current email");
        }

        // Check2: new email is already taken by another user
        if(userDomainBridgeService.checkUserExistByEmail(newEmail)){
            throw new SCUserAlreadyExistsException("User with email " + newEmail + " already exists");
        }

        userDomainBridgeService.updateEmailOrThrow(authUser.publicId(), newEmail, authUser.username());
        return true;
    }

    @Override
    @Transactional
    public boolean changeAuthPassword(ChangePasswordDto changePasswordDto) {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();

        var authUserWithPass = userDomainBridgeService.findByEmailWithRole(authUser.email())
                .orElseThrow(() -> new SCAuthSecuritySystemException("Authenticated user not found in database"));

        // Check 1: Old Password is the same as in DB
        if(!passwordEncoder.matches(changePasswordDto.oldPassword(), authUserWithPass.pHash())){
            throw new SCOldPasswordNotMatchException("Old password is incorrect");
        }

        // Check 2: New Password is the same as Old Password
        if(changePasswordDto.oldPassword().equals(changePasswordDto.newPassword())){
            throw new SCNewPasswordSameAsOldPasswordException("New password cannot be the same as old password");
        }

        // Check 3: New Password is compromised
        CompromisedPasswordDecision decision = compromisedPasswordChecker.check(changePasswordDto.newPassword());
        if(decision.isCompromised()) {
            throw new SCCompromisedPasswordException("Please chose a strong password");
        }

        // Check 4: New Password and Confirm New Password doesn't match
        if(!changePasswordDto.newPassword().equals(changePasswordDto.confirmNewPassword())){
            throw new SCPasswordAndConfirmationPasswordDoesntMatchException("New password and confirmation password doesn't match");
        }

        userDomainBridgeService.updatePasswordHashOrThrow(authUser.publicId(), passwordEncoder.encode(changePasswordDto.newPassword()), authUser.username());

        return true;
    }

    private SCUserDto toMinimalDto(SCAuthUser internal) {
        return SCUserDto.minimalInfo(
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
