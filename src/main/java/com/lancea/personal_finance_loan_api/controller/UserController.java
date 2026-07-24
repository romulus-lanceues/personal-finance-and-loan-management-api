package com.lancea.personal_finance_loan_api.controller;


import com.lancea.personal_finance_loan_api.dto.request.UpdateInfoRequest;
import com.lancea.personal_finance_loan_api.dto.response.PersonalInfo;
import com.lancea.personal_finance_loan_api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;


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

    @PutMapping("/me")
    public ResponseEntity<Void> updatePersonalInfo (@Valid @RequestBody UpdateInfoRequest updateInfoRequest,
                                                    @AuthenticationPrincipal Jwt jwt){

        userService.updatePersonalInfo(updateInfoRequest, jwt);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser (@AuthenticationPrincipal Jwt jwt){
        userService.deleteUser(jwt);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
