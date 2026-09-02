package com.restroute.common.controller;

import java.util.regex.Pattern;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Controller
public class HomeController {

    private static final Pattern MOBILE_USER_AGENT = Pattern.compile("Mobi", Pattern.CASE_INSENSITIVE);

    @GetMapping("/")
    public String index(@RequestHeader(value = "User-Agent", required = false) String userAgent) {
        if (isMobile(userAgent)) {
            return "finder";
        }
        return "index";
    }

    @GetMapping("/finder")
    public String finder() {
        return "finder";
    }

    private boolean isMobile(String userAgent) {
        return userAgent != null && MOBILE_USER_AGENT.matcher(userAgent).find();
    }
}
