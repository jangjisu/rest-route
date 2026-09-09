package com.restroute.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테스트 사이에 모든 테이블을 비운다. 컨테이너는 JVM당 하나를 재사용하므로, 앞 테스트가
 * 남긴 행이 다음 테스트의 판정을 흐리지 않게 매번 초기화가 필요하다.
 *
 * <p>엔티티 목록이 아니라 information_schema를 읽는 이유는 {@code @Table(name=...)}로 클래스명과
 * 테이블명이 갈리는 엔티티가 있어서다(예: {@code RestStopEntity} → {@code rest_stop}).
 */
@Component
public class DatabaseCleaner {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void clear() {
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        tableNames().forEach(this::truncate);
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
    }

    @SuppressWarnings("unchecked")
    private List<String> tableNames() {
        return entityManager
                .createNativeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()")
                .getResultList();
    }

    private void truncate(String tableName) {
        entityManager.createNativeQuery("TRUNCATE TABLE `" + tableName + "`").executeUpdate();
    }
}
