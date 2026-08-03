package com.restroute.service.admin;

import com.restroute.common.AdminUserExceptionFactory;
import com.restroute.domain.AdminUserEntity;
import com.restroute.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUserEntity adminUser = adminUserRepository
                .findByUsername(username)
                .orElseThrow(() -> AdminUserExceptionFactory.usernameNotFound(username));

        return User.withUsername(adminUser.getUsername())
                .password(adminUser.getPassword())
                .roles(adminUser.getRole().name())
                .build();
    }
}
