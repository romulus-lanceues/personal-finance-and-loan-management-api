package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.request.AuthenticationRequest;
import com.lancea.personal_finance_loan_api.dto.response.AuthenticationResponse;
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

    public AuthenticationResponse registerUser(AuthenticationRequest authenticationRequest){

        User newUser = User.builder()
                .fullName(authenticationRequest.fullName())
                .email(authenticationRequest.email())
                .password(authenticationRequest.password())
                .provider(AuthProvider.LOCAL)
                .build();

        userRepository.save(newUser);

        return new AuthenticationResponse(newUser.getId(), "Success");
    }

    public AuthenticationResponse loginUser(AuthenticationRequest authenticationRequest){

        User user = userRepository.findUserByEmail(authenticationRequest.email()).orElseThrow( () -> new RuntimeException("Email doesn't exist"));

        if(!authenticationRequest.password().equals(user.getPassword())) throw new RuntimeException("Incorrect password");

        return new AuthenticationResponse(user.getId(),"Success");

    }
}
