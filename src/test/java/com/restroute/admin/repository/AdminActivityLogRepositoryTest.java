package com.restroute.admin.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.admin.domain.AdminActivityLogEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AdminActivityLogRepositoryTest {

    @Autowired
    private AdminActivityLogRepository adminActivityLogRepository;

    @Test
    @DisplayName("최근 활동 로그를 시각 내림차순으로 최대 50건 조회한다")
    void findTop50ByOrderByCreatedAtDesc_returnsMostRecentFirst() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 21, 10, 0);
        adminActivityLogRepository.save(AdminActivityLogEntity.of("admin", "첫 번째 작업", now.minusMinutes(10)));
        adminActivityLogRepository.save(AdminActivityLogEntity.of("admin", "두 번째 작업", now));

        List<AdminActivityLogEntity> results = adminActivityLogRepository.findTop50ByOrderByCreatedAtDesc();

        assertThat(results)
                .hasSize(2)
                .extracting(AdminActivityLogEntity::getMessage)
                .containsExactly("두 번째 작업", "첫 번째 작업");
    }
}
