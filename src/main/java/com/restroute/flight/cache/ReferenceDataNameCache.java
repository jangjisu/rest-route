package com.restroute.flight.cache;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * code→표시명(kor 우선, eng 대체) 조회를 위한 순수 인메모리 캐시의 공통 뼈대. 항공사/공항/도시/
 * 국가 4종 참조 데이터가 "code로 조회, kor 없으면 eng로 대체, refresh로 통째로 다시 채움"
 * 로직을 그대로 공유해서 여기 한 곳에 모았다 — DB 조회 방식과 엔티티에서 code/kor/eng를
 * 꺼내는 방법만 서브클래스가 정한다.
 */
public abstract class ReferenceDataNameCache<T> {

    private final Supplier<List<T>> findAll;
    private final Function<T, String> codeOf;
    private final Function<T, String> korNameOf;
    private final Function<T, String> engNameOf;

    private volatile Map<String, String> nameByCode = Map.of();

    protected ReferenceDataNameCache(
            Supplier<List<T>> findAll,
            Function<T, String> codeOf,
            Function<T, String> korNameOf,
            Function<T, String> engNameOf) {
        this.findAll = findAll;
        this.codeOf = codeOf;
        this.korNameOf = korNameOf;
        this.engNameOf = engNameOf;
    }

    public String findName(String code) {
        return nameByCode.get(code);
    }

    public void refresh() {
        List<T> all = findAll.get();
        nameByCode = all.stream().collect(Collectors.toUnmodifiableMap(codeOf, this::displayName));
        afterRefresh(all);
    }

    private String displayName(T entity) {
        String korName = korNameOf.apply(entity);
        return korName != null ? korName : engNameOf.apply(entity);
    }

    /** refresh 때 이름 맵 말고 추가 상태(예: 항공사 저비용 여부)도 함께 채우고 싶은 서브클래스가 오버라이드한다. */
    protected void afterRefresh(List<T> all) {}
}
