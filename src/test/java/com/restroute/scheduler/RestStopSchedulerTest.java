package com.restroute.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.service.HighwayServiceAreaInfoSyncService;
import com.restroute.service.RestEventSyncService;
import com.restroute.service.RestFoodSyncService;
import com.restroute.service.RestOilPriceSyncService;
import com.restroute.service.RestOilSyncService;
import com.restroute.service.RestStopDetailSyncService;
import com.restroute.service.RestStopServiceAreaCodeBackfillService;
import com.restroute.service.RestStopSyncService;
import com.restroute.service.RestThemeSyncService;
import com.restroute.service.evcharger.EvChargerSyncService;
import com.restroute.service.evcharger.dto.EvChargerSyncResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class RestStopSchedulerTest {

    @Mock
    private RestStopSyncService restStopSyncService;

    @Mock
    private RestStopDetailSyncService restStopDetailSyncService;

    @Mock
    private HighwayServiceAreaInfoSyncService highwayServiceAreaInfoSyncService;

    @Mock
    private RestOilSyncService restOilSyncService;

    @Mock
    private RestOilPriceSyncService restOilPriceSyncService;

    @Mock
    private RestFoodSyncService restFoodSyncService;

    @Mock
    private RestThemeSyncService restThemeSyncService;

    @Mock
    private RestEventSyncService restEventSyncService;

    @Mock
    private RestStopServiceAreaCodeBackfillService restStopServiceAreaCodeBackfillService;

    @Mock
    private EvChargerSyncService evChargerSyncService;

    @InjectMocks
    private RestStopScheduler restStopScheduler;

    @Test
    @DisplayName("매일 휴게소 위치와 상세, 고속도로 휴게소 정보, 주유소 편의시설 동기화를 service에 위임한다")
    void syncRestStopsDaily_delegatesToService() {
        when(restStopSyncService.refreshRestStops()).thenReturn(203);
        when(restStopDetailSyncService.refreshRestStopDetails()).thenReturn(215);
        when(highwayServiceAreaInfoSyncService.refreshHighwayServiceAreaInfos()).thenReturn(581);
        when(restOilSyncService.refreshRestOils()).thenReturn(429);
        when(restFoodSyncService.refreshRestFoods()).thenReturn(7214);
        when(restThemeSyncService.refreshRestThemes()).thenReturn(144);
        when(restEventSyncService.refreshRestEvents()).thenReturn(898);
        when(evChargerSyncService.refreshEvChargers()).thenReturn(successfulEvChargerSync());

        restStopScheduler.syncRestStopsDaily();

        verify(restStopSyncService).refreshRestStops();
        verify(restStopDetailSyncService).refreshRestStopDetails();
        verify(highwayServiceAreaInfoSyncService).refreshHighwayServiceAreaInfos();
        verify(restOilSyncService).refreshRestOils();
        verify(restFoodSyncService).refreshRestFoods();
        verify(restThemeSyncService).refreshRestThemes();
        verify(restEventSyncService).refreshRestEvents();
        verify(evChargerSyncService).refreshEvChargers();
        InOrder inOrder = inOrder(
                restStopSyncService,
                restStopDetailSyncService,
                highwayServiceAreaInfoSyncService,
                restOilSyncService,
                restFoodSyncService,
                restThemeSyncService,
                restEventSyncService,
                restStopServiceAreaCodeBackfillService,
                evChargerSyncService);
        inOrder.verify(restStopSyncService).refreshRestStops();
        inOrder.verify(restStopDetailSyncService).refreshRestStopDetails();
        inOrder.verify(highwayServiceAreaInfoSyncService).refreshHighwayServiceAreaInfos();
        inOrder.verify(restOilSyncService).refreshRestOils();
        inOrder.verify(restFoodSyncService).refreshRestFoods();
        inOrder.verify(restThemeSyncService).refreshRestThemes();
        inOrder.verify(restEventSyncService).refreshRestEvents();
        inOrder.verify(evChargerSyncService).refreshEvChargers();
        inOrder.verify(restStopServiceAreaCodeBackfillService).backfill();
    }

    @Test
    @DisplayName("테마휴게소 동기화 실패를 로그로 기록하고 전파하지 않는다")
    void syncRestStopsDaily_doesNotPropagateRestThemeFailure(CapturedOutput output) {
        when(restThemeSyncService.refreshRestThemes()).thenThrow(new IllegalStateException("rest theme API failed"));

        assertThatCode(restStopScheduler::syncRestStopsDaily).doesNotThrowAnyException();

        assertThat(output).contains("Scheduled rest theme sync failed.").contains("rest theme API failed");
        verify(restEventSyncService).refreshRestEvents();
        verify(restStopServiceAreaCodeBackfillService).backfill();
    }

    @Test
    @DisplayName("휴게소 이벤트 동기화 실패를 로그로 기록하고 전파하지 않는다")
    void syncRestStopsDaily_doesNotPropagateRestEventFailure(CapturedOutput output) {
        when(restEventSyncService.refreshRestEvents()).thenThrow(new IllegalStateException("rest event API failed"));

        assertThatCode(restStopScheduler::syncRestStopsDaily).doesNotThrowAnyException();

        assertThat(output).contains("Scheduled rest event sync failed.").contains("rest event API failed");
        verify(restStopServiceAreaCodeBackfillService).backfill();
    }

    @Test
    @DisplayName("EV 동기화가 완전히 실패해도 backfill을 실행한다")
    void syncRestStopsDaily_runsBackfillWhenEvSyncFails() {
        when(evChargerSyncService.refreshEvChargers()).thenReturn(new EvChargerSyncResult(7, 0, 1, 0, 0));

        restStopScheduler.syncRestStopsDaily();

        verify(restStopServiceAreaCodeBackfillService).backfill();
    }

    @Test
    @DisplayName("EV 동기화 예외를 기록하고 EV 매핑을 보존한다")
    void syncRestStopsDaily_preservesEvMappingsWhenEvSyncThrows(CapturedOutput output) {
        when(evChargerSyncService.refreshEvChargers()).thenThrow(new IllegalStateException("EV API failed"));

        assertThatCode(restStopScheduler::syncRestStopsDaily).doesNotThrowAnyException();

        verify(restStopServiceAreaCodeBackfillService).backfill();
        assertThat(output).contains("Scheduled EV charger sync failed.").contains("EV API failed");
    }

    @Test
    @DisplayName("EV 매핑 backfill 실패를 기록하고 스케줄러를 중단하지 않는다")
    void syncRestStopsDaily_doesNotPropagateEvMappingBackfillFailure(CapturedOutput output) {
        when(evChargerSyncService.refreshEvChargers()).thenReturn(new EvChargerSyncResult(7, 0, 1, 0, 0));
        when(restStopServiceAreaCodeBackfillService.backfill())
                .thenThrow(new IllegalStateException("EV mapping backfill failed"));

        assertThatCode(restStopScheduler::syncRestStopsDaily).doesNotThrowAnyException();

        assertThat(output)
                .contains("Scheduled rest stop service area code backfill failed.")
                .contains("EV mapping backfill failed");
    }

    private EvChargerSyncResult successfulEvChargerSync() {
        return new EvChargerSyncResult(13, 13, 0, 2401, 1000);
    }

    @Test
    @DisplayName("휴게소 상세 동기화가 실패해도 고속도로 휴게소 정보 동기화를 계속한다")
    void syncRestStopsDaily_continuesServiceAreaInfoSyncWhenDetailSyncFails(CapturedOutput output) {
        when(restStopSyncService.refreshRestStops()).thenReturn(203);
        when(restStopDetailSyncService.refreshRestStopDetails())
                .thenThrow(new IllegalStateException("detail page 2 failed"));
        when(highwayServiceAreaInfoSyncService.refreshHighwayServiceAreaInfos()).thenReturn(581);

        assertThatCode(restStopScheduler::syncRestStopsDaily).doesNotThrowAnyException();

        verify(highwayServiceAreaInfoSyncService).refreshHighwayServiceAreaInfos();
        verify(restOilSyncService).refreshRestOils();
        assertThat(output)
                .contains("Scheduled rest stop detail sync failed.")
                .contains("detail page 2 failed")
                .contains("Scheduled highway service area info sync completed. savedCount=581");
    }

    @Test
    @DisplayName("휴게소 위치 동기화가 실패해도 나머지 동기화를 계속한다")
    void syncRestStopsDaily_continuesRemainingSyncsWhenRestStopSyncFails(CapturedOutput output) {
        when(restStopSyncService.refreshRestStops()).thenThrow(new IllegalStateException("location API failed"));
        when(restStopDetailSyncService.refreshRestStopDetails()).thenReturn(215);
        when(highwayServiceAreaInfoSyncService.refreshHighwayServiceAreaInfos()).thenReturn(581);

        assertThatCode(restStopScheduler::syncRestStopsDaily).doesNotThrowAnyException();

        verify(restStopDetailSyncService).refreshRestStopDetails();
        verify(highwayServiceAreaInfoSyncService).refreshHighwayServiceAreaInfos();
        verify(restOilSyncService).refreshRestOils();
        assertThat(output)
                .contains("Scheduled rest stop sync failed.")
                .contains("location API failed")
                .contains("Scheduled rest stop detail sync completed. savedCount=215");
    }

    @Test
    @DisplayName("고속도로 휴게소 정보 동기화 실패를 로그로 기록하고 전파하지 않는다")
    void syncRestStopsDaily_doesNotPropagateServiceAreaInfoFailure(CapturedOutput output) {
        when(restStopSyncService.refreshRestStops()).thenReturn(203);
        when(restStopDetailSyncService.refreshRestStopDetails()).thenReturn(215);
        when(highwayServiceAreaInfoSyncService.refreshHighwayServiceAreaInfos())
                .thenThrow(new IllegalStateException("service area info API failed"));

        assertThatCode(restStopScheduler::syncRestStopsDaily).doesNotThrowAnyException();

        assertThat(output)
                .contains("Scheduled highway service area info sync failed.")
                .contains("service area info API failed");
        verify(restOilSyncService).refreshRestOils();
    }

    @Test
    @DisplayName("주유소 편의시설 동기화 실패를 로그로 기록하고 전파하지 않는다")
    void syncRestStopsDaily_doesNotPropagateRestOilFailure(CapturedOutput output) {
        when(restStopSyncService.refreshRestStops()).thenReturn(203);
        when(restStopDetailSyncService.refreshRestStopDetails()).thenReturn(215);
        when(highwayServiceAreaInfoSyncService.refreshHighwayServiceAreaInfos()).thenReturn(581);
        when(restOilSyncService.refreshRestOils()).thenThrow(new IllegalStateException("rest oil API failed"));

        assertThatCode(restStopScheduler::syncRestStopsDaily).doesNotThrowAnyException();

        assertThat(output).contains("Scheduled rest oil sync failed.").contains("rest oil API failed");
    }

    @Test
    @DisplayName("음식 메뉴 동기화 실패를 로그로 기록하고 전파하지 않는다")
    void syncRestStopsDaily_doesNotPropagateRestFoodFailure(CapturedOutput output) {
        when(restStopSyncService.refreshRestStops()).thenReturn(203);
        when(restStopDetailSyncService.refreshRestStopDetails()).thenReturn(215);
        when(highwayServiceAreaInfoSyncService.refreshHighwayServiceAreaInfos()).thenReturn(581);
        when(restOilSyncService.refreshRestOils()).thenReturn(429);
        when(restFoodSyncService.refreshRestFoods()).thenThrow(new IllegalStateException("rest food API failed"));

        assertThatCode(restStopScheduler::syncRestStopsDaily).doesNotThrowAnyException();

        assertThat(output).contains("Scheduled rest food sync failed.").contains("rest food API failed");
        verify(restStopServiceAreaCodeBackfillService).backfill();
    }

    @Test
    @DisplayName("3시간마다 주유소 가격 동기화 후 조회 키 backfill을 실행한다")
    void syncRestOilPricesEveryThreeHours_delegatesToService() {
        when(restOilPriceSyncService.refreshRestOilPrices()).thenReturn(226);

        restStopScheduler.syncRestOilPricesEveryThreeHours();

        InOrder inOrder = inOrder(restOilPriceSyncService, restStopServiceAreaCodeBackfillService);
        inOrder.verify(restOilPriceSyncService).refreshRestOilPrices();
        inOrder.verify(restStopServiceAreaCodeBackfillService).backfill();
    }

    @Test
    @DisplayName("주유소 가격 동기화 실패를 로그로 기록하고 전파하지 않는다")
    void syncRestOilPricesEveryThreeHours_doesNotPropagateFailure(CapturedOutput output) {
        when(restOilPriceSyncService.refreshRestOilPrices())
                .thenThrow(new IllegalStateException("oil price API failed"));

        assertThatCode(restStopScheduler::syncRestOilPricesEveryThreeHours).doesNotThrowAnyException();

        assertThat(output).contains("Scheduled rest oil price sync failed.").contains("oil price API failed");
        verify(restStopServiceAreaCodeBackfillService).backfill();
    }

    @Test
    @DisplayName("조회 키 backfill 실패를 로그로 기록하고 전파하지 않는다")
    void syncRestStopsDaily_doesNotPropagateBackfillFailure(CapturedOutput output) {
        when(restStopServiceAreaCodeBackfillService.backfill()).thenThrow(new IllegalStateException("backfill failed"));

        assertThatCode(restStopScheduler::syncRestStopsDaily).doesNotThrowAnyException();

        assertThat(output)
                .contains("Scheduled rest stop service area code backfill failed.")
                .contains("backfill failed");
    }
}
