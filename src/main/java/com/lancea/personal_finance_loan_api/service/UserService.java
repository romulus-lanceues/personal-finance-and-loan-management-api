package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.entity.User;
import com.lancea.personal_finance_loan_api.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private UserRepository userRepository;

    public  UserService (UserRepository userRepository){
        this.userRepository = userRepository;
    }


}
