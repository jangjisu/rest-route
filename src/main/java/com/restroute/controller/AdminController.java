package com.restroute.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin")
    public String admin() {
        return "admin-dashboard";
    }

    @GetMapping("/admin/rest-stops/images")
    public String restStopImages() {
        return "admin-rest-stop-images";
    }

    @GetMapping("/admin/rest-stops/edit")
    public String restStopEdit() {
        return "admin-rest-stop-edit";
    }

    @GetMapping("/admin/rest-stops/foods")
    public String restStopFoods() {
        return "admin-rest-stop-foods";
    }

    @GetMapping("/admin/rest-stops/oil-links")
    public String restStopOilLinks() {
        return "admin-rest-stop-oil-links";
    }
}
