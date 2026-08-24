package com.restroute.reststop.repository;

import com.restroute.reststop.domain.HighwayServiceAreaInfoEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HighwayServiceAreaInfoRepository extends JpaRepository<HighwayServiceAreaInfoEntity, Long> {

    List<HighwayServiceAreaInfoEntity> findAllByBusinessFacilityCode(String businessFacilityCode);

    List<HighwayServiceAreaInfoEntity> findAllByRestStopServiceAreaCode(String restStopServiceAreaCode);

    List<HighwayServiceAreaInfoEntity> findAllByRestStopServiceAreaCodeIn(Collection<String> restStopServiceAreaCodes);
}
