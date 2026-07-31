package com.lancea.personal_finance_loan_api.utility;

import com.lancea.personal_finance_loan_api.exception.BadRequestException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class UserUtility {

    public static UUID getUserId(Jwt jwt){
        String userId = jwt.getClaim("userId");
        if( userId == null) throw new BadRequestException("User id is empty");

        return UUID.fromString(userId);
    }
}
