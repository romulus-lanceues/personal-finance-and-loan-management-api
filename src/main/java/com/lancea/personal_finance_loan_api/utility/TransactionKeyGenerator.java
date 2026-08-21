package com.lancea.personal_finance_loan_api.utility;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

@Component("customKeyGenerator")
@Slf4j
public class TransactionKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {

            Jwt jwt = (Jwt) params[2];
            UUID accountId = UserUtility.getUserId(jwt);
            int year = (int) params[0];
            int month = (int) params[1];

            return accountId + ":" + year + ":" + month ;
    }

}
