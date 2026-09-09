package com.booka.rag;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AvailabilityIntentDetectorTest {

    private final AvailabilityIntentDetector detector = new AvailabilityIntentDetector();

    @Test
    void parsesDecemberDayRangeAsExactStay() {
        var parsed = detector.detect("available rooms for December 20 to 22", "");
        assertEquals(AvailabilityIntentDetector.Intent.AVAILABILITY, parsed.intent());
        assertFalse(parsed.sampleDates());
        assertNotNull(parsed.checkIn());
        assertNotNull(parsed.checkOut());
        assertEquals(20, parsed.checkIn().getDayOfMonth());
        assertEquals(12, parsed.checkIn().getMonthValue());
        assertEquals(22, parsed.checkOut().getDayOfMonth());
        assertEquals(12, parsed.checkOut().getMonthValue());
        assertEquals(parsed.checkIn().getYear(), parsed.checkOut().getYear());
    }

    @Test
    void parsesDashRange() {
        var parsed = detector.detect("rooms Dec 20-22 for 2 guests", "");
        assertEquals(AvailabilityIntentDetector.Intent.AVAILABILITY, parsed.intent());
        assertFalse(parsed.sampleDates());
        assertEquals(LocalDate.of(parsed.checkIn().getYear(), 12, 20), parsed.checkIn());
        assertEquals(LocalDate.of(parsed.checkOut().getYear(), 12, 22), parsed.checkOut());
    }

    @Test
    void monthOnlyStillUsesSampleStay() {
        var parsed = detector.detect("available rooms in December", "");
        assertEquals(AvailabilityIntentDetector.Intent.AVAILABILITY, parsed.intent());
        assertEquals(true, parsed.sampleDates());
    }
}
