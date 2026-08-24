package com.restroute.reststop.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.restroute.common.client.response.ExApiResponse;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HighwayServiceAreaInfoResponse implements ExApiResponse {

    private String code;
    private String message;
    private String count;
    private List<HighwayServiceAreaInfoItem> list;

    @Override
    public boolean isSuccess() {
        return "SUCCESS".equals(code);
    }

    @Override
    public String getErrorMessage() {
        return message;
    }
}
