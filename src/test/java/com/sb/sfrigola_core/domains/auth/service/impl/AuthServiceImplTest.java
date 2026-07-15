package com.sb.sfrigola_core.domains.auth.service.impl;

import com.sb.sfrigola_core.common.enums.SCUserRole;
import com.sb.sfrigola_core.common.models.context.SCAuthUser;
import com.sb.sfrigola_core.config.security.jwt.jwtservice.JwtService;
import com.sb.sfrigola_core.domains.auth.dto.ChangeEmailDto;
import com.sb.sfrigola_core.domains.auth.dto.ChangePasswordDto;
import com.sb.sfrigola_core.domains.auth.dto.RegisterUserDto;
import com.sb.sfrigola_core.domains.auth.exception.SCAuthSecuritySystemException;
import com.sb.sfrigola_core.domains.auth.exception.SCCompromisedPasswordException;
import com.sb.sfrigola_core.domains.auth.exception.SCNewPasswordSameAsOldPasswordException;
import com.sb.sfrigola_core.domains.auth.exception.SCOldPasswordNotMatchException;
import com.sb.sfrigola_core.domains.auth.exception.SCPasswordAndConfirmationPasswordDoesntMatchException;
import com.sb.sfrigola_core.domains.auth.exception.SCUserAlreadyExistsException;
import com.sb.sfrigola_core.domains.languages.exception.NoValidLangCodeToChangeException;
import com.sb.sfrigola_core.domains.languages.service.ILanguageService;
import com.sb.sfrigola_core.domains.users.service.ISCUserDomainBridgeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private ISCUserDomainBridgeService userDomainBridgeService;
    @Mock
    private ILanguageService languageService;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CompromisedPasswordChecker compromisedPasswordChecker;

    private AuthServiceImpl authService;

    private SCAuthUser authUser;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userDomainBridgeService, languageService, jwtService, authenticationManager, passwordEncoder, compromisedPasswordChecker);

        authUser = new SCAuthUser(UUID.randomUUID(), SCUserRole.ROLE_USER, "john", "john@example.com", "hashedOldPass", "en", true, "John", "Doe");
        var authentication = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== LOGIN ==========

    @Test
    void login_returnsTokenAndUserData_onSuccessfulAuthentication() {
        Authentication authResult = new UsernamePasswordAuthenticationToken(authUser, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authResult);
        when(jwtService.generateJWTToken(authResult)).thenReturn("jwt-token-123");

        var result = authService.login("john", "Password1!");

        assertThat(result.token()).isEqualTo("jwt-token-123");
        assertThat(result.role()).isEqualTo("ROLE_USER");
        assertThat(result.user().username()).isEqualTo("john");
        assertThat(result.user().email()).isEqualTo("john@example.com");
    }

    @Test
    void login_throwsSCAuthSecuritySystemException_whenPrincipalNotResolvable() {
        Authentication authResult = new UsernamePasswordAuthenticationToken("john", null, List.of());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authResult);
        when(jwtService.generateJWTToken(authResult)).thenReturn("jwt-token-123");

        assertThatThrownBy(() -> authService.login("john", "Password1!"))
                .isInstanceOf(SCAuthSecuritySystemException.class);
    }

    @Test
    void login_propagatesAuthenticationException_whenCredentialsInvalid() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login("john", "wrongPass"))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService);
    }

    // ========== REGISTER ==========

    @Test
    void registerUser_createsUserSuccessfully_whenAllValidationsPass() {
        var dto = new RegisterUserDto("john", "john@example.com", "Password1!", "Password1!", "en", "John", "Doe");
        when(languageService.existsByCodeOrThrow("en")).thenReturn(true);
        when(userDomainBridgeService.checkUserExistByEmail("john@example.com")).thenReturn(false);
        when(userDomainBridgeService.checkUserExistByUsername("john")).thenReturn(false);
        when(compromisedPasswordChecker.check("Password1!")).thenReturn(new CompromisedPasswordDecision(false));
        when(passwordEncoder.encode("Password1!")).thenReturn("hashedPass");

        var result = authService.registerUser(dto);

        assertThat(result).isTrue();
        verify(userDomainBridgeService).createUserOrThrow("john", "john@example.com", "hashedPass", "en", "John", "Doe");
    }

    @Test
    void registerUser_throwsWhenLanguageCodeInvalid() {
        var dto = new RegisterUserDto("john", "john@example.com", "Password1!", "Password1!", "xx", "John", "Doe");
        when(languageService.existsByCodeOrThrow("xx")).thenThrow(new NoValidLangCodeToChangeException("Invalid lang code"));

        assertThatThrownBy(() -> authService.registerUser(dto))
                .isInstanceOf(NoValidLangCodeToChangeException.class);

        verifyNoInteractions(userDomainBridgeService);
    }

    @Test
    void registerUser_throwsWhenEmailAlreadyExists() {
        var dto = new RegisterUserDto("john", "john@example.com", "Password1!", "Password1!", "en", "John", "Doe");
        when(languageService.existsByCodeOrThrow("en")).thenReturn(true);
        when(userDomainBridgeService.checkUserExistByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerUser(dto))
                .isInstanceOf(SCUserAlreadyExistsException.class);

        verify(userDomainBridgeService, never()).createUserOrThrow(any(), any(), any(), any(), any(), any());
    }

    @Test
    void registerUser_throwsWhenUsernameAlreadyExists() {
        var dto = new RegisterUserDto("john", "john@example.com", "Password1!", "Password1!", "en", "John", "Doe");
        when(languageService.existsByCodeOrThrow("en")).thenReturn(true);
        when(userDomainBridgeService.checkUserExistByEmail("john@example.com")).thenReturn(false);
        when(userDomainBridgeService.checkUserExistByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerUser(dto))
                .isInstanceOf(SCUserAlreadyExistsException.class);

        verify(userDomainBridgeService, never()).createUserOrThrow(any(), any(), any(), any(), any(), any());
    }

    @Test
    void registerUser_throwsWhenPasswordCompromised() {
        var dto = new RegisterUserDto("john", "john@example.com", "Password1!", "Password1!", "en", "John", "Doe");
        when(languageService.existsByCodeOrThrow("en")).thenReturn(true);
        when(userDomainBridgeService.checkUserExistByEmail("john@example.com")).thenReturn(false);
        when(userDomainBridgeService.checkUserExistByUsername("john")).thenReturn(false);
        when(compromisedPasswordChecker.check("Password1!")).thenReturn(new CompromisedPasswordDecision(true));

        assertThatThrownBy(() -> authService.registerUser(dto))
                .isInstanceOf(SCCompromisedPasswordException.class);

        verify(userDomainBridgeService, never()).createUserOrThrow(any(), any(), any(), any(), any(), any());
    }

    @Test
    void registerUser_throwsWhenPasswordConfirmationMismatch() {
        var dto = new RegisterUserDto("john", "john@example.com", "Password1!", "Different1!", "en", "John", "Doe");
        when(languageService.existsByCodeOrThrow("en")).thenReturn(true);
        when(userDomainBridgeService.checkUserExistByEmail("john@example.com")).thenReturn(false);
        when(userDomainBridgeService.checkUserExistByUsername("john")).thenReturn(false);
        when(compromisedPasswordChecker.check("Password1!")).thenReturn(new CompromisedPasswordDecision(false));

        assertThatThrownBy(() -> authService.registerUser(dto))
                .isInstanceOf(SCPasswordAndConfirmationPasswordDoesntMatchException.class);

        verify(userDomainBridgeService, never()).createUserOrThrow(any(), any(), any(), any(), any(), any());
    }

    // ========== CHANGE EMAIL ==========

    @Test
    void changeRegistrationEmail_updatesEmailSuccessfully_whenNewEmailValid() {
        var dto = new ChangeEmailDto("new@example.com");
        when(userDomainBridgeService.checkUserExistByEmail("new@example.com")).thenReturn(false);

        var result = authService.changeRegistrationEmail(dto);

        assertThat(result).isTrue();
        verify(userDomainBridgeService).updateEmailOrThrow(authUser.publicId(), "new@example.com", authUser.username());
    }

    @Test
    void changeRegistrationEmail_throwsWhenNewEmailSameAsCurrent() {
        var dto = new ChangeEmailDto(authUser.email());

        assertThatThrownBy(() -> authService.changeRegistrationEmail(dto))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userDomainBridgeService, never()).updateEmailOrThrow(any(), any(), any());
    }

    @Test
    void changeRegistrationEmail_throwsWhenNewEmailAlreadyTaken() {
        var dto = new ChangeEmailDto("taken@example.com");
        when(userDomainBridgeService.checkUserExistByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.changeRegistrationEmail(dto))
                .isInstanceOf(SCUserAlreadyExistsException.class);

        verify(userDomainBridgeService, never()).updateEmailOrThrow(any(), any(), any());
    }

    // ========== CHANGE PASSWORD ==========

    @Test
    void changeAuthPassword_updatesPasswordSuccessfully_whenAllValidationsPass() {
        var dto = new ChangePasswordDto("OldPass1!", "NewPass1!", "NewPass1!");
        when(userDomainBridgeService.findByEmailWithRole(authUser.email())).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("OldPass1!", authUser.pHash())).thenReturn(true);
        when(compromisedPasswordChecker.check("NewPass1!")).thenReturn(new CompromisedPasswordDecision(false));
        when(passwordEncoder.encode("NewPass1!")).thenReturn("hashedNewPass");

        var result = authService.changeAuthPassword(dto);

        assertThat(result).isTrue();
        verify(userDomainBridgeService).updatePasswordHashOrThrow(authUser.publicId(), "hashedNewPass", authUser.username());
    }

    @Test
    void changeAuthPassword_throwsWhenAuthUserNotFoundInDatabase() {
        var dto = new ChangePasswordDto("OldPass1!", "NewPass1!", "NewPass1!");
        when(userDomainBridgeService.findByEmailWithRole(authUser.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changeAuthPassword(dto))
                .isInstanceOf(SCAuthSecuritySystemException.class);

        verify(userDomainBridgeService, never()).updatePasswordHashOrThrow(any(), any(), any());
    }

    @Test
    void changeAuthPassword_throwsWhenOldPasswordIncorrect() {
        var dto = new ChangePasswordDto("WrongOld1!", "NewPass1!", "NewPass1!");
        when(userDomainBridgeService.findByEmailWithRole(authUser.email())).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("WrongOld1!", authUser.pHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.changeAuthPassword(dto))
                .isInstanceOf(SCOldPasswordNotMatchException.class);

        verify(userDomainBridgeService, never()).updatePasswordHashOrThrow(any(), any(), any());
    }

    @Test
    void changeAuthPassword_throwsWhenNewPasswordSameAsOld() {
        var dto = new ChangePasswordDto("SamePass1!", "SamePass1!", "SamePass1!");
        when(userDomainBridgeService.findByEmailWithRole(authUser.email())).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("SamePass1!", authUser.pHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.changeAuthPassword(dto))
                .isInstanceOf(SCNewPasswordSameAsOldPasswordException.class);

        verify(userDomainBridgeService, never()).updatePasswordHashOrThrow(any(), any(), any());
    }

    @Test
    void changeAuthPassword_throwsWhenNewPasswordCompromised() {
        var dto = new ChangePasswordDto("OldPass1!", "NewPass1!", "NewPass1!");
        when(userDomainBridgeService.findByEmailWithRole(authUser.email())).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("OldPass1!", authUser.pHash())).thenReturn(true);
        when(compromisedPasswordChecker.check("NewPass1!")).thenReturn(new CompromisedPasswordDecision(true));

        assertThatThrownBy(() -> authService.changeAuthPassword(dto))
                .isInstanceOf(SCCompromisedPasswordException.class);

        verify(userDomainBridgeService, never()).updatePasswordHashOrThrow(any(), any(), any());
    }

    @Test
    void changeAuthPassword_throwsWhenConfirmationMismatch() {
        var dto = new ChangePasswordDto("OldPass1!", "NewPass1!", "Mismatch1!");
        when(userDomainBridgeService.findByEmailWithRole(authUser.email())).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("OldPass1!", authUser.pHash())).thenReturn(true);
        when(compromisedPasswordChecker.check("NewPass1!")).thenReturn(new CompromisedPasswordDecision(false));

        assertThatThrownBy(() -> authService.changeAuthPassword(dto))
                .isInstanceOf(SCPasswordAndConfirmationPasswordDoesntMatchException.class);

        verify(userDomainBridgeService, never()).updatePasswordHashOrThrow(any(), any(), any());
    }
}
