package com.lancea.personal_finance_loan_api.security;

import com.lancea.personal_finance_loan_api.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;


public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user){
        this.user = user;
    }

    public UUID getUserId(){
        return user.getId();
    }

    @Override
    public String getUsername(){
        return user.getEmail();
    }

    @Override
    public String getPassword(){
        return user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return AuthorityUtils.createAuthorityList("USER");
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean
    isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
