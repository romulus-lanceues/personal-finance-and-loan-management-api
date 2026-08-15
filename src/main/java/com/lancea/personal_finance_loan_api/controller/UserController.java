package com.lancea.personal_finance_loan_api.controller;


import com.lancea.personal_finance_loan_api.dto.request.UpdateInfoRequest;
import com.lancea.personal_finance_loan_api.dto.response.PersonalInfo;
import com.lancea.personal_finance_loan_api.dto.response.UserResponse;
import com.lancea.personal_finance_loan_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(value = "api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<PersonalInfo> me (@AuthenticationPrincipal Jwt jwt){

        return ResponseEntity.ok( userService.getPersonalInfo(jwt));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updatePersonalInfo (@Valid @RequestBody UpdateInfoRequest updateInfoRequest,
                                                            @AuthenticationPrincipal Jwt jwt){

        return ResponseEntity.ok(userService.updatePersonalInfo(updateInfoRequest, jwt));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser (@AuthenticationPrincipal Jwt jwt){
        userService.deleteUser(jwt);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
