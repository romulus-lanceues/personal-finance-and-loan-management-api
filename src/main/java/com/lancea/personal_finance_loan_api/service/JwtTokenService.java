package com.lancea.personal_finance_loan_api.service;

import com.lancea.personal_finance_loan_api.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;

    public String generateJwtToken(Authentication authentication){

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Instant issuedTime = Instant.now();

        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(issuedTime)
                .expiresAt(issuedTime.plus(1, ChronoUnit.HOURS))
                .subject(authentication.getName())
                .claim("roles", roles)
                .claim("userId", userDetails.getUserId() )
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

    }
}
