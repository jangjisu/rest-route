package com.restroute.admin;

import static com.restroute.support.AdminSessions.loggedIn;
import static com.restroute.support.AdminSessions.saveAdminUser;
import static com.restroute.support.RestStopFixtures.saveSyncedRestStop;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.restroute.AcceptanceTest;
import com.restroute.admin.repository.AdminUserRepository;
import com.restroute.reststop.repository.RestStopRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 관리자 휴게소 편집({@code /api/admin/rest-stops}) — 등록·조회·저장·잠금해제.
 *
 * <p>이 화면의 핵심 계약은 <b>동기화 잠금</b>이다. 관리자가 값을 고치면 {@code adminOverridden}이
 * {@code true}가 되어 매일 도는 배치가 그 행을 건너뛰고, 잠금을 풀면 다시 자동 갱신 대상이 된다.
 * 잠금이 걸리지 않으면 관리자가 고친 값이 다음 날 조용히 덮여 사라진다 — 화면에서는 저장 직후엔
 * 멀쩡해 보이므로 눈으로 알아채기 어려운 종류의 회귀다.
 *
 * <p>그래서 여기서 보는 것은 응답 형태만이 아니라 <b>저장 후 다시 읽었을 때의 상태</b>다.
 *
 * <p>관리자 API는 모두 인증과 CSRF를 함께 요구한다 — 그 흐름은 {@code AdminSessions}가 재현하고,
 * 빠졌을 때 무엇이 막히는지는 {@code AdminAuthorizationAcceptanceTest}가 따로 고정한다.
 */
class AdminRestStopEditAcceptanceTest extends AcceptanceTest {

    private static final String BASE = "/api/admin/rest-stops";

    private static final int SUCCESS_STATUS = 200;
    private static final int NOT_FOUND_STATUS = 404;

    /** 동기화로 들어온 휴게소의 코드. 관리자 생성과 달리 외부가 준 코드를 그대로 쓴다. */
    private static final String SYNCED_CODE = "A00001";

    private static final String ORIGINAL_NAME = "안성휴게소";
    private static final String EDITED_NAME = "안성휴게소(수정)";

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RestStopRepository restStopRepository;

    @BeforeEach
    void signInAsAdmin() {
        saveAdminUser(adminUserRepository, passwordEncoder);
    }

    private Map<String, String> updatePayload(String unitName) {
        return Map.of(
                "unitName", unitName,
                "routeNo", "0010",
                "routeName", "경부선",
                "xValue", "127.0425",
                "yValue", "37.4599",
                "telNo", "031-000-0000",
                "brand", "투썸플레이스",
                "routeCode", "0010",
                "svarAddr", "경기도 안성시",
                "convenience", "편의점");
    }

    private Response findEditable(String serviceAreaCode) {
        return loggedIn().when().get(BASE + "/" + serviceAreaCode + "/editable");
    }

    private Response saveEditable(String serviceAreaCode, Map<String, String> payload) {
        return loggedIn()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .put(BASE + "/" + serviceAreaCode + "/editable");
    }

    // --- 조회 ---------------------------------------------------------------

    @Test
    @DisplayName("편집 대상 휴게소가 없으면 404가 나간다")
    void unknownRestStop_responds404() {
        findEditable("UNKNOWN")
                .then()
                .statusCode(NOT_FOUND_STATUS)
                .body("code", equalTo("NOT_FOUND"))
                .body("message", equalTo("Rest stop not found: UNKNOWN"));
    }

    /** 아직 아무도 손대지 않은 휴게소는 잠금이 풀린 상태다 — 배치가 매일 갱신해도 되는 행. */
    @Test
    @DisplayName("동기화된 휴게소는 잠금이 걸려 있지 않다")
    void syncedRestStop_isNotLocked() {
        saveSyncedRestStop(restStopRepository, SYNCED_CODE, ORIGINAL_NAME);

        findEditable(SYNCED_CODE)
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.unitName", equalTo(ORIGINAL_NAME))
                .body("data.adminOverridden", equalTo(false));
    }

    // --- 등록 ---------------------------------------------------------------

    @Test
    @DisplayName("관리자가 휴게소를 새로 등록하면 코드가 발급되고 조회된다")
    void create_issuesCodeAndBecomesFindable() {
        String createdCode = loggedIn()
                .contentType(ContentType.JSON)
                .body(updatePayload("신규휴게소"))
                .when()
                .post(BASE)
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.serviceAreaCode", notNullValue())
                .body("data.unitName", equalTo("신규휴게소"))
                .extract()
                .path("data.serviceAreaCode");

        findEditable(createdCode).then().statusCode(SUCCESS_STATUS).body("data.unitName", equalTo("신규휴게소"));
    }

    // --- 저장과 잠금 ---------------------------------------------------------

    /**
     * 저장 응답만 보고 끝내지 않고 <b>다시 조회해</b> 확인한다. 응답을 만들어 내려주는 것과 실제로
     * 커밋되는 것은 다른 문제이고, 트랜잭션 경계가 어긋나면 응답만 맞고 DB는 그대로일 수 있다.
     */
    @Test
    @DisplayName("저장하면 값이 바뀌고 동기화 잠금이 걸린다")
    void update_appliesValuesAndLocksFromSync() {
        saveSyncedRestStop(restStopRepository, SYNCED_CODE, ORIGINAL_NAME);

        saveEditable(SYNCED_CODE, updatePayload(EDITED_NAME))
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.unitName", equalTo(EDITED_NAME))
                .body("data.adminOverridden", equalTo(true));

        findEditable(SYNCED_CODE)
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.unitName", equalTo(EDITED_NAME))
                .body("data.adminOverridden", equalTo(true));
    }

    /** 잠금을 풀면 다음 배치부터 다시 자동 갱신 대상이 된다. 값 자체는 그대로 남는다. */
    @Test
    @DisplayName("잠금을 풀면 값은 남고 잠금만 해제된다")
    void clearOverride_keepsValuesAndUnlocks() {
        saveSyncedRestStop(restStopRepository, SYNCED_CODE, ORIGINAL_NAME);
        saveEditable(SYNCED_CODE, updatePayload(EDITED_NAME));

        loggedIn()
                .when()
                .delete(BASE + "/" + SYNCED_CODE + "/editable/override")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.adminOverridden", equalTo(false));

        findEditable(SYNCED_CODE)
                .then()
                .body("data.unitName", equalTo(EDITED_NAME))
                .body("data.adminOverridden", equalTo(false));
    }

    @Test
    @DisplayName("없는 휴게소는 저장도 잠금해제도 404다")
    void mutationsOnUnknownRestStop_respond404() {
        saveEditable("UNKNOWN", updatePayload(EDITED_NAME)).then().statusCode(NOT_FOUND_STATUS);

        loggedIn().when().delete(BASE + "/UNKNOWN/editable/override").then().statusCode(NOT_FOUND_STATUS);
    }
}
