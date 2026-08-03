package com.restroute.controller.admin;

import com.restroute.service.admin.AdminActivityLogService;
import com.restroute.service.admin.AdminRestFoodImageCommandService;
import com.restroute.service.admin.AdminRestFoodImageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@ResponseBody
@RequiredArgsConstructor
@RequestMapping("/api/admin/rest-stops")
public class AdminRestFoodImageController {

    private final AdminRestFoodImageCommandService commandService;
    private final AdminRestFoodImageQueryService queryService;
    private final AdminActivityLogService adminActivityLogService;

    @GetMapping("/{serviceAreaCode}/foods/{foodId}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable String serviceAreaCode, @PathVariable Long foodId) {
        return queryService
                .findListImage(serviceAreaCode, foodId)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("image/webp"))
                        .body(image))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping(path = "/{serviceAreaCode}/foods/{foodId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> save(
            @PathVariable String serviceAreaCode,
            @PathVariable Long foodId,
            @RequestPart MultipartFile file,
            Authentication authentication) {
        commandService.save(serviceAreaCode, foodId, file);
        adminActivityLogService.logCustomFoodImageSaved(authentication, foodId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{serviceAreaCode}/foods/{foodId}/image")
    public ResponseEntity<Void> delete(
            @PathVariable String serviceAreaCode, @PathVariable Long foodId, Authentication authentication) {
        commandService.delete(serviceAreaCode, foodId);
        adminActivityLogService.logCustomFoodImageDeleted(authentication, foodId);
        return ResponseEntity.noContent().build();
    }
}
