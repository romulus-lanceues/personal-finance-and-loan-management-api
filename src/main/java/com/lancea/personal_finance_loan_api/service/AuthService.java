package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.request.AuthenticationRequest;
import com.lancea.personal_finance_loan_api.dto.request.RegisterRequest;
import com.lancea.personal_finance_loan_api.dto.response.AuthenticationResponse;
import com.lancea.personal_finance_loan_api.dto.response.RegisterResponse;
import com.lancea.personal_finance_loan_api.entity.User;
import com.lancea.personal_finance_loan_api.enums.AuthProvider;
import com.lancea.personal_finance_loan_api.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService {

    private UserRepository userRepository;

    public AuthService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public RegisterResponse registerUser(AuthenticationRequest authenticationRequest){

        User newUser = User.builder()
                .fullName(authenticationRequest.fullName())
                .email(authenticationRequest.email())
                .password(authenticationRequest.password())
                .provide(AuthProvider.LOCAL)
                .build();

        userRepository.save(newUser);

        return new RegisterResponse(newUser.getId(), "Success");
    }

    public void loginUser(AuthenticationRequest authenticationRequest){

        log.info("Authentication name: {}, Email: {}, Password: {}, Confirm Password: {}", authenticationRequest.fullName(),
                authenticationRequest.email(), authenticationRequest.password(), authenticationRequest.confirmPassword());
    }
}
