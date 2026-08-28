package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.request.AuthenticationRequest;
import com.lancea.personal_finance_loan_api.dto.response.AuthenticationResponse;
import com.lancea.personal_finance_loan_api.entity.User;
import com.lancea.personal_finance_loan_api.enums.AuthProvider;
import com.lancea.personal_finance_loan_api.repository.UserRepository;
import com.lancea.personal_finance_loan_api.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtTokenService jwtTokenService;

    @InjectMocks
    AuthService authService;

    AuthenticationRequest request;

    @BeforeEach
    void setup(){
        request = new AuthenticationRequest("Tester John",
                "testmail@mail.com", "testpassword", "testpassword");
    }

    @Nested
    @DisplayName("register tests")
    class RegisterUser {

        @Test
        void givenAuthenticationRequest_whenRegisterUser_thenReturnAuthenticationResponse(){


            User fabricatedUser = User.builder()
                    .id(UUID.fromString("5b8669be-0449-421a-9242-9899f5dc9b9f"))
                    .fullName(request.fullName())
                    .email(request.email())
                    .provider(AuthProvider.LOCAL)
                    .build();

            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

            Authentication makeUpAuth = new UsernamePasswordAuthenticationToken(request.email(),
                    request.password(), authorities);

            Jwt makeUpJwt = Jwt.withTokenValue("dummy-token")
                    .header("alg", "none")
                    .claim("userId", "5b8669be-0449-421a-9242-9899f5dc9b9f")
                    .build();


            given(userRepository.save(any(User.class))).willReturn(fabricatedUser);

            given(authenticationManager.authenticate(any(Authentication.class))).willReturn(makeUpAuth);

            given(jwtTokenService.generateJwtToken(any(Authentication.class))).willReturn(makeUpJwt.toString());

            AuthenticationResponse result = authService.registerUser(request);


            assertAll(
                    () -> assertThat(result.id()).isEqualTo(fabricatedUser.getId()),
                    () -> assertThat(result.token()).isEqualTo(makeUpJwt.toString()),
                    () -> assertThat(result.message()).isEqualTo("Success"),
                    () -> verify(userRepository).save(any(User.class))
            );

        }

        @Test
        void givenAuthenticationRequest_whenRegisterUser_thenThrowBadCredentials(){

            given(userRepository.save(any(User.class))).willThrow(DataIntegrityViolationException.class);

            assertThatThrownBy( () -> authService.registerUser(request))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("login tests")
    class UserLogin {

        @Test
        void givenAuthenticationRequest_whenLoginUser_thenReturnAuthenticationResponse(){

            User fabricatedUser = User.builder()
                    .id(UUID.fromString("03563c61-c487-45b4-92cd-3473b274baab"))
                    .fullName(request.fullName())
                    .password(request.password())
                    .email(request.email())
                    .provider(AuthProvider.LOCAL)
                    .build();

            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

            CustomUserDetails customUserDetails = new CustomUserDetails(fabricatedUser);

            Authentication makeUpAuth = new UsernamePasswordAuthenticationToken(customUserDetails, request.password(), authorities);

            Jwt makeUpJwt = Jwt.withTokenValue("dummy-token")
                    .header("alg", "none")
                    .claim("userId", "5b8669be-0449-421a-9242-9899f5dc9b9f")
                    .build();


            given(authenticationManager.authenticate(any(Authentication.class))).willReturn(makeUpAuth);
            given(jwtTokenService.generateJwtToken(any(Authentication.class))).willReturn(makeUpJwt.toString());

            AuthenticationResponse result = authService.loginUser(request);


            assertAll(
                    () -> assertThat(result.id()).isEqualTo(fabricatedUser.getId()),
                    () ->assertThat(result.token()).isEqualTo(makeUpJwt.toString())
            );

        }

        @Test
        void givenAuthenticationRequest_whenLoginUser_thenThrowBadCredentials(){
            given(authenticationManager.authenticate(any(Authentication.class))).willThrow(BadCredentialsException.class);

            assertThatThrownBy( () -> authService.loginUser(request))
                    .isInstanceOf(BadCredentialsException.class);

        }
    }

}
