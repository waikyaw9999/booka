package com.booka.domain.entity;

import com.booka.domain.enums.BookingStatus;
import com.booka.domain.enums.PaymentStatus;
import com.booka.domain.enums.ReservationChannel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "guest_id", nullable = false)
    private Long guestId;

    @Column(name = "confirmation_code", nullable = false, unique = true, length = 32)
    private String confirmationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReservationChannel channel;

    @Column(name = "external_reservation_id", length = 128)
    private String externalReservationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 32)
    private PaymentStatus paymentStatus;

    @Column(name = "stripe_checkout_session_id", length = 128)
    private String stripeCheckoutSessionId;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "num_guests", nullable = false)
    private Integer numGuests;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
