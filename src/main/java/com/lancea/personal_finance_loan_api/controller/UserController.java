package com.lancea.personal_finance_loan_api.controller;


import com.lancea.personal_finance_loan_api.dto.response.PersonalInfo;
import com.lancea.personal_finance_loan_api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "api/v1/users")
public class UserController {

    private UserService userService;

    public UserController (UserService userService){
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<PersonalInfo> me (@AuthenticationPrincipal Jwt jwt){

        return ResponseEntity.ok( userService.getPersonalInfo(jwt));
    }

}
