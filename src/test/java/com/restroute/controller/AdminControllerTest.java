package com.restroute.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminControllerTest {

    private final AdminController controller = new AdminController();

    @Test
    @DisplayName("GET /admin은 관리자 대시보드 템플릿을 반환한다")
    void admin_returnsAdminDashboardView() {
        assertThat(controller.admin()).isEqualTo("admin-dashboard");
    }

    @Test
    @DisplayName("GET /admin/rest-stops/images는 휴게소 이미지 관리 템플릿을 반환한다")
    void restStopImages_returnsAdminRestStopImagesView() {
        assertThat(controller.restStopImages()).isEqualTo("admin-rest-stop-images");
    }

    @Test
    @DisplayName("GET /admin/rest-stops/edit는 휴게소 정보 관리 템플릿을 반환한다")
    void restStopEdit_returnsAdminRestStopEditView() {
        assertThat(controller.restStopEdit()).isEqualTo("admin-rest-stop-edit");
    }

    @Test
    @DisplayName("GET /admin/rest-stops/foods는 휴게소 음식 관리 템플릿을 반환한다")
    void restStopFoods_returnsAdminRestStopFoodsView() {
        assertThat(controller.restStopFoods()).isEqualTo("admin-rest-stop-foods");
    }

    @Test
    @DisplayName("GET /admin/rest-stops/oil-links는 휴게소 주유소 연결 관리 템플릿을 반환한다")
    void restStopOilLinks_returnsAdminRestStopOilLinksView() {
        assertThat(controller.restStopOilLinks()).isEqualTo("admin-rest-stop-oil-links");
    }
}
