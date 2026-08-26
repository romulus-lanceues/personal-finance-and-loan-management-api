package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.dto.request.AuthenticationRequest;
import com.lancea.personal_finance_loan_api.dto.request.ValidationGroups;
import com.lancea.personal_finance_loan_api.dto.response.AccountResponse;
import com.lancea.personal_finance_loan_api.dto.response.AuthenticationResponse;
import com.lancea.personal_finance_loan_api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;


@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentications", description = "User registration and login")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Register a user",
            description = "Creates a new user, returns its location and an AuthenticationResponse"
    )

    @ApiResponses({
            @ApiResponse ( responseCode = "201", description = "Account was created",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse( responseCode = "400", description = "Request body failed validation",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))

    })

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> registerUser(
            @Parameter(description = "Contains the user information required for account creation")
            @Validated(ValidationGroups.Register.class)
            @RequestBody AuthenticationRequest authenticationRequest){

        AuthenticationResponse response = authService.registerUser(authenticationRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/users/me")
                .build()
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            summary = "Logs an existing user in",
            description = "Logs the user in and return an AuthenticationResponse "
    )

    @ApiResponses({
            @ApiResponse ( responseCode = "200", description = "Account was created",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse( responseCode = "400", description = "Request body failed validation or the authentication process failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> loginUser(
            @Parameter(description = "Contains the user login credentials")
            @Validated(ValidationGroups.Login.class)
            @RequestBody AuthenticationRequest authenticationRequest){

        return ResponseEntity.ok(authService.loginUser(authenticationRequest));

    }
}
