package com.restroute.support;

import com.restroute.client.response.HighwayServiceAreaInfoItem;
import com.restroute.client.response.HighwayServiceAreaInfoResponse;
import com.restroute.client.response.RestEventItem;
import com.restroute.client.response.RestEventResponse;
import com.restroute.client.response.RestOilItem;
import com.restroute.client.response.RestOilPriceItem;
import com.restroute.client.response.RestOilPriceResponse;
import com.restroute.client.response.RestOilResponse;
import com.restroute.client.response.RestStopDetailItem;
import com.restroute.client.response.RestStopDetailResponse;
import com.restroute.client.response.RestStopItem;
import com.restroute.client.response.RestStopResponse;
import com.restroute.client.response.RestThemeItem;
import com.restroute.client.response.RestThemeResponse;
import java.lang.reflect.Constructor;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

public final class RestStopTestFixtures {

    private RestStopTestFixtures() {}

    public static RestStopItem restStopItem(String unitCode, String unitName) {
        return restStopItem(unitCode, unitName, "A00001");
    }

    public static RestStopItem restStopItem(String unitCode, String unitName, String serviceAreaCode) {
        RestStopItem item = instantiate(RestStopItem.class);
        ReflectionTestUtils.setField(item, "unitCode", unitCode);
        ReflectionTestUtils.setField(item, "unitName", unitName);
        ReflectionTestUtils.setField(item, "routeNo", "0010");
        ReflectionTestUtils.setField(item, "routeName", "경부선");
        ReflectionTestUtils.setField(item, "xValue", "127.042514");
        ReflectionTestUtils.setField(item, "yValue", "37.459939");
        ReflectionTestUtils.setField(item, "stdRestCd", "000001");
        ReflectionTestUtils.setField(item, "serviceAreaCode", serviceAreaCode);
        return item;
    }

    public static RestStopResponse restStopResponse(String code, String pageSize, List<RestStopItem> items) {
        RestStopResponse response = instantiate(RestStopResponse.class);
        ReflectionTestUtils.setField(response, "code", code);
        ReflectionTestUtils.setField(response, "message", "인증키가 유효합니다.");
        ReflectionTestUtils.setField(response, "count", String.valueOf(items.size()));
        ReflectionTestUtils.setField(response, "pageSize", pageSize);
        ReflectionTestUtils.setField(response, "list", items);
        return response;
    }

    public static RestStopDetailItem restStopDetailItem(String serviceAreaCode, String serviceAreaName) {
        RestStopDetailItem item = instantiate(RestStopDetailItem.class);
        ReflectionTestUtils.setField(item, "routeName", "경부선");
        ReflectionTestUtils.setField(item, "serviceAreaCode", serviceAreaCode);
        ReflectionTestUtils.setField(item, "serviceAreaName", serviceAreaName);
        ReflectionTestUtils.setField(item, "telNo", "054-751-6890");
        ReflectionTestUtils.setField(item, "brand", "투썸플레이스");
        ReflectionTestUtils.setField(item, "routeCode", "0010");
        ReflectionTestUtils.setField(item, "serviceAreaCode2", "000054");
        ReflectionTestUtils.setField(item, "svarAddr", "경북 경주시 건천읍방내리 14");
        ReflectionTestUtils.setField(item, "convenience", "수유실");
        ReflectionTestUtils.setField(item, "maintenanceYn", "X");
        ReflectionTestUtils.setField(item, "truckSaYn", "X");
        return item;
    }

    public static RestStopDetailResponse restStopDetailResponse(
            String code, String pageSize, List<RestStopDetailItem> items) {
        RestStopDetailResponse response = instantiate(RestStopDetailResponse.class);
        ReflectionTestUtils.setField(response, "code", code);
        ReflectionTestUtils.setField(response, "message", "인증키가 유효합니다.");
        ReflectionTestUtils.setField(response, "count", String.valueOf(items.size()));
        ReflectionTestUtils.setField(response, "pageSize", pageSize);
        ReflectionTestUtils.setField(response, "list", items);
        return response;
    }

    public static HighwayServiceAreaInfoItem highwayServiceAreaInfoItem(
            String serviceAreaCode, String serviceAreaName) {
        HighwayServiceAreaInfoItem item = instantiate(HighwayServiceAreaInfoItem.class);
        ReflectionTestUtils.setField(item, "serviceAreaCode", serviceAreaCode);
        ReflectionTestUtils.setField(item, "serviceAreaName", serviceAreaName);
        ReflectionTestUtils.setField(item, "routeCode", "2510");
        ReflectionTestUtils.setField(item, "routeName", "호남선의 지선");
        ReflectionTestUtils.setField(item, "headquartersCode", "400000");
        ReflectionTestUtils.setField(item, "headquartersName", "대전충남본부");
        ReflectionTestUtils.setField(item, "branchOfficeCode", "410200");
        ReflectionTestUtils.setField(item, "branchOfficeName", "대전");
        ReflectionTestUtils.setField(item, "facilityTypeCode", "0");
        ReflectionTestUtils.setField(item, "facilityTypeName", "휴게소");
        ReflectionTestUtils.setField(item, "directionTypeCode", "1");
        ReflectionTestUtils.setField(item, "directionTypeName", "하행");
        ReflectionTestUtils.setField(item, "postalCode", "30535 ");
        ReflectionTestUtils.setField(item, "serviceAreaAddress", "대전광역시 유성구 방현동 86");
        ReflectionTestUtils.setField(item, "compactCarParkingCount", "0");
        ReflectionTestUtils.setField(item, "fullSizeCarParkingCount", "0");
        ReflectionTestUtils.setField(item, "disabledParkingCount", "0");
        ReflectionTestUtils.setField(item, "businessFacilityCode", "A00282");
        ReflectionTestUtils.setField(item, "representativeTelNo", "0420000000");
        return item;
    }

    public static RestThemeItem restThemeItem(String stdRestCd, String itemNm) {
        RestThemeItem item = instantiate(RestThemeItem.class);
        ReflectionTestUtils.setField(item, "stdRestCd", stdRestCd);
        ReflectionTestUtils.setField(item, "stdRestNm", "서울만남(부산)휴게소");
        ReflectionTestUtils.setField(item, "itemNm", itemNm);
        ReflectionTestUtils.setField(item, "detail", "365일 꽃향기가 나는 휴게소입니다");
        ReflectionTestUtils.setField(item, "regId", "MANN003");
        ReflectionTestUtils.setField(item, "regDtime", "07/12/2024 17:21:31.");
        ReflectionTestUtils.setField(item, "lsttmAltrUser", "SYSTEM");
        ReflectionTestUtils.setField(item, "lsttmAltrDttm", "2026-07-28");
        ReflectionTestUtils.setField(item, "svarAddr", "서울 서초구 원지동10-16");
        ReflectionTestUtils.setField(item, "routeCd", "0010");
        ReflectionTestUtils.setField(item, "routeNm", "경부선");
        return item;
    }

    public static RestThemeResponse restThemeResponse(String code, List<RestThemeItem> items) {
        RestThemeResponse response = instantiate(RestThemeResponse.class);
        ReflectionTestUtils.setField(response, "code", code);
        ReflectionTestUtils.setField(response, "message", "인증키가 유효합니다.");
        ReflectionTestUtils.setField(response, "count", String.valueOf(items.size()));
        ReflectionTestUtils.setField(response, "list", items);
        return response;
    }

    public static RestEventItem restEventItem(String stdRestCd, String eventSeq) {
        RestEventItem item = instantiate(RestEventItem.class);
        ReflectionTestUtils.setField(item, "stdRestCd", stdRestCd);
        ReflectionTestUtils.setField(item, "stdRestNm", "서울만남(부산)휴게소");
        ReflectionTestUtils.setField(item, "eventSeq", eventSeq);
        ReflectionTestUtils.setField(item, "eventNm", "TEN+1 이벤트");
        ReflectionTestUtils.setField(item, "eventDetail", "한식당 식사 10번 이용 시 1번 무료");
        ReflectionTestUtils.setField(item, "stime", "2020-01-01");
        ReflectionTestUtils.setField(item, "etime", "2027-12-31");
        ReflectionTestUtils.setField(item, "stdRestGubun", "S");
        ReflectionTestUtils.setField(item, "lastId", "dmsrud527");
        ReflectionTestUtils.setField(item, "lastDtime", "20260621150549");
        ReflectionTestUtils.setField(item, "lsttmAltrUser", "SYSTEM");
        ReflectionTestUtils.setField(item, "lsttmAltrDttm", "2026-07-28");
        ReflectionTestUtils.setField(item, "svarAddr", "서울 서초구 원지동10-16");
        ReflectionTestUtils.setField(item, "routeCd", "0010");
        ReflectionTestUtils.setField(item, "routeNm", "경부선");
        return item;
    }

    public static RestEventResponse restEventResponse(String code, List<RestEventItem> items) {
        RestEventResponse response = instantiate(RestEventResponse.class);
        ReflectionTestUtils.setField(response, "code", code);
        ReflectionTestUtils.setField(response, "message", "인증키가 유효합니다.");
        ReflectionTestUtils.setField(response, "count", String.valueOf(items.size()));
        ReflectionTestUtils.setField(response, "list", items);
        return response;
    }

    public static HighwayServiceAreaInfoResponse highwayServiceAreaInfoResponse(
            String code, List<HighwayServiceAreaInfoItem> items) {
        HighwayServiceAreaInfoResponse response = instantiate(HighwayServiceAreaInfoResponse.class);
        ReflectionTestUtils.setField(response, "code", code);
        ReflectionTestUtils.setField(response, "message", "인증키가 유효합니다.");
        ReflectionTestUtils.setField(response, "count", String.valueOf(items.size()));
        ReflectionTestUtils.setField(response, "list", items);
        return response;
    }

    public static RestOilItem restOilItem(String standardRestCode, String standardRestName) {
        return restOilItem(standardRestCode, standardRestName, "07");
    }

    public static RestOilItem restOilItem(String standardRestCode, String standardRestName, String convenienceCode) {
        RestOilItem item = instantiate(RestOilItem.class);
        ReflectionTestUtils.setField(item, "standardRestCode", standardRestCode);
        ReflectionTestUtils.setField(item, "standardRestName", standardRestName);
        ReflectionTestUtils.setField(item, "startTime", "00:00");
        ReflectionTestUtils.setField(item, "endTime", "24:00");
        ReflectionTestUtils.setField(item, "originalModifierId", "SYSTEM");
        ReflectionTestUtils.setField(item, "originalModifiedDateTime", "26/06/15");
        ReflectionTestUtils.setField(item, "lastModifiedUser", "SYSTEM");
        ReflectionTestUtils.setField(item, "lastModifiedDateTime", "2026-06-15");
        ReflectionTestUtils.setField(item, "serviceAreaAddress", "서울시 서초구");
        ReflectionTestUtils.setField(item, "routeCode", "0010");
        ReflectionTestUtils.setField(item, "routeName", "경부선");
        ReflectionTestUtils.setField(item, "convenienceCode", convenienceCode);
        ReflectionTestUtils.setField(item, "convenienceName", "쉼터");
        ReflectionTestUtils.setField(item, "convenienceDescription", "고객쉼터");
        return item;
    }

    public static RestOilResponse restOilResponse(String code, List<RestOilItem> items) {
        RestOilResponse response = instantiate(RestOilResponse.class);
        ReflectionTestUtils.setField(response, "code", code);
        ReflectionTestUtils.setField(response, "message", "인증키가 유효합니다.");
        ReflectionTestUtils.setField(response, "count", items.size());
        ReflectionTestUtils.setField(response, "list", items);
        return response;
    }

    public static RestOilPriceItem restOilPriceItem(String serviceAreaCode2, String serviceAreaName) {
        RestOilPriceItem item = instantiate(RestOilPriceItem.class);
        ReflectionTestUtils.setField(item, "routeCode", "0010");
        ReflectionTestUtils.setField(item, "serviceAreaCode", "B00001");
        ReflectionTestUtils.setField(item, "routeName", "경부선");
        ReflectionTestUtils.setField(item, "direction", "부산");
        ReflectionTestUtils.setField(item, "oilCompany", "AD");
        ReflectionTestUtils.setField(item, "lpgYn", "Y");
        ReflectionTestUtils.setField(item, "serviceAreaName", serviceAreaName);
        ReflectionTestUtils.setField(item, "telNo", "02-573-7430");
        ReflectionTestUtils.setField(item, "gasolinePrice", "1,999원");
        ReflectionTestUtils.setField(item, "dieselPrice", "1,997원");
        ReflectionTestUtils.setField(item, "lpgPrice", "1,157원");
        ReflectionTestUtils.setField(item, "numOfRows", null);
        ReflectionTestUtils.setField(item, "pageNo", null);
        ReflectionTestUtils.setField(item, "serviceAreaCode2", serviceAreaCode2);
        ReflectionTestUtils.setField(item, "serviceAreaAddress", "서울시 서초구 원지동10-16");
        return item;
    }

    public static RestOilPriceResponse restOilPriceResponse(String code, List<RestOilPriceItem> items) {
        RestOilPriceResponse response = instantiate(RestOilPriceResponse.class);
        ReflectionTestUtils.setField(response, "code", code);
        ReflectionTestUtils.setField(response, "message", "인증키가 유효합니다.");
        ReflectionTestUtils.setField(response, "count", items.size());
        ReflectionTestUtils.setField(response, "pageNo", 1);
        ReflectionTestUtils.setField(response, "numOfRows", 99);
        ReflectionTestUtils.setField(response, "pageSize", 3);
        ReflectionTestUtils.setField(response, "list", items);
        return response;
    }

    private static <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate test fixture: " + type.getName(), e);
        }
    }
}
