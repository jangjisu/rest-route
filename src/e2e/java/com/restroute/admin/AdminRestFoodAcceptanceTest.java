package com.restroute.admin;

import static com.restroute.support.AdminSessions.loggedIn;
import static com.restroute.support.AdminSessions.saveAdminUser;
import static com.restroute.support.RestStopFixtures.pngBytes;
import static com.restroute.support.RestStopFixtures.saveSyncedRestStop;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

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
 * 관리자 먹거리 편집({@code /api/admin/rest-stops/{code}/foods}) — 조회·추가·수정·삭제·잠금해제와
 * 메뉴 이미지.
 *
 * <p>휴게소 편집과 갈리는 지점이 하나 있다. 먹거리는 <b>관리자가 직접 추가한 메뉴</b>와
 * <b>동기화로 들어온 메뉴</b>가 섞여 있고, 삭제는 앞의 것만 허용한다 — 동기화 메뉴는 지워봐야
 * 다음 배치가 외부 API에서 다시 만들어오므로 지우는 시늉만 하게 된다. 그 규칙이 실제로 걸리는지가
 * 이 파일의 핵심이다.
 *
 * <p>이미지는 multipart 업로드와 바이너리 응답이라 실제 HTTP 왕복이 있어야 검증된다.
 */
class AdminRestFoodAcceptanceTest extends AcceptanceTest {

    private static final String SYNCED_CODE = "A00001";
    private static final String BASE = "/api/admin/rest-stops/" + SYNCED_CODE + "/foods";

    private static final int SUCCESS_STATUS = 200;
    private static final int NO_CONTENT_STATUS = 204;
    private static final int NOT_FOUND_STATUS = 404;

    private static final String FOOD_NAME = "안성한우국밥";
    private static final String FOOD_COST = "9000";

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RestStopRepository restStopRepository;

    @BeforeEach
    void signInAndSeedRestStop() {
        saveAdminUser(adminUserRepository, passwordEncoder);
        saveSyncedRestStop(restStopRepository, SYNCED_CODE, "안성휴게소");
    }

    private Map<String, String> foodPayload(String foodName, String foodCost) {
        return Map.of("foodName", foodName, "foodCost", foodCost, "description", "설명");
    }

    private Response createFood(String foodName) {
        return loggedIn()
                .contentType(ContentType.JSON)
                .body(foodPayload(foodName, FOOD_COST))
                .when()
                .post(BASE);
    }

    private Integer createdFoodId(String foodName) {
        return createFood(foodName).then().statusCode(SUCCESS_STATUS).extract().path("data.id");
    }

    // --- 조회·추가 -----------------------------------------------------------

    @Test
    @DisplayName("등록된 먹거리가 없으면 빈 목록이 나간다")
    void noFoods_respondsEmptyList() {
        loggedIn().when().get(BASE).then().statusCode(SUCCESS_STATUS).body("data", hasSize(0));
    }

    /**
     * 관리자가 직접 추가한 메뉴는 처음부터 잠겨 있다 — 동기화가 만들지 않은 행이므로 배치가
     * 건드리면 안 된다.
     */
    @Test
    @DisplayName("관리자가 추가한 메뉴는 잠긴 상태로 목록에 들어간다")
    void createdFood_isLockedAndListed() {
        createFood(FOOD_NAME)
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.foodName", equalTo(FOOD_NAME))
                .body("data.foodCost", equalTo(FOOD_COST))
                .body("data.adminOverridden", equalTo(true))
                .body("data.adminCreated", equalTo(true))
                .body("data.hasImage", equalTo(false));

        loggedIn()
                .when()
                .get(BASE)
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data", hasSize(1))
                .body("data[0].foodName", equalTo(FOOD_NAME));
    }

    // --- 수정 ---------------------------------------------------------------

    @Test
    @DisplayName("메뉴를 수정하면 값이 바뀌고 다시 읽어도 유지된다")
    void updateFood_persists() {
        Integer foodId = createdFoodId(FOOD_NAME);

        loggedIn()
                .contentType(ContentType.JSON)
                .body(foodPayload("안성한우국밥(개선)", "10000"))
                .when()
                .put(BASE + "/" + foodId)
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.foodName", equalTo("안성한우국밥(개선)"))
                .body("data.foodCost", equalTo("10000"));

        loggedIn()
                .when()
                .get(BASE)
                .then()
                .body("data[0].foodName", equalTo("안성한우국밥(개선)"))
                .body("data[0].foodCost", equalTo("10000"));
    }

    @Test
    @DisplayName("없는 메뉴는 수정도 삭제도 404다")
    void unknownFood_responds404() {
        loggedIn()
                .contentType(ContentType.JSON)
                .body(foodPayload(FOOD_NAME, FOOD_COST))
                .when()
                .put(BASE + "/999999")
                .then()
                .statusCode(NOT_FOUND_STATUS);

        loggedIn().when().delete(BASE + "/999999").then().statusCode(NOT_FOUND_STATUS);
    }

    // --- 삭제: 관리자가 만든 메뉴만 -------------------------------------------

    @Test
    @DisplayName("관리자가 추가한 메뉴는 삭제되고 목록에서 사라진다")
    void deleteAdminCreatedFood_removesIt() {
        Integer foodId = createdFoodId(FOOD_NAME);

        loggedIn().when().delete(BASE + "/" + foodId).then().statusCode(NO_CONTENT_STATUS);

        loggedIn().when().get(BASE).then().statusCode(SUCCESS_STATUS).body("data", hasSize(0));
    }

    // --- 잠금해제 -----------------------------------------------------------

    /**
     * 잠금을 풀면 다음 배치가 다시 값을 갱신할 수 있게 된다. 관리자가 추가한 메뉴에도 같은
     * 잠금해제가 걸리는지를 본다.
     */
    @Test
    @DisplayName("잠금을 풀면 메뉴는 남고 잠금만 해제된다")
    void clearOverride_keepsFoodAndUnlocks() {
        Integer foodId = createdFoodId(FOOD_NAME);

        loggedIn()
                .when()
                .delete(BASE + "/" + foodId + "/override")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.foodName", equalTo(FOOD_NAME))
                .body("data.adminOverridden", equalTo(false));

        loggedIn().when().get(BASE).then().body("data", hasSize(1)).body("data[0].adminOverridden", equalTo(false));
    }

    // --- 메뉴 이미지 ---------------------------------------------------------

    /** 이미지가 없는 메뉴는 오류가 아니라 "보여줄 것이 없음"이다. */
    @Test
    @DisplayName("메뉴 이미지가 없으면 204가 나간다")
    void foodWithoutImage_responds204() {
        Integer foodId = createdFoodId(FOOD_NAME);

        loggedIn().when().get(BASE + "/" + foodId + "/image").then().statusCode(NO_CONTENT_STATUS);
    }

    @Test
    @DisplayName("메뉴가 없으면 이미지 조회는 404다")
    void unknownFoodImage_responds404() {
        loggedIn().when().get(BASE + "/999999/image").then().statusCode(NOT_FOUND_STATUS);
    }

    /**
     * multipart 업로드와 바이너리 응답은 실제 HTTP 왕복이 있어야 도는 구간이다. 업로드한 뒤
     * {@code hasImage}가 켜지는지까지 확인해야, 저장은 됐는데 목록이 모르는 상태를 잡아낸다.
     */
    @Test
    @DisplayName("메뉴 이미지를 올리면 webp로 내려받히고 목록에 반영된다")
    void uploadFoodImage_isServedAndReflectedInList() {
        Integer foodId = createdFoodId(FOOD_NAME);

        loggedIn()
                .multiPart("file", "food.png", pngBytes(600), "image/png")
                .when()
                .put(BASE + "/" + foodId + "/image")
                .then()
                .statusCode(NO_CONTENT_STATUS);

        loggedIn()
                .when()
                .get(BASE + "/" + foodId + "/image")
                .then()
                .statusCode(SUCCESS_STATUS)
                .contentType("image/webp");

        loggedIn().when().get(BASE).then().body("data[0].hasImage", equalTo(true));
    }

    @Test
    @DisplayName("메뉴 이미지를 지우면 다시 204가 된다")
    void deleteFoodImage_returnsToNoContent() {
        Integer foodId = createdFoodId(FOOD_NAME);
        loggedIn()
                .multiPart("file", "food.png", pngBytes(600), "image/png")
                .when()
                .put(BASE + "/" + foodId + "/image");

        loggedIn().when().delete(BASE + "/" + foodId + "/image").then().statusCode(NO_CONTENT_STATUS);

        loggedIn().when().get(BASE + "/" + foodId + "/image").then().statusCode(NO_CONTENT_STATUS);
        loggedIn().when().get(BASE).then().body("data[0].hasImage", equalTo(false));
    }

    // --- 없는 휴게소 ---------------------------------------------------------

    @Test
    @DisplayName("없는 휴게소에 메뉴를 추가하면 404가 나간다")
    void createFoodOnUnknownRestStop_responds404() {
        loggedIn()
                .contentType(ContentType.JSON)
                .body(foodPayload(FOOD_NAME, FOOD_COST))
                .when()
                .post("/api/admin/rest-stops/UNKNOWN/foods")
                .then()
                .statusCode(NOT_FOUND_STATUS);
    }
}
