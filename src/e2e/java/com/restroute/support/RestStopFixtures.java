package com.restroute.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.oilprice.client.response.RestOilItem;
import com.restroute.oilprice.domain.RestOilEntity;
import com.restroute.oilprice.repository.RestOilRepository;
import com.restroute.reststop.client.response.RestStopItem;
import com.restroute.reststop.domain.RestStopEntity;
import com.restroute.reststop.domain.RestStopImageEntity;
import com.restroute.reststop.repository.RestStopImageRepository;
import com.restroute.reststop.repository.RestStopRepository;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

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

    /**
     * 업로드에 쓸 실제 PNG 바이트를 만든다. 서버가 {@code ImageIO}로 읽어 webp로 변환하므로
     * 아무 바이트나 보내면 400이 되고, 그 400은 업로드 경로를 검증한 것이 아니라 픽스처가
     * 잘못된 것일 뿐이다.
     *
     * @param size 한 변의 픽셀 수. 리사이즈 경로를 태우려면 목록용 폭보다 크게 준다
     */
    public static byte[] pngBytes(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.GRAY);
        graphics.fillRect(0, 0, size, size);
        graphics.dispose();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("PNG 픽스처를 만들지 못했다", e);
        }
    }

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

    /**
     * 외부 API 동기화로 들어온 휴게소를 심는다. {@link #saveRestStop}이 쓰는
     * {@code createByAdmin}은 관리자가 만든 행이라 {@code adminOverridden}이 처음부터 켜져
     * 있으므로, 잠금이 <b>풀린</b> 상태가 전제인 시나리오는 이쪽을 써야 한다.
     *
     * <p>서비스지역코드를 호출부가 정할 수 있다는 것도 차이다 — 관리자 생성은 코드를 스스로
     * 만들어내지만 동기화는 외부가 준 코드를 그대로 쓴다.
     */
    public static void saveSyncedRestStop(RestStopRepository repository, String serviceAreaCode, String unitName) {
        repository.save(RestStopEntity.from(restStopItem(serviceAreaCode, unitName)));
    }

    private static RestStopItem restStopItem(String serviceAreaCode, String unitName) {
        try {
            return new ObjectMapper().readValue("""
                            {
                              "unitCode": "001",
                              "unitName": "%s",
                              "routeNo": "0010",
                              "routeName": "경부선",
                              "xValue": "127.0425",
                              "yValue": "37.4599",
                              "stdRestCd": "000001",
                              "serviceAreaCode": "%s"
                            }
                            """.formatted(unitName, serviceAreaCode), RestStopItem.class);
        } catch (Exception e) {
            throw new IllegalStateException("휴게소 픽스처를 만들지 못했다", e);
        }
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
