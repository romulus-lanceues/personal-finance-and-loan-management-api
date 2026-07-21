package com.lancea.personal_finance_loan_api.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class RsaKeyConfig {

    @Value("${app.jwt.key.private}")
    private String privateJwtKeyValue;

    @Value("${app.jwt.key.public}")
    private String publicJwtKeyValue;


    @Bean
    RSAPrivateKey rsaPrivateKey() throws Exception {
        byte[] decoded = Base64.getDecoder().decode(privateJwtKeyValue);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    @Bean
    RSAPublicKey rsaPublicKey() throws Exception {
        byte[] decoded = Base64.getDecoder().decode(publicJwtKeyValue);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    @Bean
    JwtEncoder jwtEncoder(RSAPrivateKey rsaPrivateKey, RSAPublicKey rsaPublicKey){

        RSAKey rsaKey = new RSAKey.Builder(rsaPublicKey).privateKey(rsaPrivateKey).build();
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(source);
    }

    @Bean
    JwtDecoder jwtDecoder (RSAPublicKey rsaPublicKey){
        return NimbusJwtDecoder.withPublicKey(rsaPublicKey).build();
    }

}
