package com.restroute.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.domain.AdminActivityLogEntity;
import com.restroute.repository.AdminActivityLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AdminActivityLogServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-07T01:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private AdminActivityLogRepository adminActivityLogRepository;

    @Mock
    private Authentication authentication;

    private AdminActivityLogService adminActivityLogService;

    @BeforeEach
    void setUp() {
        adminActivityLogService = new AdminActivityLogService(adminActivityLogRepository, CLOCK);
    }

    @Test
    @DisplayName("전달받은 메시지를 로그인한 관리자 이름과 현재 시각으로 저장한다")
    void log_savesEntryWithActorMessageAndClockTime() {
        when(authentication.getName()).thenReturn("admin");

        adminActivityLogService.log(authentication, "휴게소(A00001) 이미지를 등록했습니다.");

        ArgumentCaptor<AdminActivityLogEntity> captor = ArgumentCaptor.forClass(AdminActivityLogEntity.class);
        verify(adminActivityLogRepository).save(captor.capture());
        AdminActivityLogEntity saved = captor.getValue();
        assertThat(saved.getActor()).isEqualTo("admin");
        assertThat(saved.getMessage()).isEqualTo("휴게소(A00001) 이미지를 등록했습니다.");
    }

    @Test
    @DisplayName("최근 활동 로그를 레포지토리에서 그대로 반환한다")
    void findRecent_delegatesToRepository() {
        AdminActivityLogEntity entity = AdminActivityLogEntity.of("admin", "메시지", java.time.LocalDateTime.now());
        when(adminActivityLogRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of(entity));

        List<AdminActivityLogEntity> results = adminActivityLogService.findRecent();

        assertThat(results).containsExactly(entity);
    }
}
