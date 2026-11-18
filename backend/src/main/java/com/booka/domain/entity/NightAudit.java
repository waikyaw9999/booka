package com.booka.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "night_audits",
        uniqueConstraints = @UniqueConstraint(name = "uq_night_audits_hotel_date", columnNames = {"hotel_id", "business_date"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NightAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "occupancy_percent", nullable = false, precision = 5, scale = 1)
    private BigDecimal occupancyPercent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal adr;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal revpar;

    @Column(name = "room_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal roomRevenue;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal payments;

    @Column(name = "open_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal openBalance;

    @Column(name = "rooms_sold", nullable = false)
    private int roomsSold;

    @Column(name = "sellable_rooms", nullable = false)
    private int sellableRooms;

    @Column(name = "in_house", nullable = false)
    private int inHouse;

    @Column(nullable = false)
    private int arrivals;

    @Column(nullable = false)
    private int departures;

    @Column(name = "no_shows", nullable = false)
    private int noShows;

    @Column(length = 500)
    private String note;

    @Column(name = "closed_at", nullable = false)
    private Instant closedAt;
}
