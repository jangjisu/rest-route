package com.restroute.reststopcontent.controller;

import com.restroute.common.ApiResponse;
import com.restroute.common.ResponseCode;
import com.restroute.reststopcontent.controller.response.RestStopEventResponse;
import com.restroute.reststopcontent.service.RestStopEventQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rest-stops")
public class RestStopEventController {

    private final RestStopEventQueryService restStopEventQueryService;

    @GetMapping("/{serviceAreaCode}/events")
    public ResponseEntity<ApiResponse<RestStopEventResponse>> getRestStopEvents(@PathVariable String serviceAreaCode) {
        return restStopEventQueryService
                .findByServiceAreaCode(serviceAreaCode)
                .map(events -> ResponseEntity.ok(ApiResponse.success(events)))
                .orElseGet(() -> ResponseEntity.status(ResponseCode.NOT_FOUND.getHttpStatus())
                        .body(ApiResponse.error(ResponseCode.NOT_FOUND)));
    }
}
