package com.restroute.reststopcontent.service;

import com.restroute.common.client.ExApiClient;
import com.restroute.common.sync.NaturalKeyUpserter;
import com.restroute.reststopcontent.client.response.RestThemeItem;
import com.restroute.reststopcontent.client.response.RestThemeResponse;
import com.restroute.reststopcontent.domain.RestThemeEntity;
import com.restroute.reststopcontent.repository.RestThemeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class RestThemeSyncService {

    private static final NaturalKeyUpserter<RestThemeItem, String, RestThemeEntity> UPSERTER = NaturalKeyUpserter.of(
            entity -> themeKey(entity.getStdRestCd(), entity.getItemNm()),
            item -> themeKey(item.getStdRestCd(), item.getItemNm()),
            RestThemeEntity::from,
            RestThemeEntity::updateFrom);

    private final ExApiClient exApiClient;
    private final RestThemeRepository restThemeRepository;
    private final TransactionTemplate transactionTemplate;

    public int initializeRestThemesIfEmpty() {
        if (restThemeRepository.count() > 0) {
            return 0;
        }

        return refreshRestThemes();
    }

    public int refreshRestThemes() {
        List<RestThemeItem> items = fetchRestThemes();
        transactionTemplate.executeWithoutResult(status -> upsertRestThemes(items));
        return items.size();
    }

    private List<RestThemeItem> fetchRestThemes() {
        RestThemeResponse response = exApiClient.getRestThemeList();
        if (response.getList() == null) {
            return List.of();
        }
        return response.getList();
    }

    private void upsertRestThemes(List<RestThemeItem> items) {
        List<RestThemeEntity> toSave = UPSERTER.upsert(items, restThemeRepository.findAll());
        restThemeRepository.saveAll(toSave);
    }

    private static String themeKey(String stdRestCd, String itemNm) {
        return stdRestCd + "\n" + itemNm;
    }
}
