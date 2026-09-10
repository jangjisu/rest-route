package com.restroute.admin;

import static com.restroute.support.AdminSessions.loggedIn;
import static com.restroute.support.AdminSessions.saveAdminUser;
import static com.restroute.support.RestStopFixtures.pngBytes;
import static com.restroute.support.RestStopFixtures.saveSyncedRestStop;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.restroute.AcceptanceTest;
import com.restroute.admin.repository.AdminUserRepository;
import com.restroute.reststop.repository.RestStopRepository;
import java.nio.charset.Charset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 관리자 업로드 계열 — 휴게소 대표 이미지(multipart)와 CSV 일괄 등록, 그리고 휴게소명 매칭 백필.
 *
 * <p>이 파일이 다루는 것은 전부 <b>파일이 오가는 경로</b>다. multipart 파싱, 인코딩, 이미지 변환은
 * 서비스만 호출하는 테스트에서는 건너뛰어지는 구간이고, 특히 CSV는 <b>CP949</b>로 올라온다 —
 * 한글이 깨지면 매칭이 통째로 실패하는데 UTF-8로 읽어도 예외는 안 나서 조용히 0건이 된다.
 *
 * <p>이미지 업로드는 서버가 {@code ImageIO}로 읽어 webp로 변환하므로 실제 PNG를 보내야 한다.
 */
class AdminRestStopUploadAcceptanceTest extends AcceptanceTest {

    private static final String SYNCED_CODE = "A00001";
    private static final String UNIT_NAME = "안성휴게소";

    private static final String IMAGE_PATH = "/api/admin/rest-stops/" + SYNCED_CODE + "/image";
    private static final String PUBLIC_DETAIL_IMAGE = "/api/rest-stops/" + SYNCED_CODE + "/images/detail";
    private static final String PUBLIC_LIST_IMAGE = "/api/rest-stops/" + SYNCED_CODE + "/images/list";

    private static final String BACKFILL_PATH = "/api/admin/sales-rankings/backfill";
    private static final String RESTROOM_UPLOAD_PATH = "/api/admin/rest-stops/restrooms";

    private static final int SUCCESS_STATUS = 200;
    private static final int NO_CONTENT_STATUS = 204;
    private static final int NOT_FOUND_STATUS = 404;

    /** 공공기관이 내려주는 CSV 인코딩. UTF-8로 읽으면 한글이 깨진다. */
    private static final Charset CP949 = Charset.forName("MS949");

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RestStopRepository restStopRepository;

    @BeforeEach
    void signInAndSeedRestStop() {
        saveAdminUser(adminUserRepository, passwordEncoder);
        saveSyncedRestStop(restStopRepository, SYNCED_CODE, UNIT_NAME);
    }

    // --- 대표 이미지 ---------------------------------------------------------

    /**
     * 업로드 한 번이 <b>상세용과 목록용 두 벌</b>을 만든다. 공개 조회까지 이어서 확인해야, 저장은
     * 됐는데 사용자 화면에는 안 나가는 상태를 잡아낸다.
     */
    @Test
    @DisplayName("대표 이미지를 올리면 상세용·목록용이 모두 공개 조회에 나타난다")
    void uploadImage_servesBothSizesOnPublicApi() {
        loggedIn()
                .multiPart("file", "rest-stop.png", pngBytes(800), "image/png")
                .when()
                .put(IMAGE_PATH)
                .then()
                .statusCode(NO_CONTENT_STATUS);

        given().when()
                .get(PUBLIC_DETAIL_IMAGE)
                .then()
                .statusCode(SUCCESS_STATUS)
                .contentType("image/webp")
                .header("ETag", notNullValue());

        given().when().get(PUBLIC_LIST_IMAGE).then().statusCode(SUCCESS_STATUS).contentType("image/webp");
    }

    @Test
    @DisplayName("대표 이미지를 지우면 공개 조회가 다시 204가 된다")
    void deleteImage_returnsPublicApiToNoContent() {
        loggedIn()
                .multiPart("file", "rest-stop.png", pngBytes(800), "image/png")
                .when()
                .put(IMAGE_PATH);

        loggedIn().when().delete(IMAGE_PATH).then().statusCode(NO_CONTENT_STATUS);

        given().when().get(PUBLIC_DETAIL_IMAGE).then().statusCode(NO_CONTENT_STATUS);
    }

    @Test
    @DisplayName("없는 휴게소에는 이미지를 올릴 수 없다")
    void uploadImageOnUnknownRestStop_responds404() {
        loggedIn()
                .multiPart("file", "rest-stop.png", pngBytes(800), "image/png")
                .when()
                .put("/api/admin/rest-stops/UNKNOWN/image")
                .then()
                .statusCode(NOT_FOUND_STATUS);
    }

    // --- CSV 업로드 ----------------------------------------------------------

    /**
     * 화장실 현황 CSV는 휴게소명으로 매칭되므로 한글이 살아 있어야 한다. CP949로 인코딩해 올리고,
     * 매칭 건수가 0이 아닌지로 인코딩이 제대로 풀렸는지를 본다.
     */
    @Test
    @DisplayName("CP949 CSV를 올리면 한글 휴게소명으로 매칭된다")
    void uploadRestroomCsv_matchesByKoreanName() {
        // 헤더 이름은 파서가 요구하는 값 그대로여야 한다 — 다르면 필수 헤더 검증에서 400이다.
        String csv = "노선,시설명,남자_변기수,여자_변기수\n경부선," + UNIT_NAME + ",10,8\n";

        loggedIn()
                .multiPart("restroomFile", "restroom.csv", csv.getBytes(CP949), "text/csv")
                .when()
                .post(RESTROOM_UPLOAD_PATH)
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"));

        // 매칭이 됐다면 공개 편의시설 조회에 변기 수가 실린다.
        given().when()
                .get("/api/rest-stops/" + SYNCED_CODE + "/facilities")
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("data.maleToiletCount", equalTo(10))
                .body("data.femaleToiletCount", equalTo(8));
    }

    // --- 백필 ---------------------------------------------------------------

    /**
     * 백필은 외부 호출 없이 이미 저장된 데이터끼리 휴게소명을 맞춰 연결을 채운다. 데이터가 없어도
     * 실패가 아니라 0건으로 끝나야 한다 — 관리자 대시보드에서 아무 때나 누를 수 있는 버튼이다.
     */
    @Test
    @DisplayName("연결할 데이터가 없어도 백필은 0건으로 성공한다")
    void backfillWithNothingToMatch_succeedsWithZeroCounts() {
        loggedIn()
                .when()
                .post(BACKFILL_PATH)
                .then()
                .statusCode(SUCCESS_STATUS)
                .body("code", equalTo("SUCCESS"))
                .body("data", notNullValue());
    }
}
