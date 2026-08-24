package com.restroute.admin.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.admin.domain.AdminRole;
import com.restroute.admin.domain.AdminUserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AdminUserRepositoryTest {

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Test
    @DisplayName("관리자 아이디로 계정을 조회한다")
    void findByUsername_returnsAdminUser() {
        AdminUserEntity saved =
                adminUserRepository.save(AdminUserEntity.of("admin", "encoded-password", AdminRole.ADMIN));

        assertThat(adminUserRepository.findByUsername("admin")).contains(saved);
    }
}
