package com.restroute.flight.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.service.dto.FlightRangeSearchPlan;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlightRangeSearchPlannerTest {

    private static String futureDate(int daysFromToday) {
        return LocalDate.now().plusDays(daysFromToday).toString();
    }

    @Test
    @DisplayName("destination을 직접 지정하면 destinations는 그 하나뿐이다")
    void plan_usesSingleDestination_whenDestinationGiven() {
        FlightSearchRequestDto request = request(futureDate(10), futureDate(20), "OSA", null, List.of("3"));

        FlightRangeSearchPlan plan = FlightRangeSearchPlanner.plan(request);

        assertThat(plan.destinations()).containsExactly("OSA");
    }

    @Test
    @DisplayName("destination을 직접 지정하면 예산이 남아도 전체 조회를 얹지 않는다 — 사용자가 정확히 그 하나만 원한 것이다")
    void plan_neverAddsAggregateDestination_whenDestinationDirectlyGiven() {
        FlightSearchRequestDto request = request(futureDate(10), futureDate(20), "OSA", null, List.of("3", "4", "5"));

        FlightRangeSearchPlan plan = FlightRangeSearchPlanner.plan(request);

        assertThat(plan.destinations()).containsExactly("OSA");
    }

    @Test
    @DisplayName("sector/destination이 둘 다 없으면 destinations는 빈 목록이다(생략)")
    void plan_returnsEmptyDestinations_whenNeitherGiven() {
        FlightSearchRequestDto request = request(futureDate(10), futureDate(20), null, null, List.of("3"));

        FlightRangeSearchPlan plan = FlightRangeSearchPlanner.plan(request);

        assertThat(plan.destinations()).isEmpty();
    }

    @Test
    @DisplayName("예산(20) 안이면 nights를 요청한 값 각각 정확한 창으로 쪼갠다")
    void plan_usesExactNightsWindows_whenWithinBudget() {
        FlightSearchRequestDto request =
                request(futureDate(10), futureDate(20), null, List.of("JAPAN"), List.of("3", "4", "5"));

        FlightRangeSearchPlan plan = FlightRangeSearchPlanner.plan(request);

        assertThat(plan.destinations()).containsExactly("JP", null);
        assertThat(plan.nightsWindows())
                .containsExactly(
                        new FlightRangeSearchPlan.NightsWindow(3, 3),
                        new FlightRangeSearchPlan.NightsWindow(4, 4),
                        new FlightRangeSearchPlan.NightsWindow(5, 5));
    }

    @Test
    @DisplayName("sector를 4개 다 고르면(9개국) nights 3개와 곱해 20을 넘어서 범위 모드로 낮추고, 전체 조회를 하나 더 얹는다")
    void plan_downgradesToRangeWindow_whenExactNightsExceedsBudget() {
        FlightSearchRequestDto request = request(
                futureDate(10),
                futureDate(20),
                null,
                List.of("JAPAN", "SOUTHEAST_ASIA", "GREATER_CHINA", "GUAM_SAIPAN"),
                List.of("3", "4", "5"));

        FlightRangeSearchPlan plan = FlightRangeSearchPlanner.plan(request);

        assertThat(plan.destinations()).hasSize(10).contains((String) null);
        assertThat(plan.nightsWindows()).containsExactly(new FlightRangeSearchPlan.NightsWindow(3, 5));
    }

    @Test
    @DisplayName("sector로 여러 국가가 잡히면 예산 안에서 국가별 조회에 전체 조회를 하나 더 얹는다")
    void plan_addsAggregateDestination_whenBudgetAllows() {
        FlightSearchRequestDto request =
                request(futureDate(10), futureDate(20), null, List.of("JAPAN"), List.of("3", "4", "5"));

        FlightRangeSearchPlan plan = FlightRangeSearchPlanner.plan(request);

        assertThat(plan.destinations()).hasSize(2).containsExactly("JP", null);
    }

    @Test
    @DisplayName("국가별+전체를 합쳐도 예산을 넘으면 국가별 조회를 포기하고 전체 조회 하나만 한다")
    void plan_dropsPerCountryDestinations_whenAggregateBudgetExceeded() {
        LocalDate dateFrom = LocalDate.now();
        LocalDate dateTo = dateFrom.plusMonths(3);
        FlightSearchRequestDto request = request(
                dateFrom.toString(),
                dateTo.toString(),
                null,
                List.of("JAPAN", "SOUTHEAST_ASIA", "GREATER_CHINA", "GUAM_SAIPAN"),
                List.of("3"));

        FlightRangeSearchPlan plan = FlightRangeSearchPlanner.plan(request);

        assertThat(plan.months()).hasSize(4);
        assertThat(plan.destinations()).isEmpty();
    }

    @Test
    @DisplayName("nights를 생략하면(자동 확장) 항상 범위 모드다")
    void plan_alwaysUsesRangeWindow_whenNightsOmitted() {
        FlightSearchRequestDto request = request(futureDate(10), futureDate(15), null, List.of("JAPAN"), null);

        FlightRangeSearchPlan plan = FlightRangeSearchPlanner.plan(request);

        assertThat(plan.nightsWindows()).containsExactly(new FlightRangeSearchPlan.NightsWindow(1, 5));
    }

    @Test
    @DisplayName("nights에 중복값이 있으면 중복 없이 창을 만든다")
    void plan_dedupesDuplicateNightsValues() {
        FlightSearchRequestDto request =
                request(futureDate(10), futureDate(20), null, List.of("JAPAN"), List.of("3", "3", "4"));

        FlightRangeSearchPlan plan = FlightRangeSearchPlanner.plan(request);

        assertThat(plan.nightsWindows())
                .containsExactly(
                        new FlightRangeSearchPlan.NightsWindow(3, 3), new FlightRangeSearchPlan.NightsWindow(4, 4));
    }

    @Test
    @DisplayName("dateFrom~dateTo가 두 달에 걸치면 months는 두 항목이다")
    void plan_listsEachCalendarMonthTouchedByRange() {
        LocalDate dateFrom =
                java.time.YearMonth.from(LocalDate.now().plusDays(20)).atEndOfMonth();
        LocalDate dateTo = dateFrom.plusDays(1);
        FlightSearchRequestDto request =
                request(dateFrom.toString(), dateTo.toString(), null, List.of("JAPAN"), List.of("3"));

        FlightRangeSearchPlan plan = FlightRangeSearchPlanner.plan(request);

        assertThat(plan.months())
                .containsExactly(
                        dateFrom.getYear() + "-" + "%02d".formatted(dateFrom.getMonthValue()),
                        dateTo.getYear() + "-" + "%02d".formatted(dateTo.getMonthValue()));
    }

    private static FlightSearchRequestDto request(
            String dateFrom, String dateTo, String destination, List<String> sector, List<String> nights) {
        return new FlightSearchRequestDto(
                "ICN",
                "range",
                dateFrom,
                dateTo,
                destination,
                nights,
                sector,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
