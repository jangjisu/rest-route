package com.restroute;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.restroute.support.DatabaseCleaner;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * E2E 인수 테스트의 공통 기반. 실제 서버를 랜덤 포트로 띄우고, DB는 운영과 같은 MySQL
 * 컨테이너를, 외부 API 자리에는 WireMock을 세운다 — 즉 <b>외부 서버만</b> 가짜고
 * Feign 설정·타임아웃·HTTP 전송·JSON 역직렬화는 전부 실제로 동작한다.
 *
 * <p>{@code src/test}의 단위 테스트가 Mockito로 {@code KakaoMapClient}를 통째로 바꿔치기해
 * 그 아래를 아예 실행하지 않는 것과 대비되는 지점이고, 이 계층을 두는 이유이기도 하다.
 */
@ActiveProfiles("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTest {

    /**
     * JUnit의 {@code @Container} 대신 static 블록에서 직접 띄운다. {@code @Container}는 테스트
     * 클래스가 끝날 때 컨테이너를 내리는데, Spring은 컨텍스트를 캐싱해 다음 클래스에 재사용하므로
     * 두 번째 클래스부터 이미 종료된 컨테이너를 가리키는 커넥션 풀을 물게 된다. JVM당 한 번만
     * 띄우고 내리지 않으면(정리는 Testcontainers의 Ryuk이 맡는다) 그 문제가 생기지 않는다.
     */
    @ServiceConnection
    protected static final MySQLContainer<?> DATABASE = new MySQLContainer<>("mysql:8.0");

    /** 카카오 로컬·모빌리티가 같은 서버 하나를 공유한다 — 경로가 서로 달라 충돌하지 않는다. */
    protected static final WireMockServer KAKAO = new WireMockServer(options().dynamicPort());

    static {
        DATABASE.start();
        KAKAO.start();
    }

    @LocalServerPort
    private int port;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @DynamicPropertySource
    static void kakaoApiUrls(DynamicPropertyRegistry registry) {
        registry.add("kakao.local.url", KAKAO::baseUrl);
        registry.add("kakao.navi.url", KAKAO::baseUrl);
    }

    @BeforeEach
    void setUpAcceptance() {
        RestAssured.port = port;
        KAKAO.resetAll();
        databaseCleaner.clear();
    }
}
