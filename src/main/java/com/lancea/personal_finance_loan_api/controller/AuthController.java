package com.lancea.personal_finance_loan_api.controller;

import com.lancea.personal_finance_loan_api.dto.request.AuthenticationRequest;
import com.lancea.personal_finance_loan_api.dto.request.RegisterRequest;
import com.lancea.personal_finance_loan_api.dto.request.ValidationGroups;
import com.lancea.personal_finance_loan_api.dto.response.AuthenticationResponse;
import com.lancea.personal_finance_loan_api.dto.response.RegisterResponse;
import com.lancea.personal_finance_loan_api.service.AuthService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @GetMapping("/test")
    public String sample(){
        return "Hello from my new project!!";
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(@Validated(ValidationGroups.Register.class)
                                                             @RequestBody AuthenticationRequest authenticationRequest){

        return ResponseEntity.ok(authService.registerUser(authenticationRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Validated(ValidationGroups.Login.class)
                                                                @RequestBody AuthenticationRequest authenticationRequest){

        authService.loginUser(authenticationRequest);

        return ResponseEntity.ok("Success");

    }
}
