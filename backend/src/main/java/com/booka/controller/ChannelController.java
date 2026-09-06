package com.booka.controller;

import com.booka.dto.BookingDtos.BookingResponse;
import com.booka.dto.BookingDtos.ChannelReservationRequest;
import com.booka.service.ChannelReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelReservationService channelReservationService;

    @PostMapping("/{channel}/reservations")
    BookingResponse upsertReservation(
            @PathVariable String channel,
            @RequestHeader(value = "X-Channel-Api-Key", required = false) String apiKey,
            @Valid @RequestBody ChannelReservationRequest request) {
        channelReservationService.assertValidApiKey(channel, apiKey);
        return channelReservationService.upsert(channel, request);
    }
}
