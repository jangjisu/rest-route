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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * 모킹 API 전용 검색 세션 저장소. 실제 연동에서 붙게 될 "검색 결과를 짧게 캐시해두고 cursor로
 * 잘라서 서빙"하는 구조를 모킹 단계에서부터 그대로 흉내낸다.
 *
 * <p>cursor가 없거나, 그 세션이 캐시에 없거나(만료 포함), 있어도 이번 요청의 검색 조건이 그
 * 세션에 저장된 조건과 다르면 에러 없이 새 세션을 만든다. 조건이 일치하는 세션을 찾았는데 그
 * 안에 그 id가 실제로 없는 경우에만 {@link FlightDealNotFoundException}을 던진다.
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

    FlightDealSearchResponse page(
            FlightSearchRequestValidator.ValidatedRequest request, int totalSize, String cursor, int size) {
        CachedSession existing = findMatchingSession(request, totalSize, cursor);

        CachedSession session;
        int startIndex;
        if (existing != null) {
            Integer index = existing.indexById().get(cursor);
            if (index == null) {
                throw new FlightDealNotFoundException(cursor);
            }
            session = existing;
            startIndex = index + 1;
        } else {
            session = createSession(request, totalSize);
            startIndex = 0;
        }

        List<FlightDealResponse> all = session.items();
        int endIndex = Math.min(startIndex + size, all.size());
        List<FlightDealResponse> items = all.subList(startIndex, endIndex);
        boolean hasNext = endIndex < all.size();
        String nextCursor = hasNext ? items.get(items.size() - 1).id() : null;

        return new FlightDealSearchResponse(items, new FlightDealSearchMeta(nextCursor, hasNext, all.size()));
    }

    private CachedSession findMatchingSession(
            FlightSearchRequestValidator.ValidatedRequest request, int totalSize, String cursor) {
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

    private CachedSession createSession(FlightSearchRequestValidator.ValidatedRequest request, int totalSize) {
        String token = generateUniqueToken();
        List<FlightDealResponse> items = FlightSearchMockFixture.generateAll(request, token, totalSize);
        Map<String, Integer> indexById = buildIndex(items);
        CachedSession session = new CachedSession(
                request, totalSize, items, indexById, Instant.now().plus(ttl));
        sessions.put(token, session);
        return session;
    }

    private String generateUniqueToken() {
        String token;
        do {
            token = randomToken();
        } while (sessions.containsKey(token));
        return token;
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

    private record CachedSession(
            FlightSearchRequestValidator.ValidatedRequest request,
            int totalSize,
            List<FlightDealResponse> items,
            Map<String, Integer> indexById,
            Instant expiresAt) {

        boolean matches(FlightSearchRequestValidator.ValidatedRequest otherRequest, int otherTotalSize) {
            return totalSize == otherTotalSize && request.equals(otherRequest);
        }
    }
}
