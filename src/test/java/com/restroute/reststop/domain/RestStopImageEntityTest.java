package com.restroute.reststop.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class RestStopImageEntityTest {

    @Test
    void imageDataColumnsUseMediumBlob() throws NoSuchFieldException {
        assertThat(columnDefinitionOf("detailImageData")).isEqualTo("MEDIUMBLOB");
        assertThat(columnDefinitionOf("listImageData")).isEqualTo("MEDIUMBLOB");
    }

    private String columnDefinitionOf(String fieldName) throws NoSuchFieldException {
        Field field = RestStopImageEntity.class.getDeclaredField(fieldName);
        return field.getAnnotation(Column.class).columnDefinition();
    }
}
