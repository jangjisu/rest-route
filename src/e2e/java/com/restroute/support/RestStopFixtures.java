package com.restroute.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.oilprice.client.response.RestOilItem;
import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.repository.RestOilRepository;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopImageEntity;
import com.restroute.reststop.repository.RestStopImageRepository;
import com.restroute.reststop.repository.RestStopRepository;

/**
 * 휴게소 공개 API 인수 테스트가 쓰는 데이터. {@link RouteFixtures}와 합치지 않는다 — 그쪽은
 * 좌표들 사이의 관계(경로 위/밖)가 곧 의미인 반면 이쪽은 연관 데이터의 유무가 의미라, 한 파일에
 * 두면 어느 상수가 어느 테스트의 전제인지 읽기 어려워진다.
 *
 * <p>휴게소는 {@code createByAdmin}으로 심는다 — 서비스지역코드를 스스로 만들어내므로 저장 후
 * 돌려받아 쓴다. 테스트가 코드를 지어내지 않으니 실제 저장 경로와 어긋날 일이 없다.
 */
public final class RestStopFixtures {

    public static final String ROUTE_NAME = "경부고속도로";
    public static final String ROUTE_NO = "1";

    /** 이미지 바이트는 내용이 중요한 게 아니라 detail/list가 서로 달라야 한다는 것이 계약이다. */
    public static final byte[] DETAIL_IMAGE_BYTES = "detail-image-bytes".getBytes();

    public static final byte[] LIST_IMAGE_BYTES = "list-image".getBytes();

    private RestStopFixtures() {}

    /** @return 저장되며 생성된 서비스지역코드 */
    public static String saveRestStop(
            RestStopRepository repository, String unitName, String longitude, String latitude) {
        return repository
                .save(RestStopEntity.createByAdmin(unitName, ROUTE_NO, ROUTE_NAME, longitude, latitude))
                .getServiceAreaCode();
    }

    /** 좌표가 중요하지 않은 시나리오용. */
    public static String saveRestStop(RestStopRepository repository, String unitName) {
        return saveRestStop(repository, unitName, "127.0000", "37.5000");
    }

    public static void saveImages(RestStopImageRepository repository, String serviceAreaCode) {
        repository.save(RestStopImageEntity.of(serviceAreaCode, DETAIL_IMAGE_BYTES, LIST_IMAGE_BYTES));
    }

    /**
     * 휴게소에 주유소를 연결한다. 유가 조회·갱신은 이 연결이 있어야만 외부까지 나가므로,
     * 연결이 없는 상태와 있는 상태가 갈래로 나뉜다.
     *
     * <p>{@link RestOilItem}은 Jackson 전용 DTO라 공개 생성자가 없어 JSON에서 역직렬화한다 —
     * 비공개 필드에 리플렉션으로 값을 밀어넣는 것보다, 외부 API가 실제로 주는 형태를 그대로 쓰는
     * 편이 이 계층의 취지에 맞는다. 연결 자체는 프로덕션이 여는 {@code applyAdminLink}로 건다.
     *
     * @param standardRestCode 유가 조회 키가 되는 코드(외부 API의 {@code serviceAreaCode2})
     */
    public static void saveLinkedOilStation(
            RestOilRepository repository, String serviceAreaCode, String standardRestCode) {
        RestOilEntity oilStation = RestOilEntity.from(oilItem(standardRestCode));
        oilStation.applyAdminLink(serviceAreaCode);
        repository.save(oilStation);
    }

    private static RestOilItem oilItem(String standardRestCode) {
        try {
            return new ObjectMapper().readValue("""
                            {
                              "stdRestCd": "%s",
                              "stdRestNm": "안성주유소",
                              "routeCd": "0010",
                              "routeNm": "경부선",
                              "svarAddr": "경기도 안성시",
                              "psCode": "1",
                              "psName": "주유소",
                              "psDesc": "주유소"
                            }
                            """.formatted(standardRestCode), RestOilItem.class);
        } catch (Exception e) {
            throw new IllegalStateException("주유소 픽스처를 만들지 못했다", e);
        }
    }
}
