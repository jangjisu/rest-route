package com.restroute.admin.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminUserEntityTest {

    @Test
    @DisplayName("관리자 계정은 아이디, 해시 비밀번호와 역할을 저장한다")
    void of_storesAdminCredentials() {
        AdminUserEntity entity = AdminUserEntity.of("admin", "encoded-password", AdminRole.ADMIN);

        assertThat(entity.getUsername()).isEqualTo("admin");
        assertThat(entity.getPassword()).isEqualTo("encoded-password");
        assertThat(entity.getRole()).isEqualTo(AdminRole.ADMIN);
    }
}
