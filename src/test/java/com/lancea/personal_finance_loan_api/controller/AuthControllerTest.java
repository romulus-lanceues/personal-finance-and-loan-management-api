package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.config.TestSecurityConfig;
import com.lancea.personal_finance_loan_api.dto.request.AuthenticationRequest;
import com.lancea.personal_finance_loan_api.dto.response.AuthenticationResponse;
import com.lancea.personal_finance_loan_api.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    CacheManager cacheManager;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("register tests")
    class RegisterUserTests {

        @Test
        void givenAuthenticationRequest_whenRegisterUser_thenReturnAuthResponse() throws Exception {

            Jwt makeUpJwt = Jwt.withTokenValue("dummy-token")
                    .header("alg", "none")
                    .claim("userId", "5b8669be-0449-421a-9242-9899f5dc9b9f")
                    .build();

            AuthenticationRequest request = new AuthenticationRequest("John Doe",
                    "johndoe@testmail.com", "testpassword101", "testpassword101");

            AuthenticationResponse authenticationResponse = new AuthenticationResponse(UUID.fromString("85172288-7c6e-42e3-bc01-aca16e0af239"),"Success", makeUpJwt.toString());

            given(authService.registerUser(any(AuthenticationRequest.class))).willReturn(authenticationResponse);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/v1/users/me"))
                    .andExpect(jsonPath("$.id").value("85172288-7c6e-42e3-bc01-aca16e0af239"))
                    .andExpect(jsonPath("$.message").value("Success"))
                    .andExpect(jsonPath("$.token").value(makeUpJwt.toString()));
        }

        @Test
        void givenBlankAccountName_whenRegisterUser_thenThrowBadRequest() throws Exception {

            String invalidRequest = """
                {"fullName" : "", "email": "johndoe@testmail.com", "password": "testpassword101". "confirmPassword": "testpassword101"}
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

    }

    @Nested
    @DisplayName("login user tests")
    class LoginUserTest {
        @Test
        void givenAuthenticationRequest_whenLoginUser_thenReturnAuthenticationResponse() throws Exception{

            String loginAuthenticationRequest = """
                {"email" : "johndoe@testmail.com", "password" : "testpassword101"}
                """;

            Jwt makeUpJwt = Jwt.withTokenValue("dummy-token")
                    .header("alg", "none")
                    .claim("userId", "5b8669be-0449-421a-9242-9899f5dc9b9f")
                    .build();

            AuthenticationResponse authenticationResponse = new AuthenticationResponse(UUID.fromString("85172288-7c6e-42e3-bc01-aca16e0af239"),"Success", makeUpJwt.toString());

            given(authService.loginUser(any(AuthenticationRequest.class))).willReturn(authenticationResponse);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginAuthenticationRequest))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("85172288-7c6e-42e3-bc01-aca16e0af239"))
                    .andExpect(jsonPath("$.message").value("Success"))
                    .andExpect(jsonPath("$.token").value(makeUpJwt.toString()));
        }

        @Test
        void givenInvalidAuthenticationRequest_whenLoginUser_thenReturnAuthenticationResponse() throws Exception{

            String invalidLoginAuthenticationRequest = """
                {"email" : "", "password" : "testpassword101"}
                """;

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidLoginAuthenticationRequest))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }
    }



}
