package com.restroute.service.admin;

public class RestOilNotFoundException extends RuntimeException {

    private RestOilNotFoundException(String message) {
        super(message);
    }

    public static RestOilNotFoundException forId(Long oilId) {
        return new RestOilNotFoundException("주유소를 찾을 수 없습니다: " + oilId);
    }
}
