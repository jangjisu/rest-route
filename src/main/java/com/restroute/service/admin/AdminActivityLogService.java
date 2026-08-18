package com.restroute.service.admin;

import com.restroute.domain.AdminActivityLogEntity;
import com.restroute.repository.AdminActivityLogRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminActivityLogService {

    private static final String PRODUCT_SALES_UPLOAD_MESSAGE = "상품 판매순위 CSV(%s)를 업로드했습니다.";
    private static final String STORE_SALES_UPLOAD_MESSAGE = "매장 판매순위 CSV(%s)를 업로드했습니다.";
    private static final String BACKFILL_MESSAGE = "전체 휴게소명 매핑을 실행했습니다.";
    private static final String IMAGE_SAVED_MESSAGE = "휴게소(%s) 이미지를 등록했습니다.";
    private static final String IMAGE_DELETED_MESSAGE = "휴게소(%s) 이미지를 삭제했습니다.";
    private static final String EDITED_MESSAGE = "%s 정보를 수정했습니다.";
    private static final String OVERRIDE_CLEARED_MESSAGE = "%s의 동기화 잠금을 해제했습니다.";
    private static final String CREATED_MESSAGE = "%s 휴게소를 새로 등록했습니다.";
    private static final String CUSTOM_FOOD_ADDED_MESSAGE = "%s 메뉴를 추가했습니다.";
    private static final String CUSTOM_FOOD_EDITED_MESSAGE = "%s 메뉴를 수정했습니다.";
    private static final String CUSTOM_FOOD_OVERRIDE_CLEARED_MESSAGE = "%s 메뉴의 동기화 잠금을 해제했습니다.";
    private static final String CUSTOM_FOOD_DELETED_MESSAGE = "메뉴(%d)를 삭제했습니다.";
    private static final String CUSTOM_FOOD_IMAGE_SAVED_MESSAGE = "메뉴(%d) 이미지를 등록했습니다.";
    private static final String CUSTOM_FOOD_IMAGE_DELETED_MESSAGE = "메뉴(%d) 이미지를 삭제했습니다.";
    private static final String OIL_STATION_LINKED_MESSAGE = "%s 주유소를 %s에 연결했습니다.";
    private static final String OIL_STATION_UNLINKED_MESSAGE = "%s 주유소의 연결을 해제했습니다.";
    private static final String OIL_STATION_OVERRIDE_CLEARED_MESSAGE = "%s 주유소를 자동 매칭으로 되돌렸습니다.";
    private static final String FLIGHT_HOLIDAY_ADDED_MESSAGE = "공휴일(%s %s)을 추가했습니다.";
    private static final String FLIGHT_HOLIDAY_DELETED_MESSAGE = "공휴일(%s)을 삭제했습니다.";

    private final AdminActivityLogRepository adminActivityLogRepository;
    private final Clock clock;

    @Transactional
    public void logProductSalesUpload(Authentication authentication, String fileName) {
        saveLog(authentication, String.format(PRODUCT_SALES_UPLOAD_MESSAGE, fileName));
    }

    @Transactional
    public void logStoreSalesUpload(Authentication authentication, String fileName) {
        saveLog(authentication, String.format(STORE_SALES_UPLOAD_MESSAGE, fileName));
    }

    @Transactional
    public void logBackfill(Authentication authentication) {
        saveLog(authentication, BACKFILL_MESSAGE);
    }

    @Transactional
    public void logRestStopImageSaved(Authentication authentication, String serviceAreaCode) {
        saveLog(authentication, String.format(IMAGE_SAVED_MESSAGE, serviceAreaCode));
    }

    @Transactional
    public void logRestStopImageDeleted(Authentication authentication, String serviceAreaCode) {
        saveLog(authentication, String.format(IMAGE_DELETED_MESSAGE, serviceAreaCode));
    }

    @Transactional
    public void logRestStopEdited(Authentication authentication, String unitName) {
        saveLog(authentication, String.format(EDITED_MESSAGE, unitName));
    }

    @Transactional
    public void logRestStopOverrideCleared(Authentication authentication, String unitName) {
        saveLog(authentication, String.format(OVERRIDE_CLEARED_MESSAGE, unitName));
    }

    @Transactional
    public void logRestStopCreated(Authentication authentication, String unitName) {
        saveLog(authentication, String.format(CREATED_MESSAGE, unitName));
    }

    @Transactional
    public void logCustomFoodAdded(Authentication authentication, String foodName) {
        saveLog(authentication, String.format(CUSTOM_FOOD_ADDED_MESSAGE, foodName));
    }

    @Transactional
    public void logCustomFoodEdited(Authentication authentication, String foodName) {
        saveLog(authentication, String.format(CUSTOM_FOOD_EDITED_MESSAGE, foodName));
    }

    @Transactional
    public void logCustomFoodOverrideCleared(Authentication authentication, String foodName) {
        saveLog(authentication, String.format(CUSTOM_FOOD_OVERRIDE_CLEARED_MESSAGE, foodName));
    }

    @Transactional
    public void logCustomFoodDeleted(Authentication authentication, Long foodId) {
        saveLog(authentication, String.format(CUSTOM_FOOD_DELETED_MESSAGE, foodId));
    }

    @Transactional
    public void logCustomFoodImageSaved(Authentication authentication, Long foodId) {
        saveLog(authentication, String.format(CUSTOM_FOOD_IMAGE_SAVED_MESSAGE, foodId));
    }

    @Transactional
    public void logCustomFoodImageDeleted(Authentication authentication, Long foodId) {
        saveLog(authentication, String.format(CUSTOM_FOOD_IMAGE_DELETED_MESSAGE, foodId));
    }

    @Transactional
    public void logOilStationLinked(Authentication authentication, String oilStationName, String restStopName) {
        saveLog(authentication, String.format(OIL_STATION_LINKED_MESSAGE, oilStationName, restStopName));
    }

    @Transactional
    public void logOilStationUnlinked(Authentication authentication, String oilStationName) {
        saveLog(authentication, String.format(OIL_STATION_UNLINKED_MESSAGE, oilStationName));
    }

    @Transactional
    public void logOilStationOverrideCleared(Authentication authentication, String oilStationName) {
        saveLog(authentication, String.format(OIL_STATION_OVERRIDE_CLEARED_MESSAGE, oilStationName));
    }

    @Transactional
    public void logFlightHolidayAdded(Authentication authentication, String date, String name) {
        saveLog(authentication, String.format(FLIGHT_HOLIDAY_ADDED_MESSAGE, date, name));
    }

    @Transactional
    public void logFlightHolidayDeleted(Authentication authentication, String date) {
        saveLog(authentication, String.format(FLIGHT_HOLIDAY_DELETED_MESSAGE, date));
    }

    @Transactional(readOnly = true)
    public List<AdminActivityLogEntity> findRecent() {
        return adminActivityLogRepository.findTop50ByOrderByCreatedAtDesc();
    }

    private void saveLog(Authentication authentication, String message) {
        adminActivityLogRepository.save(
                AdminActivityLogEntity.of(authentication.getName(), message, LocalDateTime.now(clock)));
    }
}
