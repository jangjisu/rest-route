package com.restroute.flight.service;

import java.util.function.Supplier;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * 국가/도시/공항/항공사 참조 데이터 공통 시딩 컴포넌트. 외부 API를 직접 호출하지 않고, 미리
 * 생성해둔 SQL 파일({@code src/main/resources/data/*.sql})을 테이블이 비어있을 때만
 * 읽어서 insert한다 — 이 데이터는 자주 바뀌지 않아서 매 배포마다 라이브 API를 다시 부를
 * 필요가 없다는 판단으로, 갱신이 필요해지면 SQL 파일을 다시 생성해서 교체하는 방식을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class FlightReferenceDataSeeder {

    private final DataSource dataSource;

    /**
     * @param sqlResourcePath classpath 기준 SQL 파일 경로 (예: "data/flight-city-seed.sql")
     * @param currentCount 시딩 전 현재 행 수를 조회하는 콜백 — 0보다 크면 시딩을 건너뛴다
     * @return 시딩 후 행 수. 이미 데이터가 있어 건너뛰었으면 0.
     */
    public int seedIfEmpty(String sqlResourcePath, Supplier<Long> currentCount) {
        if (currentCount.get() > 0) {
            return 0;
        }
        new ResourceDatabasePopulator(new ClassPathResource(sqlResourcePath)).execute(dataSource);
        return currentCount.get().intValue();
    }
}
