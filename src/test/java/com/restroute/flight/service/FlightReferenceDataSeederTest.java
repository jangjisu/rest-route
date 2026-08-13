package com.restroute.flight.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class FlightReferenceDataSeederTest {

    private DriverManagerDataSource dataSource;
    private FlightReferenceDataSeeder flightReferenceDataSeeder;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:seeder-test-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setDriverClassName("org.h2.Driver");
        flightReferenceDataSeeder = new FlightReferenceDataSeeder(dataSource);
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE sample (code VARCHAR(10))");
        }
    }

    @Test
    @DisplayName("테이블이 비어있으면 SQL 파일을 읽어 insert하고 최종 행 수를 반환한다")
    void seedIfEmpty_insertsWhenTableIsEmpty() {
        int savedCount = flightReferenceDataSeeder.seedIfEmpty("test-seed/sample-seed.sql", this::currentCount);

        assertThat(savedCount).isEqualTo(2);
        assertThat(currentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("이미 데이터가 있으면 SQL을 실행하지 않고 0을 반환한다")
    void seedIfEmpty_skipsWhenTableAlreadyHasData() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO sample (code) VALUES ('EXISTING')");
        }

        int savedCount = flightReferenceDataSeeder.seedIfEmpty("test-seed/sample-seed.sql", this::currentCount);

        assertThat(savedCount).isZero();
        assertThat(currentCount()).isEqualTo(1);
    }

    private long currentCount() {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM sample")) {
            resultSet.next();
            return resultSet.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
