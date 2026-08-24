package com.restroute.oilprice.domain;

import com.restroute.oilprice.client.response.OpinetAverageOilPriceItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "national_oil_price",
        indexes = {@Index(name = "idx_national_oil_price_trade_date", columnList = "trade_date")},
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_national_oil_price_trade_date_product_code",
                    columnNames = {"trade_date", "product_code"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NationalOilPriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    private String productName;
    private String price;
    private String diff;

    private NationalOilPriceEntity(OpinetAverageOilPriceItem item) {
        this.tradeDate = LocalDate.parse(item.getTradeDate(), DateTimeFormatter.BASIC_ISO_DATE);
        this.productCode = item.getProductCode();
        this.productName = item.getProductName();
        this.price = item.getPrice();
        this.diff = item.getDiff();
    }

    public static NationalOilPriceEntity from(OpinetAverageOilPriceItem item) {
        return new NationalOilPriceEntity(item);
    }

    public int roundedPrice() {
        return new BigDecimal(price).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    public String formattedPrice() {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(roundedPrice()) + "원";
    }
}
