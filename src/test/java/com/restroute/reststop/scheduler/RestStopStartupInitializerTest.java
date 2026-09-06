package com.restroute.reststop.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.evcharger.service.EvChargerSyncService;
import com.restroute.evcharger.service.dto.EvChargerSyncResult;
import com.restroute.oilprice.service.RestOilPriceSyncService;
import com.restroute.oilprice.service.RestOilSyncService;
import com.restroute.reststop.service.RestStopDetailSyncService;
import com.restroute.reststop.service.RestStopServiceAreaCodeBackfillService;
import com.restroute.reststop.service.RestStopSyncService;
import com.restroute.reststopcontent.service.RestEventSyncService;
import com.restroute.reststopcontent.service.RestFoodSyncService;
import com.restroute.reststopcontent.service.RestThemeSyncService;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class RestStopStartupInitializerTest {

    @Mock
    private RestStopSyncService restStopSyncService;

    @Mock
    private RestStopDetailSyncService restStopDetailSyncService;

    @Mock
    private RestOilSyncService restOilSyncService;

    @Mock
    private RestOilPriceSyncService restOilPriceSyncService;

    @Mock
    private RestFoodSyncService restFoodSyncService;

    @Mock
    private RestStopServiceAreaCodeBackfillService restStopServiceAreaCodeBackfillService;

    @Mock
    private EvChargerSyncService evChargerSyncService;

    @Mock
    private RestThemeSyncService restThemeSyncService;

    @Mock
    private RestEventSyncService restEventSyncService;

    @Mock
    private ApplicationArguments applicationArguments;

    @InjectMocks
    private RestStopStartupInitializer restStopStartupInitializer;

    @Test
    @DisplayName("서버 시작 시 휴게소 위치와 상세, 주유소 편의시설 초기 적재를 service에 위임한다")
    void run_delegatesInitialSyncToService() {
        when(restStopSyncService.initializeRestStopsIfEmpty()).thenReturn(203);
        when(restStopDetailSyncService.initializeRestStopDetailsIfEmpty()).thenReturn(215);
        when(restOilSyncService.initializeRestOilsIfEmpty()).thenReturn(429);
        when(restOilPriceSyncService.initializeRestOilPricesIfEmpty()).thenReturn(226);
        when(restFoodSyncService.initializeRestFoodsIfEmpty()).thenReturn(7214);
        when(evChargerSyncService.initializeEvChargersIfEmpty()).thenReturn(successfulEvChargerSync());
        when(restThemeSyncService.initializeRestThemesIfEmpty()).thenReturn(144);
        when(restEventSyncService.initializeRestEventsIfEmpty()).thenReturn(898);
        when(restStopServiceAreaCodeBackfillService.backfill())
                .thenReturn(Map.of(
                        RestStopServiceAreaCodeBackfillService.REST_STOP_DETAIL_MAPPED_COUNT,
                        203,
                        RestStopServiceAreaCodeBackfillService.HIGHWAY_SERVICE_AREA_INFO_MAPPED_COUNT,
                        203,
                        RestStopServiceAreaCodeBackfillService.REST_FOOD_MAPPED_COUNT,
                        7214,
                        RestStopServiceAreaCodeBackfillService.REST_OIL_MAPPED_COUNT,
                        317,
                        RestStopServiceAreaCodeBackfillService.REST_OIL_PRICE_MAPPED_COUNT,
                        164));

        restStopStartupInitializer.run(applicationArguments);

        verify(restStopSyncService).initializeRestStopsIfEmpty();
        verify(restStopDetailSyncService).initializeRestStopDetailsIfEmpty();
        verify(restOilSyncService).initializeRestOilsIfEmpty();
        verify(restOilPriceSyncService).initializeRestOilPricesIfEmpty();
        verify(restFoodSyncService).initializeRestFoodsIfEmpty();
        verify(evChargerSyncService).initializeEvChargersIfEmpty();
        verify(restThemeSyncService).initializeRestThemesIfEmpty();
        verify(restEventSyncService).initializeRestEventsIfEmpty();
        verify(restStopServiceAreaCodeBackfillService).backfill();
        InOrder inOrder = inOrder(
                restStopSyncService,
                restStopDetailSyncService,
                restOilSyncService,
                restOilPriceSyncService,
                restFoodSyncService,
                restThemeSyncService,
                restEventSyncService,
                restStopServiceAreaCodeBackfillService,
                evChargerSyncService);
        inOrder.verify(restStopSyncService).initializeRestStopsIfEmpty();
        inOrder.verify(restStopDetailSyncService).initializeRestStopDetailsIfEmpty();
        inOrder.verify(restOilSyncService).initializeRestOilsIfEmpty();
        inOrder.verify(restOilPriceSyncService).initializeRestOilPricesIfEmpty();
        inOrder.verify(restFoodSyncService).initializeRestFoodsIfEmpty();
        inOrder.verify(evChargerSyncService).initializeEvChargersIfEmpty();
        inOrder.verify(restThemeSyncService).initializeRestThemesIfEmpty();
        inOrder.verify(restEventSyncService).initializeRestEventsIfEmpty();
        inOrder.verify(restStopServiceAreaCodeBackfillService).backfill();
    }

    private EvChargerSyncResult successfulEvChargerSync() {
        return new EvChargerSyncResult(13, 13, 0, 2401, 1000);
    }

    @Test
    @DisplayName("휴게소 위치 초기 동기화가 실패해도 상세 동기화를 계속한다")
    void run_continuesDetailSyncWhenRestStopSyncFails(CapturedOutput output) {
        when(restStopSyncService.initializeRestStopsIfEmpty())
                .thenThrow(new IllegalStateException("location API failed"));
        when(restStopDetailSyncService.initializeRestStopDetailsIfEmpty()).thenReturn(215);

        assertThatCode(() -> restStopStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        verify(restStopDetailSyncService).initializeRestStopDetailsIfEmpty();
        verify(restOilSyncService).initializeRestOilsIfEmpty();
        verify(restOilPriceSyncService).initializeRestOilPricesIfEmpty();
        assertThat(output)
                .contains("Initial rest stop sync failed.")
                .contains("location API failed")
                .contains("Initial rest stop detail sync completed. savedCount=215");
    }

    @Test
    @DisplayName("휴게소 상세 초기 동기화 실패가 앱 시작으로 전파되지 않는다")
    void run_doesNotPropagateDetailSyncFailure(CapturedOutput output) {
        when(restStopSyncService.initializeRestStopsIfEmpty()).thenReturn(203);
        when(restStopDetailSyncService.initializeRestStopDetailsIfEmpty())
                .thenThrow(new IllegalStateException("detail API failed"));

        assertThatCode(() -> restStopStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        assertThat(output).contains("Initial rest stop detail sync failed.").contains("detail API failed");
        verify(restOilSyncService).initializeRestOilsIfEmpty();
        verify(restOilPriceSyncService).initializeRestOilPricesIfEmpty();
    }

    @Test
    @DisplayName("주유소 편의시설 초기 동기화 실패가 앱 시작으로 전파되지 않는다")
    void run_doesNotPropagateRestOilSyncFailure(CapturedOutput output) {
        when(restStopSyncService.initializeRestStopsIfEmpty()).thenReturn(203);
        when(restStopDetailSyncService.initializeRestStopDetailsIfEmpty()).thenReturn(215);
        when(restOilSyncService.initializeRestOilsIfEmpty())
                .thenThrow(new IllegalStateException("rest oil API failed"));

        assertThatCode(() -> restStopStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        assertThat(output).contains("Initial rest oil sync failed.").contains("rest oil API failed");
        verify(restOilPriceSyncService).initializeRestOilPricesIfEmpty();
    }

    @Test
    @DisplayName("주유소 가격 초기 동기화 실패가 앱 시작으로 전파되지 않는다")
    void run_doesNotPropagateRestOilPriceSyncFailure(CapturedOutput output) {
        when(restStopSyncService.initializeRestStopsIfEmpty()).thenReturn(203);
        when(restStopDetailSyncService.initializeRestStopDetailsIfEmpty()).thenReturn(215);
        when(restOilSyncService.initializeRestOilsIfEmpty()).thenReturn(429);
        when(restOilPriceSyncService.initializeRestOilPricesIfEmpty())
                .thenThrow(new IllegalStateException("rest oil price API failed"));

        assertThatCode(() -> restStopStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        assertThat(output).contains("Initial rest oil price sync failed.").contains("rest oil price API failed");
    }

    @Test
    @DisplayName("초기 적재할 데이터가 있으면 모든 동기화를 생략했다고 기록한다")
    void run_logsSkippedSyncs(CapturedOutput output) {
        when(restStopSyncService.initializeRestStopsIfEmpty()).thenReturn(0);
        when(restStopDetailSyncService.initializeRestStopDetailsIfEmpty()).thenReturn(0);
        when(restOilSyncService.initializeRestOilsIfEmpty()).thenReturn(0);
        when(restOilPriceSyncService.initializeRestOilPricesIfEmpty()).thenReturn(0);
        when(restFoodSyncService.initializeRestFoodsIfEmpty()).thenReturn(0);
        when(evChargerSyncService.initializeEvChargersIfEmpty()).thenReturn(new EvChargerSyncResult(0, 0, 0, 0, 0));
        when(restThemeSyncService.initializeRestThemesIfEmpty()).thenReturn(0);
        when(restEventSyncService.initializeRestEventsIfEmpty()).thenReturn(0);

        restStopStartupInitializer.run(applicationArguments);

        assertThat(output)
                .contains("Initial rest stop sync skipped because rest_stop table already has data.")
                .contains("Initial rest stop detail sync skipped because rest_stop_detail table already has data.")
                .contains("Initial rest oil sync skipped because rest_oil table already has data.")
                .contains("Initial rest oil price sync skipped because rest_oil_price table already has data.")
                .contains("Initial rest food sync skipped because rest_food table already has data.")
                .contains("Initial rest theme sync skipped because rest_theme table already has data.")
                .contains("Initial rest event sync skipped because rest_event table already has data.");
    }

    @Test
    @DisplayName("EV 초기 동기화가 부분 실패해도 backfill을 실행한다")
    void run_runsBackfillWhenEvSyncPartiallyFails() {
        when(evChargerSyncService.initializeEvChargersIfEmpty()).thenReturn(new EvChargerSyncResult(7, 0, 1, 0, 0));

        restStopStartupInitializer.run(applicationArguments);

        verify(restStopServiceAreaCodeBackfillService).backfill();
    }

    @Test
    @DisplayName("EV 초기 동기화 예외를 기록하고 매핑을 보존한다")
    void run_preservesEvMappingsWhenEvSyncThrows(CapturedOutput output) {
        when(evChargerSyncService.initializeEvChargersIfEmpty()).thenThrow(new IllegalStateException("EV API failed"));

        assertThatCode(() -> restStopStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        verify(restStopServiceAreaCodeBackfillService).backfill();
        assertThat(output).contains("Initial EV charger sync failed.").contains("EV API failed");
    }

    @Test
    @DisplayName("EV 초기 동기화 결과가 없으면 기존 backfill 흐름을 사용한다")
    void run_usesDefaultBackfillWhenEvSyncResultIsMissing() {
        when(evChargerSyncService.initializeEvChargersIfEmpty()).thenReturn(null);

        restStopStartupInitializer.run(applicationArguments);

        verify(restStopServiceAreaCodeBackfillService).backfill();
    }

    @Test
    @DisplayName("음식 메뉴 초기 동기화 실패가 앱 시작으로 전파되지 않는다")
    void run_doesNotPropagateRestFoodSyncFailure(CapturedOutput output) {
        when(restStopSyncService.initializeRestStopsIfEmpty()).thenReturn(203);
        when(restStopDetailSyncService.initializeRestStopDetailsIfEmpty()).thenReturn(215);
        when(restOilSyncService.initializeRestOilsIfEmpty()).thenReturn(429);
        when(restOilPriceSyncService.initializeRestOilPricesIfEmpty()).thenReturn(226);
        when(restFoodSyncService.initializeRestFoodsIfEmpty())
                .thenThrow(new IllegalStateException("rest food API failed"));

        assertThatCode(() -> restStopStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        assertThat(output).contains("Initial rest food sync failed.").contains("rest food API failed");
    }

    @Test
    @DisplayName("테마휴게소 초기 동기화 실패가 앱 시작으로 전파되지 않는다")
    void run_doesNotPropagateRestThemeSyncFailure(CapturedOutput output) {
        when(restThemeSyncService.initializeRestThemesIfEmpty())
                .thenThrow(new IllegalStateException("rest theme API failed"));

        assertThatCode(() -> restStopStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        assertThat(output).contains("Initial rest theme sync failed.").contains("rest theme API failed");
        verify(restEventSyncService).initializeRestEventsIfEmpty();
        verify(restStopServiceAreaCodeBackfillService).backfill();
    }

    @Test
    @DisplayName("휴게소 이벤트 초기 동기화 실패가 앱 시작으로 전파되지 않는다")
    void run_doesNotPropagateRestEventSyncFailure(CapturedOutput output) {
        when(restEventSyncService.initializeRestEventsIfEmpty())
                .thenThrow(new IllegalStateException("rest event API failed"));

        assertThatCode(() -> restStopStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        assertThat(output).contains("Initial rest event sync failed.").contains("rest event API failed");
        verify(restStopServiceAreaCodeBackfillService).backfill();
    }

    @Test
    @DisplayName("조회 키 backfill 실패가 앱 시작으로 전파되지 않는다")
    void run_doesNotPropagateRestStopServiceAreaCodeBackfillFailure(CapturedOutput output) {
        when(restStopSyncService.initializeRestStopsIfEmpty()).thenReturn(203);
        when(restStopDetailSyncService.initializeRestStopDetailsIfEmpty()).thenReturn(215);
        when(restOilSyncService.initializeRestOilsIfEmpty()).thenReturn(429);
        when(restOilPriceSyncService.initializeRestOilPricesIfEmpty()).thenReturn(226);
        when(restFoodSyncService.initializeRestFoodsIfEmpty()).thenReturn(7214);
        when(restStopServiceAreaCodeBackfillService.backfill()).thenThrow(new IllegalStateException("backfill failed"));

        assertThatCode(() -> restStopStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        assertThat(output)
                .contains("Rest stop service area code backfill failed.")
                .contains("backfill failed");
    }

    @Test
    @DisplayName("EV 매핑 backfill 실패를 기록하고 앱 시작으로 전파하지 않는다")
    void run_doesNotPropagateEvMappingBackfillFailure(CapturedOutput output) {
        when(evChargerSyncService.initializeEvChargersIfEmpty()).thenReturn(new EvChargerSyncResult(7, 0, 1, 0, 0));
        when(restStopServiceAreaCodeBackfillService.backfill())
                .thenThrow(new IllegalStateException("EV mapping backfill failed"));

        assertThatCode(() -> restStopStartupInitializer.run(applicationArguments))
                .doesNotThrowAnyException();

        assertThat(output)
                .contains("Rest stop service area code backfill failed.")
                .contains("EV mapping backfill failed");
    }
}
