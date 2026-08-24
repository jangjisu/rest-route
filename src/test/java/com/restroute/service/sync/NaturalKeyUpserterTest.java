package com.restroute.service.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NaturalKeyUpserterTest {

    private record Item(String key, String value) {}

    private static final class Entity {
        private final String key;
        private String value;

        private Entity(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private final NaturalKeyUpserter<Item, String, Entity> upserter = NaturalKeyUpserter.of(
            entity -> entity.key,
            Item::key,
            item -> new Entity(item.key(), item.value()),
            (entity, item) -> entity.value = item.value());

    @Test
    @DisplayName("기존에 없는 자연키는 새 엔티티로 생성한다")
    void upsert_createsNewEntityWhenKeyIsAbsent() {
        List<Entity> result = upserter.upsert(List.of(new Item("A", "새값")), List.of());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).key).isEqualTo("A");
        assertThat(result.get(0).value).isEqualTo("새값");
    }

    @Test
    @DisplayName("기존 자연키가 있으면 새로 만들지 않고 같은 엔티티를 갱신한다")
    void upsert_updatesExistingEntityWhenKeyIsPresent() {
        Entity existing = new Entity("A", "옛값");

        List<Entity> result = upserter.upsert(List.of(new Item("A", "새값")), List.of(existing));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(existing);
        assertThat(existing.value).isEqualTo("새값");
    }

    @Test
    @DisplayName("같은 자연키를 가진 항목이 여러 번 오면 먼저 생성된 하나만 유지한다")
    void upsert_keepsFirstOccurrenceWhenDuplicateNaturalKeysAppear() {
        List<Entity> result = upserter.upsert(List.of(new Item("A", "1번째"), new Item("A", "2번째")), List.of());

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isSameAs(result.get(1));
        assertThat(result.get(0).value).isEqualTo("2번째");
    }

    @Test
    @DisplayName("빈 목록을 넘기면 아무것도 생성하거나 갱신하지 않는다")
    void upsert_returnsEmptyListForEmptyItems() {
        assertThat(upserter.upsert(List.of(), List.of(new Entity("A", "값")))).isEmpty();
    }
}
