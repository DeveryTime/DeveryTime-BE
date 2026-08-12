package com.dms.devrytime.global.security.auth;

import com.dms.devrytime.domain.user.domain.User;
import com.dms.devrytime.domain.user.domain.UserRepository;
import com.dms.devrytime.global.exception.DevryTimeException;
import com.dms.devrytime.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userId){
        Long id = Long.valueOf(userId);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new DevryTimeException(ErrorCode.USER_NOT_FOUND));

        return new CustomUserDetails(user);
    }
}
