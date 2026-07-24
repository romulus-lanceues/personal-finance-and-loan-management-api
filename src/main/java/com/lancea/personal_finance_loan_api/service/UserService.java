package com.lancea.personal_finance_loan_api.service;


import com.lancea.personal_finance_loan_api.dto.request.UpdateInfoRequest;
import com.lancea.personal_finance_loan_api.dto.response.PersonalInfo;
import com.lancea.personal_finance_loan_api.entity.User;
import com.lancea.personal_finance_loan_api.exception.UserNotFoundException;
import com.lancea.personal_finance_loan_api.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;


import java.time.Instant;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public  UserService (UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public PersonalInfo getPersonalInfo(Jwt jwt){

        User user = getUserOrThrow(jwt);
        return new PersonalInfo(user.getFullName(), user.getEmail(), user.getId());

    }

    public void updatePersonalInfo(UpdateInfoRequest updateInfoRequest, Jwt jwt){

        User user = getUserOrThrow(jwt);

        if(updateInfoRequest.fullName() == null){
            user.setEmail(updateInfoRequest.email());
            userRepository.save(user);
            return;
        }

        else if(updateInfoRequest.email() == null) {
            user.setFullName(updateInfoRequest.fullName());
            userRepository.save(user);
            return;
        }

        user.setEmail(updateInfoRequest.email());
        user.setFullName(updateInfoRequest.fullName());


        userRepository.save(user);
    }

    public void deleteUser (Jwt jwt){

        User user =getUserOrThrow(jwt);

        user.setIsDeleted(true);
        user.setDeletedAt(Instant.now());

        userRepository.save(user);
    }

    private User getUserOrThrow(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaim("userId"));
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

}
