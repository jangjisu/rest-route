package com.restroute.reststopcontent.repository;

import com.restroute.reststopcontent.domain.RestThemeEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestThemeRepository extends JpaRepository<RestThemeEntity, Long> {

    List<RestThemeEntity> findAllByRestStopServiceAreaCodeOrderByIdAsc(String restStopServiceAreaCode);

    List<RestThemeEntity> findAllByRestStopServiceAreaCodeIn(Collection<String> serviceAreaCodes);
}
