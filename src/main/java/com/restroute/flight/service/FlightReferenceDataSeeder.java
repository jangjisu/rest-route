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
 * 지우고 다시 읽어서 insert한다 — 로컬 파일 읽기라 비용이 작아서 "비어있을 때만" 조건을
 * 둘 이유가 없고, 오히려 그 조건 때문에 스키마 마이그레이션 중 기존 행이 남아있으면
 * 재시딩이 통째로 스킵되어 방치된 값이 남는 사고가 실제로 있었다(2026-08-13, flight_city
 * 컬럼 분리 배포 때 MySQL이 기존 행의 새 컬럼을 NULL/빈 문자열로 채웠는데 재시딩이
 * 스킵돼서 프로덕션에 그대로 노출됨). 매번 지우고 다시 넣으면 배포된 SQL 파일 내용과
 * DB 상태가 항상 일치한다.
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
