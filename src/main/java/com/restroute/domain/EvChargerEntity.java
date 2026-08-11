package com.restroute.domain;

import com.restroute.client.response.EvChargerItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@Entity
@Table(
        name = "ev_charger",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_ev_charger_stat_id_chger_id",
                        columnNames = {"stat_id", "chger_id"}),
        indexes = @Index(name = "idx_ev_charger_stat_id", columnList = "stat_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvChargerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String statNm;
    private String statId;
    private String chgerId;
    private String chgerType;
    private String addr;
    private String addrDetail;
    private String location;
    private String useTime;
    private String lat;
    private String lng;
    private String busiId;
    private String bnm;
    private String busiNm;
    private String busiCall;
    private String stat;
    private String statUpdDt;
    private String lastTsdt;
    private String lastTedt;
    private String nowTsdt;
    private String powerType;
    private String output;
    private String method;
    private String zcode;
    private String zscode;
    private String kind;
    private String kindDetail;
    private String parkingFree;
    private String note;
    private String limitYn;
    private String limitDetail;
    private String delYn;
    private String delDetail;
    private String trafficYn;

    @Column(name = "\"year\"")
    private String year;

    private String floorNum;
    private String floorType;
    private String maker;

    private EvChargerEntity(EvChargerItem item) {
        apply(item);
    }

    public static EvChargerEntity from(EvChargerItem item) {
        return new EvChargerEntity(item);
    }

    public void updateFrom(EvChargerItem item) {
        apply(item);
    }

    private void apply(EvChargerItem item) {
        this.statNm = item.getStatNm();
        this.statId = item.getStatId();
        this.chgerId = item.getChgerId();
        this.chgerType = item.getChgerType();
        this.addr = item.getAddr();
        this.addrDetail = item.getAddrDetail();
        this.location = item.getLocation();
        this.useTime = item.getUseTime();
        this.lat = item.getLat();
        this.lng = item.getLng();
        this.busiId = item.getBusiId();
        this.bnm = item.getBnm();
        this.busiNm = item.getBusiNm();
        this.busiCall = item.getBusiCall();
        this.stat = item.getStat();
        this.statUpdDt = item.getStatUpdDt();
        this.lastTsdt = item.getLastTsdt();
        this.lastTedt = item.getLastTedt();
        this.nowTsdt = item.getNowTsdt();
        this.powerType = item.getPowerType();
        this.output = item.getOutput();
        this.method = item.getMethod();
        this.zcode = item.getZcode();
        this.zscode = item.getZscode();
        this.kind = item.getKind();
        this.kindDetail = item.getKindDetail();
        this.parkingFree = item.getParkingFree();
        this.note = item.getNote();
        this.limitYn = item.getLimitYn();
        this.limitDetail = item.getLimitDetail();
        this.delYn = item.getDelYn();
        this.delDetail = item.getDelDetail();
        this.trafficYn = item.getTrafficYn();
        this.year = item.getYear();
        this.floorNum = item.getFloorNum();
        this.floorType = item.getFloorType();
        this.maker = item.getMaker();
    }

    public boolean canBeMappedToRestStop() {
        return StringUtils.hasText(statId) && "N".equals(delYn);
    }

    public boolean hasSameStationId(EvChargerEntity other) {
        return statId.equals(other.statId);
    }
}
