package com.restroute.admin;

import static com.restroute.support.AdminSessions.loggedIn;
import static com.restroute.support.AdminSessions.saveAdminUser;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import com.restroute.AcceptanceTest;
import com.restroute.admin.repository.AdminUserRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 관리자 항공 휴일 관리({@code /api/admin/flights/holidays}) — 조회·등록·삭제.
 *
 * <p>관리자 영역에서 <b>생성과 삭제가 모두 열려 있는 유일한 자원</b>이라 CRUD 한 바퀴가 온전히
 * 돈다. 다른 관리자 화면은 동기화된 데이터를 고치거나 연결을 거는 쪽이라 생명주기가 반쪽이다.
 *
 * <p>관리자가 등록한 휴일은 공개 조회({@code GET /api/flights/holidays})에도 나타나야 한다 —
 * 관리자 화면에만 보이고 사용자에게 안 나가면 등록한 의미가 없는데, 두 엔드포인트가 서로 다른
 * 서비스를 타므로 어긋날 수 있다. 그 연결까지 여기서 확인한다.
 */
class AdminFlightHolidayAcceptanceTest extends AcceptanceTest {

    private static final String ADMIN_BASE = "/api/admin/flights/holidays";
    private static final String PUBLIC_BASE = "/api/flights/holidays";

    private static final int SUCCESS_STATUS = 200;
    private static final int NO_CONTENT_STATUS = 204;
    private static final int NOT_FOUND_STATUS = 404;

    private static final String HOLIDAY_DATE = "2026-12-25";
    private static final String HOLIDAY_NAME = "크리스마스";

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void signInAsAdmin() {
        saveAdminUser(adminUserRepository, passwordEncoder);
    }

    private Response create(String date, String name) {
        return loggedIn()
                .contentType(ContentType.JSON)
                .body(Map.of("date", date, "name", name))
                .when()
                .post(ADMIN_BASE);
    }

    private Integer createdId(String date, String name) {
        return create(date, name).then().statusCode(SUCCESS_STATUS).extract().path("data.id");
    }

    @Test
    @DisplayName("등록된 휴일이 없으면 빈 목록이 나간다")
    void noHolidays_respondsEmptyList() {
        loggedIn()
                .when()
                .get(ADMIN_BASE)
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data", hasSize(0));
    }

    @Test
    @DisplayName("휴일을 등록하면 목록에 나타난다")
    void create_appearsInList() {
        create(HOLIDAY_DATE, HOLIDAY_NAME).then().statusCode(SUCCESS_STATUS).body("data.name", equalTo(HOLIDAY_NAME));

        loggedIn().when().get(ADMIN_BASE).then().body("data", hasSize(1)).body("data[0].name", equalTo(HOLIDAY_NAME));
    }

    /**
     * 관리자 화면과 사용자 화면이 서로 다른 엔드포인트라, 등록이 한쪽에만 반영되면 관리자는
     * 등록했다고 믿는데 사용자에게는 안 나가는 상태가 된다.
     */
    @Test
    @DisplayName("관리자가 등록한 휴일은 공개 조회에도 나타난다")
    void createdHoliday_isVisibleOnPublicApi() {
        create(HOLIDAY_DATE, HOLIDAY_NAME);

        // 공개 조회는 로그인 없이 부른다 — 사용자가 실제로 보는 경로가 그쪽이다.
        given().when().get(PUBLIC_BASE).then().statusCode(SUCCESS_STATUS).body("data.name", hasItem(HOLIDAY_NAME));
    }

    @Test
    @DisplayName("휴일을 삭제하면 목록에서 사라진다")
    void delete_removesFromList() {
        Integer holidayId = createdId(HOLIDAY_DATE, HOLIDAY_NAME);

        loggedIn().when().delete(ADMIN_BASE + "/" + holidayId).then().statusCode(NO_CONTENT_STATUS);

        loggedIn().when().get(ADMIN_BASE).then().body("data", hasSize(0));
    }

    @Test
    @DisplayName("없는 휴일을 삭제하면 404가 나간다")
    void deleteUnknownHoliday_responds404() {
        loggedIn().when().delete(ADMIN_BASE + "/999999").then().statusCode(NOT_FOUND_STATUS);
    }
}
