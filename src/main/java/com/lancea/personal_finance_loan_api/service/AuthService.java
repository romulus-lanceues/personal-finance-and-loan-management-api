package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.dto.request.RegisterRequest;
import com.lancea.personal_finance_loan_api.dto.response.RegisterResponse;
import com.lancea.personal_finance_loan_api.entity.User;
import com.lancea.personal_finance_loan_api.enums.AuthProvider;
import com.lancea.personal_finance_loan_api.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private UserRepository userRepository;

    public AuthService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public RegisterResponse registerUser(RegisterRequest registerRequest){

        User newUser = User.builder()
                .fullName(registerRequest.fullName())
                .email(registerRequest.email())
                .password(registerRequest.password())
                .provide(AuthProvider.LOCAL)
                .build();

        userRepository.save(newUser);

        return new RegisterResponse(newUser.getId(), "Success");
    }
}
