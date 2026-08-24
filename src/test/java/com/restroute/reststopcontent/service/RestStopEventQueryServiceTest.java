package com.restroute.reststopcontent.service;

import static com.restroute.support.RestStopTestFixtures.restEventItem;
import static com.restroute.support.RestStopTestFixtures.restStopItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.restroute.domain.RestStopEntity;
import com.restroute.repository.RestStopRepository;
import com.restroute.reststopcontent.controller.response.RestStopEventResponse;
import com.restroute.reststopcontent.domain.RestEventEntity;
import com.restroute.reststopcontent.repository.RestEventRepository;
import com.restroute.service.RestStopRelatedInfoQueryService;
import com.restroute.service.dto.RestStopRelatedInfo;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RestStopEventQueryServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-28T01:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private RestStopRepository restStopRepository;

    @Mock
    private RestStopRelatedInfoQueryService restStopRelatedInfoQueryService;

    @Mock
    private RestEventRepository restEventRepository;

    private RestStopEventQueryService restStopEventQueryService;

    @BeforeEach
    void setUp() {
        restStopEventQueryService = new RestStopEventQueryService(
                restStopRepository, restStopRelatedInfoQueryService, restEventRepository, CLOCK);
    }

    @Test
    @DisplayName("오늘 날짜가 기간 안에 있는 이벤트만 반환한다")
    void findByServiceAreaCode_returnsOnlyEventsActiveToday() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestEventEntity ongoing = eventWithPeriod("1", "2026-01-01", "2026-12-31");
        RestEventEntity ended = eventWithPeriod("2", "2020-01-01", "2021-01-01");
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        List.of(ongoing, ended)));

        Optional<RestStopEventResponse> result = restStopEventQueryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().events()).extracting("name").containsExactly("TEN+1 이벤트");
    }

    @Test
    @DisplayName("시작일과 종료일 당일도 진행 중으로 취급한다")
    void findByServiceAreaCode_treatsBoundaryDatesAsActive() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestEventEntity startsToday = eventWithPeriod("1", "2026-07-28", "2026-12-31");
        RestEventEntity endsToday = eventWithPeriod("2", "2026-01-01", "2026-07-28");
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        List.of(startsToday, endsToday)));

        Optional<RestStopEventResponse> result = restStopEventQueryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().events()).hasSize(2);
    }

    @Test
    @DisplayName("아직 시작하지 않은 이벤트는 제외한다")
    void findByServiceAreaCode_excludesEventsThatHaveNotStartedYet() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestEventEntity notStartedYet = eventWithPeriod("1", "2026-08-01", "2026-12-31");
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        List.of(notStartedYet)));

        Optional<RestStopEventResponse> result = restStopEventQueryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().events()).isEmpty();
    }

    @Test
    @DisplayName("휴게소가 없으면 이벤트 정보가 없다")
    void findByServiceAreaCode_returnsEmptyWhenRestStopMissing() {
        when(restStopRepository.findByServiceAreaCode("UNKNOWN")).thenReturn(Optional.empty());

        Optional<RestStopEventResponse> result = restStopEventQueryService.findByServiceAreaCode("UNKNOWN");

        assertThat(result).isEmpty();
        verify(restStopRelatedInfoQueryService, never()).findByRestStop(ArgumentMatchers.any());
    }

    @Test
    @DisplayName("기간 형식이 잘못된 이벤트는 건너뛴다")
    void findByServiceAreaCode_skipsEventsWithUnparsableDates() {
        RestStopEntity restStop = RestStopEntity.from(restStopItem("001", "서울만남(부산)휴게소"));
        RestEventEntity malformed = eventWithPeriod("1", "not-a-date", "2026-12-31");
        when(restStopRepository.findByServiceAreaCode("A00001")).thenReturn(Optional.of(restStop));
        when(restStopRelatedInfoQueryService.findByRestStop(restStop))
                .thenReturn(RestStopRelatedInfo.of(
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        List.of(malformed)));

        Optional<RestStopEventResponse> result = restStopEventQueryService.findByServiceAreaCode("A00001");

        assertThat(result).isPresent();
        assertThat(result.get().events()).isEmpty();
    }

    @Test
    @DisplayName("오늘 진행 중인 이벤트가 있는 휴게소 코드를 일괄 조회한다")
    void findActiveEventMappedServiceAreaCodes_returnsOnlyCodesWithActiveEvents() {
        RestEventEntity ongoing = eventWithPeriod("1", "2026-01-01", "2026-12-31");
        ongoing.updateRestStopServiceAreaCode("A00001");
        RestEventEntity ended = eventWithPeriod("2", "2020-01-01", "2021-01-01");
        ended.updateRestStopServiceAreaCode("A00002");
        when(restEventRepository.findAllByRestStopServiceAreaCodeIn(List.of("A00001", "A00002")))
                .thenReturn(List.of(ongoing, ended));

        List<String> result =
                restStopEventQueryService.findActiveEventMappedServiceAreaCodes(List.of("A00001", "A00002"));

        assertThat(result).containsExactly("A00001");
    }

    @Test
    @DisplayName("한 휴게소에 진행 중인 이벤트가 여러 개여도 휴게소 코드는 중복하지 않는다")
    void findActiveEventMappedServiceAreaCodes_removesDuplicateRestStops() {
        RestEventEntity first = eventWithPeriod("1", "2026-01-01", "2026-12-31");
        first.updateRestStopServiceAreaCode("A00001");
        RestEventEntity second = eventWithPeriod("2", "2026-01-01", "2026-12-31");
        second.updateRestStopServiceAreaCode("A00001");
        when(restEventRepository.findAllByRestStopServiceAreaCodeIn(List.of("A00001")))
                .thenReturn(List.of(first, second));

        assertThat(restStopEventQueryService.findActiveEventMappedServiceAreaCodes(List.of("A00001")))
                .containsExactly("A00001");
    }

    @Test
    @DisplayName("휴게소 코드가 없으면 진행 중인 이벤트를 조회하지 않는다")
    void findActiveEventMappedServiceAreaCodes_returnsEmptyForBlankInput() {
        assertThat(restStopEventQueryService.findActiveEventMappedServiceAreaCodes(List.of("", " ")))
                .isEmpty();
    }

    private RestEventEntity eventWithPeriod(String eventSeq, String stime, String etime) {
        RestEventEntity event = RestEventEntity.from(restEventItem("000001", eventSeq));
        ReflectionTestUtils.setField(event, "stime", stime);
        ReflectionTestUtils.setField(event, "etime", etime);
        return event;
    }
}
