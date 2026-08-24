package com.restroute.reststop.controller;

import com.restroute.reststop.service.image.RestStopImageQueryService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;

@Controller
@ResponseBody
@RequiredArgsConstructor
@RequestMapping("/api/rest-stops")
public class RestStopImageController {

    private static final MediaType WEBP_MEDIA_TYPE = MediaType.parseMediaType("image/webp");
    private static final String CACHE_CONTROL_VALUE = "public, no-cache";

    private final RestStopImageQueryService queryService;

    @GetMapping("/{serviceAreaCode}/images/detail")
    public ResponseEntity<byte[]> getDetailImage(@PathVariable String serviceAreaCode, ServletWebRequest webRequest) {
        return imageResponse(queryService.findDetailImage(serviceAreaCode), webRequest);
    }

    @GetMapping("/{serviceAreaCode}/images/list")
    public ResponseEntity<byte[]> getListImage(@PathVariable String serviceAreaCode, ServletWebRequest webRequest) {
        return imageResponse(queryService.findListImage(serviceAreaCode), webRequest);
    }

    private ResponseEntity<byte[]> imageResponse(Optional<byte[]> image, ServletWebRequest webRequest) {
        if (image.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        byte[] imageData = image.get();
        String eTag = "\"" + DigestUtils.md5DigestAsHex(imageData) + "\"";
        if ("*".equals(webRequest.getHeader(HttpHeaders.IF_NONE_MATCH)) || webRequest.checkNotModified(eTag)) {
            return ResponseEntity.status(304)
                    .eTag(eTag)
                    .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE)
                    .build();
        }
        return ResponseEntity.ok()
                .contentType(WEBP_MEDIA_TYPE)
                .eTag(eTag)
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_VALUE)
                .body(imageData);
    }
}
