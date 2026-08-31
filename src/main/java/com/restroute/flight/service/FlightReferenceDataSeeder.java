package com.restroute.flight.service;

import java.util.function.Supplier;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * 국가/도시/공항/항공사 참조 데이터 공통 시딩 컴포넌트. 외부 API를 직접 호출하지 않고, 미리
 * 생성해둔 SQL 파일({@code src/main/resources/data/*.sql})을 매 시작마다 기존 데이터를
 * 지우고 다시 읽어서 insert한다 — 매번 지우고 다시 넣어야 배포된 SQL 파일 내용과 DB 상태가
 * 항상 일치한다. "비어있을 때만" 같은 조건을 두면 스키마 마이그레이션으로 남은 기존 행 때문에
 * 재시딩이 통째로 스킵되고, 방치된 컬럼 값이 그대로 노출된다. 로컬 파일 읽기라 매 시작마다
 * 전량 재삽입해도 비용이 작다.
 */
@Component
@RequiredArgsConstructor
public class FlightReferenceDataSeeder {

    private final DataSource dataSource;

    /**
     * @param sqlResourcePath classpath 기준 SQL 파일 경로 (예: "data/flight-city-seed.sql")
     * @param clearExisting 재시딩 전 기존 데이터를 지우는 콜백 (예: repository::deleteAllInBatch)
     * @param currentCount 시딩 후 행 수를 조회하는 콜백 (로그용)
     * @return 시딩 후 행 수
     */
    public int reseed(String sqlResourcePath, Runnable clearExisting, Supplier<Long> currentCount) {
        clearExisting.run();
        new ResourceDatabasePopulator(new ClassPathResource(sqlResourcePath)).execute(dataSource);
        return currentCount.get().intValue();
    }
}
