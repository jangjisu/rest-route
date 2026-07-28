package com.restroute.repository;

import com.restroute.domain.RestThemeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestThemeRepository extends JpaRepository<RestThemeEntity, Long> {

    List<RestThemeEntity> findAllByRestStopServiceAreaCodeOrderByIdAsc(String restStopServiceAreaCode);
}
