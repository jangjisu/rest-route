package com.restroute.reststop.client.response;

import com.restroute.common.client.response.ExApiResponse;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestStopResponse implements ExApiResponse {

    private String code;
    private String message;
    private String count;
    private String pageSize;
    private List<RestStopItem> list;

    @Override
    public boolean isSuccess() {
        return "SUCCESS".equals(code);
    }

    @Override
    public String getErrorMessage() {
        return message;
    }

    public int getTotalPageCount() {
        try {
            return Integer.parseInt(pageSize);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
