package com.restroute.domain;

import com.restroute.service.salesranking.dto.SalesRankingProductRow;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@Entity
@Table(
        name = "rest_stop_product_sales_rank",
        indexes = {
            @Index(name = "idx_product_sales_rank_service_area", columnList = "rest_stop_service_area_code"),
            @Index(name = "idx_product_sales_rank_month", columnList = "base_year_month")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestStopProductSalesRankEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String baseYearMonth;
    private String restStopRank;
    private String sourceRestStopCode;
    private String sourceRestStopName;
    private String sourceStoreCode;
    private String sourceStoreName;
    private String productSequence;
    private String productName;
    private String restStopServiceAreaCode;

    private RestStopProductSalesRankEntity(SalesRankingProductRow row) {
        updateFrom(row);
        this.restStopServiceAreaCode = "";
    }

    public static RestStopProductSalesRankEntity from(SalesRankingProductRow row) {
        return new RestStopProductSalesRankEntity(row);
    }

    public void updateFrom(SalesRankingProductRow row) {
        this.baseYearMonth = row.baseYearMonth();
        this.restStopRank = row.restStopRank();
        this.sourceRestStopCode = row.sourceRestStopCode();
        this.sourceRestStopName = row.sourceRestStopName();
        this.sourceStoreCode = row.sourceStoreCode();
        this.sourceStoreName = row.sourceStoreName();
        this.productSequence = row.productSequence();
        this.productName = row.productName();
    }

    public boolean isUnmapped() {
        return !StringUtils.hasText(restStopServiceAreaCode);
    }

    public void updateRestStopServiceAreaCode(String serviceAreaCode) {
        this.restStopServiceAreaCode = serviceAreaCode;
    }
}
