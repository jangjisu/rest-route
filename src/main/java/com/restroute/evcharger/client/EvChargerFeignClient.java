package com.restroute.evcharger.client;

import com.restroute.evcharger.client.response.EvChargerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ev-charger-api", url = "${ev.api.url}")
public interface EvChargerFeignClient {

    String CHARGER_INFO_PATH = "/getChargerInfo";
    String SERVICE_KEY_PARAMETER = "serviceKey";
    String PAGE_NO_PARAMETER = "pageNo";
    String NUM_OF_ROWS_PARAMETER = "numOfRows";
    String DATA_TYPE_PARAMETER = "dataType";
    String KIND_PARAMETER = "kind";
    String JSON_DATA_TYPE = "JSON";
    String REST_FACILITY_KIND = "C0";
    String HIGHWAY_REST_STOP_KIND_DETAIL = "C001";
    int CHARGER_NUM_OF_ROWS = 200;

    @GetMapping(CHARGER_INFO_PATH)
    EvChargerResponse getChargerInfo(
            @RequestParam(SERVICE_KEY_PARAMETER) String serviceKey,
            @RequestParam(PAGE_NO_PARAMETER) int pageNo,
            @RequestParam(NUM_OF_ROWS_PARAMETER) int numOfRows,
            @RequestParam(DATA_TYPE_PARAMETER) String dataType,
            @RequestParam(KIND_PARAMETER) String kind);
}
