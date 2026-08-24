package com.restroute.service.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 외부 API 동기화 서비스들이 공통으로 쓰는 "자연키로 기존 엔티티를 찾아 있으면 갱신하고,
 * 없으면 새로 만든다" upsert 패턴을 하나로 묶는다. 각 서비스는 키 추출/생성/갱신 방법만
 * 제공하면 되고, 저장(findAll/saveAll)은 호출하는 쪽의 책임으로 남긴다.
 */
public final class NaturalKeyUpserter<ITEM, KEY, ENTITY> {

    private final Function<ENTITY, KEY> entityKeyFn;
    private final Function<ITEM, KEY> itemKeyFn;
    private final Function<ITEM, ENTITY> createFn;
    private final BiConsumer<ENTITY, ITEM> updateFn;

    private NaturalKeyUpserter(
            Function<ENTITY, KEY> entityKeyFn,
            Function<ITEM, KEY> itemKeyFn,
            Function<ITEM, ENTITY> createFn,
            BiConsumer<ENTITY, ITEM> updateFn) {
        this.entityKeyFn = entityKeyFn;
        this.itemKeyFn = itemKeyFn;
        this.createFn = createFn;
        this.updateFn = updateFn;
    }

    public static <ITEM, KEY, ENTITY> NaturalKeyUpserter<ITEM, KEY, ENTITY> of(
            Function<ENTITY, KEY> entityKeyFn,
            Function<ITEM, KEY> itemKeyFn,
            Function<ITEM, ENTITY> createFn,
            BiConsumer<ENTITY, ITEM> updateFn) {
        return new NaturalKeyUpserter<>(entityKeyFn, itemKeyFn, createFn, updateFn);
    }

    public List<ENTITY> upsert(List<ITEM> items, List<ENTITY> existingEntities) {
        Map<KEY, ENTITY> existingByKey = existingEntities.stream()
                .collect(Collectors.toMap(entityKeyFn, entity -> entity, (first, second) -> first));

        List<ENTITY> toSave = new ArrayList<>();
        for (ITEM item : items) {
            toSave.add(upsertOne(item, existingByKey));
        }
        return toSave;
    }

    private ENTITY upsertOne(ITEM item, Map<KEY, ENTITY> existingByKey) {
        KEY key = itemKeyFn.apply(item);
        ENTITY existing = existingByKey.get(key);

        if (existing == null) {
            ENTITY created = createFn.apply(item);
            existingByKey.put(key, created);
            return created;
        }

        updateFn.accept(existing, item);
        return existing;
    }
}
