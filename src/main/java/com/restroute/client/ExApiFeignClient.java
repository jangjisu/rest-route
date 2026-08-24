package com.restroute.client;

import com.restroute.oilprice.client.response.RestOilPriceResponse;
import com.restroute.oilprice.client.response.RestOilResponse;
import com.restroute.reststop.client.response.HighwayServiceAreaInfoResponse;
import com.restroute.reststop.client.response.RestStopDetailResponse;
import com.restroute.reststop.client.response.RestStopResponse;
import com.restroute.reststopcontent.client.response.RestBestfoodResponse;
import com.restroute.reststopcontent.client.response.RestEventResponse;
import com.restroute.reststopcontent.client.response.RestThemeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 한국도로공사 공공데이터 Feign Client (data.ex.co.kr)
 */
@FeignClient(name = "ex-api", url = "${ex.api.url}")
public interface ExApiFeignClient {

    String LOCATION_INFO_REST_PATH = "/openapi/locationinfo/locationinfoRest";
    String CONVENIENCE_SERVICE_AREA_PATH = "/openapi/business/conveniServiceArea";
    String HIGHWAY_SERVICE_AREA_INFO_PATH = "/openapi/restinfo/hiwaySvarInfoList";
    String REST_OIL_LIST_PATH = "/openapi/restinfo/restOilList";
    String CUR_STATE_STATION_PATH = "/openapi/business/curStateStation";
    String REST_BESTFOOD_LIST_PATH = "/openapi/restinfo/restBestfoodList";
    String REST_THEME_LIST_PATH = "/openapi/restinfo/restThemeList";
    String REST_EVENT_LIST_PATH = "/openapi/restinfo/restEventList";

    String KEY_PARAMETER = "key";
    String TYPE_PARAMETER = "type";
    String NUM_OF_ROWS_PARAMETER = "numOfRows";
    String PAGE_NO_PARAMETER = "pageNo";
    String SERVICE_AREA_CODE2_PARAMETER = "serviceAreaCode2";

    String REST_STOP_NUM_OF_ROWS = "99";

    @GetMapping(LOCATION_INFO_REST_PATH)
    RestStopResponse getLocationInfoRest(
            @RequestParam(KEY_PARAMETER) String key,
            @RequestParam(TYPE_PARAMETER) String type,
            @RequestParam(value = NUM_OF_ROWS_PARAMETER, required = false) String numOfRows,
            @RequestParam(value = PAGE_NO_PARAMETER, required = false) String pageNo);

    @GetMapping(CONVENIENCE_SERVICE_AREA_PATH)
    RestStopDetailResponse getConvenienceServiceArea(
            @RequestParam(KEY_PARAMETER) String key,
            @RequestParam(TYPE_PARAMETER) String type,
            @RequestParam(value = NUM_OF_ROWS_PARAMETER, required = false) String numOfRows,
            @RequestParam(value = PAGE_NO_PARAMETER, required = false) String pageNo);

    @GetMapping(HIGHWAY_SERVICE_AREA_INFO_PATH)
    HighwayServiceAreaInfoResponse getHighwayServiceAreaInfoList(
            @RequestParam(KEY_PARAMETER) String key, @RequestParam(TYPE_PARAMETER) String type);

    @GetMapping(REST_OIL_LIST_PATH)
    RestOilResponse getRestOilList(@RequestParam(KEY_PARAMETER) String key, @RequestParam(TYPE_PARAMETER) String type);

    @GetMapping(CUR_STATE_STATION_PATH)
    RestOilPriceResponse getCurStateStation(
            @RequestParam(KEY_PARAMETER) String key,
            @RequestParam(TYPE_PARAMETER) String type,
            @RequestParam(value = NUM_OF_ROWS_PARAMETER, required = false) String numOfRows,
            @RequestParam(value = PAGE_NO_PARAMETER, required = false) String pageNo);

    @GetMapping(CUR_STATE_STATION_PATH)
    RestOilPriceResponse getCurStateStation(
            @RequestParam(KEY_PARAMETER) String key,
            @RequestParam(TYPE_PARAMETER) String type,
            @RequestParam(value = NUM_OF_ROWS_PARAMETER, required = false) String numOfRows,
            @RequestParam(value = PAGE_NO_PARAMETER, required = false) String pageNo,
            @RequestParam(value = SERVICE_AREA_CODE2_PARAMETER, required = false) String serviceAreaCode2);

    @GetMapping(REST_BESTFOOD_LIST_PATH)
    RestBestfoodResponse getRestBestfoodList(
            @RequestParam(KEY_PARAMETER) String key,
            @RequestParam(TYPE_PARAMETER) String type,
            @RequestParam(value = NUM_OF_ROWS_PARAMETER, required = false) String numOfRows,
            @RequestParam(value = PAGE_NO_PARAMETER, required = false) String pageNo);

    @GetMapping(REST_THEME_LIST_PATH)
    RestThemeResponse getRestThemeList(
            @RequestParam(KEY_PARAMETER) String key, @RequestParam(TYPE_PARAMETER) String type);

    @GetMapping(REST_EVENT_LIST_PATH)
    RestEventResponse getRestEventList(
            @RequestParam(KEY_PARAMETER) String key, @RequestParam(TYPE_PARAMETER) String type);
}
