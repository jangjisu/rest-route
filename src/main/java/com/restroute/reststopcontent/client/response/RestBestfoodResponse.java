package com.restroute.reststopcontent.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.restroute.client.response.ExApiResponse;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestBestfoodResponse implements ExApiResponse {

    private String code;
    private String message;
    private int count;
    private int pageNo;
    private int numOfRows;
    private int pageSize;
    private List<RestBestfoodItem> list;

    @Override
    public boolean isSuccess() {
        return "SUCCESS".equals(code);
    }

    @Override
    public String getErrorMessage() {
        return message;
    }
}
