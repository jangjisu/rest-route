package com.restroute.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.restroute.admin.domain.AdminRole;
import com.restroute.admin.domain.AdminUserEntity;
import com.restroute.admin.repository.AdminUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class AdminUserDetailsServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AdminUserDetailsService adminUserDetailsService;

    @Test
    @DisplayName("관리자 계정을 Spring Security 사용자 정보로 변환한다")
    void loadUserByUsername_returnsAdminAuthority() {
        AdminUserEntity adminUser = AdminUserEntity.of("admin", "encoded-password", AdminRole.ADMIN);
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        var result = adminUserDetailsService.loadUserByUsername("admin");

        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("없는 관리자 계정은 UsernameNotFoundException을 발생시킨다")
    void loadUserByUsername_throwsWhenMissing() {
        when(adminUserRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserDetailsService.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
