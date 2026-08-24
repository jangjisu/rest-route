package com.restroute.evcharger.client.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.restroute.client.response.ExApiResponse;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvChargerResponse implements ExApiResponse {

    private String resultCode;
    private String resultMsg;
    private int totalCount;
    private int pageNo;
    private int numOfRows;
    private Items items;

    public List<EvChargerItem> getList() {
        if (items == null || items.item == null) {
            return List.of();
        }
        return items.item;
    }

    public int getTotalPageCount() {
        if (numOfRows <= 0 || totalCount <= 0) {
            return 1;
        }
        return (totalCount + numOfRows - 1) / numOfRows;
    }

    @Override
    public boolean isSuccess() {
        return "00".equals(resultCode);
    }

    @Override
    public String getErrorMessage() {
        return resultMsg;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    private static class Items {

        private List<EvChargerItem> item;
    }
}
