package com.thomascook.hotelbooking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Backend-computed price: base amount (nights x price per night) + weekend surcharge = total.
 * Also returned inside every booking so the voucher can show the exact same numbers.
 */
public record PriceBreakdownResponse(
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int nights,
        BigDecimal pricePerNight,
        int weekendNights,
        BigDecimal weekendSurchargePercent,
        BigDecimal baseAmount,
        BigDecimal surchargeAmount,
        BigDecimal totalAmount,
        String currency,
        List<NightlyPriceLine> nightlyBreakdown) {
}
