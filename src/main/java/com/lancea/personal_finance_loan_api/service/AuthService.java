package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.aspect.Auditable;
import com.lancea.personal_finance_loan_api.dto.request.AuthenticationRequest;
import com.lancea.personal_finance_loan_api.dto.response.AuthenticationResponse;
import com.lancea.personal_finance_loan_api.entity.User;
import com.lancea.personal_finance_loan_api.enums.AuthProvider;
import com.lancea.personal_finance_loan_api.repository.UserRepository;
import com.lancea.personal_finance_loan_api.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;


    public AuthenticationResponse registerUser(AuthenticationRequest authenticationRequest){

        User newUser = User.builder()
                .fullName(authenticationRequest.fullName())
                .email(authenticationRequest.email())
                .password(passwordEncoder.encode(authenticationRequest.password()))
                .provider(AuthProvider.LOCAL)
                .build();

        userRepository.save(newUser);

        Authentication authRequest = UsernamePasswordAuthenticationToken
                .unauthenticated(authenticationRequest.email(), authenticationRequest.password());

        Authentication authenticatedRequest = authenticationManager.authenticate(authRequest);

        String jwtToken = jwtTokenService.generateJwtToken(authenticatedRequest);

        return new AuthenticationResponse(newUser.getId(), "Success", jwtToken);
    }

    public AuthenticationResponse loginUser(AuthenticationRequest authenticationRequest){

        Authentication authRequest = UsernamePasswordAuthenticationToken
                .unauthenticated(authenticationRequest.email(), authenticationRequest.password());

        Authentication authenticatedRequest = authenticationManager.authenticate(authRequest);

        UUID userId = ((CustomUserDetails) authenticatedRequest.getPrincipal()).getUserId();

        String jwtToken = jwtTokenService.generateJwtToken(authenticatedRequest);

        return new AuthenticationResponse(userId,"Success", jwtToken);

    }
}
