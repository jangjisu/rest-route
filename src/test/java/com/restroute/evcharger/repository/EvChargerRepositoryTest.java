package com.restroute.evcharger.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restroute.evcharger.client.response.EvChargerItem;
import com.restroute.evcharger.domain.EvChargerEntity;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class EvChargerRepositoryTest {

    @Autowired
    private EvChargerRepository evChargerRepository;

    @Test
    @DisplayName("삭제되지 않은 EV 충전기만 조회한다")
    void findAllByDelYn_returnsMatchingChargers() throws Exception {
        evChargerRepository.save(charger("ME1", "N"));
        evChargerRepository.save(charger("ME2", "Y"));

        List<EvChargerEntity> result = evChargerRepository.findAllByDelYn("N");

        assertThat(result).extracting(EvChargerEntity::getStatId).containsExactly("ME1");
    }

    private EvChargerEntity charger(String statId, String delYn) throws Exception {
        EvChargerItem item = new ObjectMapper()
                .readValue(
                        "{\"statId\":\"" + statId + "\",\"chgerId\":\"01\",\"delYn\":\"" + delYn + "\"}",
                        EvChargerItem.class);
        return EvChargerEntity.from(item);
    }
}
