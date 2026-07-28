package com.restroute.service;

import com.restroute.client.ExApiClient;
import com.restroute.client.response.RestThemeItem;
import com.restroute.client.response.RestThemeResponse;
import com.restroute.domain.RestThemeEntity;
import com.restroute.repository.RestThemeRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class RestThemeSyncService {

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
        Map<String, RestThemeEntity> existingByKey = restThemeRepository.findAll().stream()
                .collect(Collectors.toMap(
                        entity -> themeKey(entity.getStdRestCd(), entity.getItemNm()),
                        entity -> entity,
                        (first, second) -> first));

        List<RestThemeEntity> toSave = new ArrayList<>();
        for (RestThemeItem item : items) {
            toSave.add(upsertOne(item, existingByKey));
        }
        restThemeRepository.saveAll(toSave);
    }

    private RestThemeEntity upsertOne(RestThemeItem item, Map<String, RestThemeEntity> existingByKey) {
        String key = themeKey(item.getStdRestCd(), item.getItemNm());
        RestThemeEntity existing = existingByKey.get(key);
        if (existing == null) {
            RestThemeEntity created = RestThemeEntity.from(item);
            existingByKey.put(key, created);
            return created;
        }
        existing.updateFrom(item);
        return existing;
    }

    private String themeKey(String stdRestCd, String itemNm) {
        return stdRestCd + "\n" + itemNm;
    }
}
