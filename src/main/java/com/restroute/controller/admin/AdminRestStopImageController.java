package com.restroute.controller.admin;

import com.restroute.admin.service.AdminActivityLogService;
import com.restroute.service.image.RestStopImageCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class AdminRestStopImageController {

    private static final String IMAGE_SAVED_MESSAGE = "휴게소(%s) 이미지를 등록했습니다.";
    private static final String IMAGE_DELETED_MESSAGE = "휴게소(%s) 이미지를 삭제했습니다.";

    private final RestStopImageCommandService commandService;
    private final AdminActivityLogService adminActivityLogService;

    @PutMapping(path = "/{serviceAreaCode}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> save(
            @PathVariable String serviceAreaCode, @RequestPart MultipartFile file, Authentication authentication) {
        commandService.save(serviceAreaCode, file);
        adminActivityLogService.log(authentication, String.format(IMAGE_SAVED_MESSAGE, serviceAreaCode));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{serviceAreaCode}/image")
    public ResponseEntity<Void> delete(@PathVariable String serviceAreaCode, Authentication authentication) {
        commandService.delete(serviceAreaCode);
        adminActivityLogService.log(authentication, String.format(IMAGE_DELETED_MESSAGE, serviceAreaCode));
        return ResponseEntity.noContent().build();
    }
}
