package com.restroute.service.admin;

import com.restroute.domain.AdminActivityLogEntity;
import com.restroute.repository.AdminActivityLogRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 활동 로그 저장/조회만 담당한다. 각 도메인의 로그 메시지 문구 조립은 그 도메인을 다루는
 * 컨트롤러(또는 서비스)가 책임지고, 이 서비스는 완성된 메시지를 받아 저장하기만 한다.
 */
@Service
@RequiredArgsConstructor
public class AdminActivityLogService {

    private final AdminActivityLogRepository adminActivityLogRepository;
    private final Clock clock;

    @Transactional
    public void log(Authentication authentication, String message) {
        adminActivityLogRepository.save(
                AdminActivityLogEntity.of(authentication.getName(), message, LocalDateTime.now(clock)));
    }

    @Transactional(readOnly = true)
    public List<AdminActivityLogEntity> findRecent() {
        return adminActivityLogRepository.findTop50ByOrderByCreatedAtDesc();
    }
}
