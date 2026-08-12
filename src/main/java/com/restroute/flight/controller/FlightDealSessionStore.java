package com.restroute.flight.controller;

import com.restroute.flight.controller.exception.FlightDealNotFoundException;
import com.restroute.flight.controller.response.FlightDealResponse;
import com.restroute.flight.controller.response.FlightDealSearchMeta;
import com.restroute.flight.controller.response.FlightDealSearchResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
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

    private final Map<String, CachedSession> sessions = new ConcurrentHashMap<>();
    private final Duration ttl;

    FlightDealSessionStore() {
        this(DEFAULT_TTL);
    }

    FlightDealSessionStore(Duration ttl) {
        this.ttl = ttl;
    }

    /**
     * 새 세션을 만든다 — 토큰 발급, {@code fetcher}로 조회, 저장, 첫 페이지 계산까지 전부
     * 여기서 처리한다.
     */
    FlightDealSearchResponse create(FlightSearchRequestDto request, Integer totalSize, int size, Fetcher fetcher) {
        String token = reserveToken();
        List<FlightDealResponse> items = fetcher.fetch(token);
        save(token, request, totalSize, items);
        return toResponse(items, 0, size);
    }

    /**
     * cursor로 세션을 찾아 이어지는 페이지를 반환한다. 세션을 못 찾으면(형식 이상·만료·조건
     * 불일치 포함) "존재하지 않으면 실패"가 고정 규칙이라 {@link FlightDealNotFoundException}을
     * 던진다.
     */
    FlightDealSearchResponse find(FlightSearchRequestDto request, Integer totalSize, String cursor, int size) {
        CachedSession session = requireSession(request, totalSize, cursor);
        int startIndex = indexOf(session, cursor) + 1;
        return toResponse(session.items(), startIndex, size);
    }

    @FunctionalInterface
    interface Fetcher {
        List<FlightDealResponse> fetch(String token);
    }

    private String reserveToken() {
        String token;
        do {
            token = randomToken();
        } while (sessions.containsKey(token));
        return token;
    }

    private void save(String token, FlightSearchRequestDto request, Integer totalSize, List<FlightDealResponse> items) {
        Map<String, Integer> indexById = buildIndex(items);
        CachedSession session = new CachedSession(
                request, totalSize, items, indexById, Instant.now().plus(ttl));
        sessions.put(token, session);
    }

    private CachedSession requireSession(FlightSearchRequestDto request, Integer totalSize, String cursor) {
        CachedSession cached = tryFind(request, totalSize, cursor);
        if (cached == null) {
            throw new FlightDealNotFoundException(cursor);
        }
        return cached;
    }

    private CachedSession tryFind(FlightSearchRequestDto request, Integer totalSize, String cursor) {
        String token = tokenOf(cursor);
        if (token == null) {
            return null;
        }
        CachedSession cached = sessions.get(token);
        if (cached == null) {
            return null;
        }
        if (Instant.now().isAfter(cached.expiresAt())) {
            sessions.remove(token);
            return null;
        }
        if (!cached.matches(request, totalSize)) {
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

    private static FlightDealSearchResponse toResponse(List<FlightDealResponse> items, int startIndex, int size) {
        int endIndex = Math.min(startIndex + size, items.size());
        List<FlightDealResponse> page = items.subList(startIndex, endIndex);
        boolean hasNext = endIndex < items.size();
        String nextCursor = hasNext ? page.get(page.size() - 1).id() : null;
        return FlightDealSearchResponse.of(page, FlightDealSearchMeta.of(nextCursor, hasNext, items.size()));
    }

    private record CachedSession(
            FlightSearchRequestDto request,
            Integer totalSize,
            List<FlightDealResponse> items,
            Map<String, Integer> indexById,
            Instant expiresAt) {

        boolean matches(FlightSearchRequestDto otherRequest, Integer otherTotalSize) {
            return Objects.equals(totalSize, otherTotalSize) && request.equals(otherRequest);
        }
    }
}
