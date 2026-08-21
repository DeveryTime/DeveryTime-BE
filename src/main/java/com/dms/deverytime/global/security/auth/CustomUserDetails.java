package com.dms.deverytime.global.security.auth;

import com.dms.deverytime.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final User user;

    public Long getUserId(){
        return user.getId();
    }

    @Override
    public String getUsername(){
        return user.getId().toString();
    }

    @Override
    public String getPassword(){
        return user.getPasswordHash();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return Collections.emptyList();
    }

}
