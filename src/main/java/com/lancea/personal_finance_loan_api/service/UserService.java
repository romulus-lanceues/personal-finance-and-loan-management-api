package com.lancea.personal_finance_loan_api.service;


import com.lancea.personal_finance_loan_api.dto.response.PersonalInfo;
import com.lancea.personal_finance_loan_api.entity.User;
import com.lancea.personal_finance_loan_api.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;


import java.util.UUID;

@Service
public class UserService {

    private UserRepository userRepository;

    public  UserService (UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public PersonalInfo getPersonalInfo(Jwt jwt){


        UUID userId = UUID.fromString(jwt.getClaim("userId"));

        User user = userRepository.findById(userId).orElseThrow( () -> new RuntimeException("User not found"));

        return new PersonalInfo(user.getFullName(), user.getEmail(), user.getId());

    }

}
