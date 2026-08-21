package com.restroute.flight.service;

import com.restroute.flight.controller.dto.FlightSearchRequestDto;
import com.restroute.flight.controller.exception.FlightDealNotFoundException;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchMeta;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import com.restroute.flight.service.util.FlightDealResponses;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

/**
 * 검색 세션 저장소 — 조회 결과를 세션 토큰으로 메모리에 잠깐 캐시해두고 cursor로 이어서
 * 페이지를 잘라 서빙한다. 세션 토큰, 저장된 리스트, 페이지 계산(subList/hasNext/nextCursor)
 * 전부 이 클래스만 아는 내부 구현 세부사항이다.
 *
 * <p>호출부(={@link FlightSearchService})는 "새 세션을 만들지 vs 기존 세션을 이어갈지"만
 * 판단하고, {@link #create}/{@link #find} 둘 중 하나를 불러서 완성된
 * {@link FlightDealSearchResponse}를 그대로 받는다.
 */
@Component
class FlightDealSessionStore {

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(300);
    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 4;
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    /** reserveToken()이 토큰을 원자적으로 선점하는 동안 잠깐 채워두는 자리표시자. */
    private static final CachedSession RESERVED = new CachedSession(null, List.of(), Map.of(), Instant.MAX, null);

    private final Map<String, CachedSession> sessions = new ConcurrentHashMap<>();
    private final Duration ttl;

    FlightDealSessionStore() {
        this(DEFAULT_TTL);
    }

    FlightDealSessionStore(Duration ttl) {
        this.ttl = ttl;
    }

    static FlightDealSessionStore create() {
        return new FlightDealSessionStore();
    }

    /**
     * 새 세션을 만든다 — 토큰 발급, {@code fetcher}로 조회, id 부여, 저장, 첫 페이지 계산까지
     * 전부 여기서 처리한다. 토큰은 이 클래스만 알면 되므로 {@code fetcher}에는 넘기지 않는다 —
     * fetch 결과가 나온 뒤 이 클래스가 각 항목에 최종 id(토큰+순번)를 부여한다.
     */
    FlightDealSearchResponse create(FlightSearchRequestDto request, int size, Fetcher fetcher) {
        String token = reserveToken();
        List<FlightDealResponse> items = stampIds(fetcher.fetch(), token);
        OffsetDateTime fetchedAt = OffsetDateTime.now(KST);
        save(token, request, items, fetchedAt);
        return toResponse(items, 0, size, request, fetchedAt);
    }

    private static List<FlightDealResponse> stampIds(List<FlightDealResponse> items, String token) {
        return IntStream.range(0, items.size())
                .mapToObj(index -> FlightDealResponses.withId(items.get(index), FlightDealResponses.idOf(token, index)))
                .toList();
    }

    /**
     * cursor로 세션을 찾아 이어지는 페이지를 반환한다. 세션을 못 찾으면(형식 이상·만료·조건
     * 불일치 포함) "존재하지 않으면 실패"가 고정 규칙이라 {@link FlightDealNotFoundException}을
     * 던진다. fetchedAt은 세션이 처음 만들어진 시점 값을 그대로 이어 쓴다 — 같은 검색의
     * 페이지들은 전부 "언제 조회됐는지"가 같아야 한다.
     */
    FlightDealSearchResponse find(FlightSearchRequestDto request, String cursor, int size) {
        CachedSession session = requireSession(request, cursor);
        int startIndex = indexOf(session, cursor) + 1;
        return toResponse(session.items(), startIndex, size, request, session.fetchedAt());
    }

    @FunctionalInterface
    interface Fetcher {
        List<FlightDealResponse> fetch();
    }

    /**
     * 토큰을 뽑고 그 자리에 {@link #RESERVED}를 원자적으로 선점해둔다 — putIfAbsent 하나로
     * 확인과 선점을 한 번에 처리해서, 조회 후 나중에 저장하는 사이에 다른 요청이 같은 토큰을
     * 골라 세션을 덮어쓰는 경합을 막는다. 실제 fetcher 호출은 초 단위로 걸릴 수 있어 그 사이의
     * 창이 실제로 위험하다.
     */
    private String reserveToken() {
        String token;
        do {
            token = randomToken();
        } while (sessions.putIfAbsent(token, RESERVED) != null);
        return token;
    }

    private void save(
            String token, FlightSearchRequestDto request, List<FlightDealResponse> items, OffsetDateTime fetchedAt) {
        Map<String, Integer> indexById = buildIndex(items);
        CachedSession session =
                new CachedSession(request, items, indexById, Instant.now().plus(ttl), fetchedAt);
        sessions.put(token, session);
    }

    private CachedSession requireSession(FlightSearchRequestDto request, String cursor) {
        CachedSession cached = tryFind(request, cursor);
        if (cached == null) {
            throw new FlightDealNotFoundException(cursor);
        }
        return cached;
    }

    private CachedSession tryFind(FlightSearchRequestDto request, String cursor) {
        String token = tokenOf(cursor);
        if (token == null) {
            return null;
        }
        CachedSession cached = sessions.get(token);
        if (cached == null || cached == RESERVED) {
            return null;
        }
        if (Instant.now().isAfter(cached.expiresAt())) {
            sessions.remove(token);
            return null;
        }
        if (!cached.matches(request)) {
            return null;
        }
        return cached;
    }

    private static int indexOf(CachedSession session, String cursor) {
        Integer index = session.indexById().get(cursor);
        if (index == null) {
            throw new FlightDealNotFoundException(cursor);
        }
        return index;
    }

    private static String tokenOf(String cursor) {
        if (cursor == null) {
            return null;
        }
        int separatorIndex = cursor.indexOf('_');
        if (separatorIndex <= 0) {
            return null;
        }
        return cursor.substring(0, separatorIndex);
    }

    private static String randomToken() {
        StringBuilder token = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(TOKEN_CHARS.charAt(ThreadLocalRandom.current().nextInt(TOKEN_CHARS.length())));
        }
        return token.toString();
    }

    private static Map<String, Integer> buildIndex(List<FlightDealResponse> items) {
        Map<String, Integer> indexById = new LinkedHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            indexById.put(items.get(i).id(), i);
        }
        return indexById;
    }

    private static FlightDealSearchResponse toResponse(
            List<FlightDealResponse> items,
            int startIndex,
            int size,
            FlightSearchRequestDto request,
            OffsetDateTime fetchedAt) {
        int endIndex = Math.min(startIndex + size, items.size());
        List<FlightDealResponse> page = items.subList(startIndex, endIndex);
        boolean hasNext = endIndex < items.size();
        String nextCursor = hasNext ? page.get(page.size() - 1).id() : null;
        FlightDealSearchMeta meta = FlightDealSearchMeta.of(
                nextCursor,
                hasNext,
                items.size(),
                request.parsedLocale(),
                request.parsedCurrency(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(fetchedAt));
        return FlightDealSearchResponse.of(page, meta);
    }

    private record CachedSession(
            FlightSearchRequestDto request,
            List<FlightDealResponse> items,
            Map<String, Integer> indexById,
            Instant expiresAt,
            OffsetDateTime fetchedAt) {

        boolean matches(FlightSearchRequestDto otherRequest) {
            return request.equals(otherRequest);
        }
    }
}
