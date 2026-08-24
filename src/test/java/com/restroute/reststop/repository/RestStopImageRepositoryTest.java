package com.restroute.reststop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.restroute.reststop.domain.RestStopImageEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RestStopImageRepositoryTest {

    @Autowired
    private RestStopImageRepository repository;

    @Test
    void findsStoredImageVariantsWithoutLoadingTheEntity() {
        byte[] detail = {1, 2, 3};
        byte[] list = {4, 5, 6};
        repository.save(RestStopImageEntity.of("A00001", detail, list));

        assertThat(repository.findDetailImageDataByServiceAreaCode("A00001")).contains(detail);
        assertThat(repository.findListImageDataByServiceAreaCode("A00001")).contains(list);
        assertThat(repository.findServiceAreaCodesIn(List.of("A00001", "UNKNOWN")))
                .containsExactly("A00001");
    }
}
