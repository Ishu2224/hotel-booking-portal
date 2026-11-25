package com.thomascook.hotelbooking.service;

import com.thomascook.hotelbooking.config.PricingProperties;
import com.thomascook.hotelbooking.dto.response.NightlyPriceLine;
import com.thomascook.hotelbooking.dto.response.PriceBreakdownResponse;
import com.thomascook.hotelbooking.entity.Booking;
import com.thomascook.hotelbooking.entity.Hotel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * The only place prices are computed. Rules:
 * <ul>
 *   <li>A stay [checkIn, checkOut) is charged one night per day from check-in up to, but excluding, check-out.</li>
 *   <li>Friday and Saturday nights carry a surcharge (default 15%) on that night's base price.</li>
 *   <li>The surcharge of each night is rounded to 2 decimals (HALF_UP); the total is the sum of the nightly prices.</li>
 * </ul>
 */
@Service
public class PricingService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PricingProperties properties;

    public PricingService(PricingProperties properties) {
        this.properties = properties;
    }

    /** Price for a new stay using the hotel's current rate and the configured surcharge. */
    public PriceBreakdownResponse quote(Hotel hotel, LocalDate checkIn, LocalDate checkOut) {
        return calculate(hotel.getPricePerNight(), properties.weekendSurchargePercent(), checkIn, checkOut);
    }

    /** Re-derives the breakdown of an existing booking from the price snapshot stored on it. */
    public PriceBreakdownResponse forBooking(Booking booking) {
        return calculate(booking.getPricePerNight(), booking.getWeekendSurchargePercent(),
                booking.getCheckInDate(), booking.getCheckOutDate());
    }

    public PriceBreakdownResponse calculate(BigDecimal pricePerNight, BigDecimal surchargePercent,
                                            LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("checkOut must be after checkIn");
        }
        BigDecimal basePrice = pricePerNight.setScale(2, RoundingMode.HALF_UP);
        BigDecimal percent = surchargePercent.setScale(2, RoundingMode.HALF_UP);
        BigDecimal weekendSurcharge = basePrice.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);

        List<NightlyPriceLine> lines = new ArrayList<>();
        BigDecimal baseTotal = BigDecimal.ZERO.setScale(2);
        BigDecimal surchargeTotal = BigDecimal.ZERO.setScale(2);
        int weekendNights = 0;

        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            boolean weekend = isWeekendNight(night);
            BigDecimal surcharge = weekend ? weekendSurcharge : BigDecimal.ZERO.setScale(2);
            lines.add(new NightlyPriceLine(night, night.getDayOfWeek(), weekend, basePrice, surcharge,
                    basePrice.add(surcharge)));
            baseTotal = baseTotal.add(basePrice);
            surchargeTotal = surchargeTotal.add(surcharge);
            if (weekend) {
                weekendNights++;
            }
        }

        return new PriceBreakdownResponse(
                checkIn, checkOut, lines.size(), basePrice, weekendNights, percent,
                baseTotal, surchargeTotal, baseTotal.add(surchargeTotal), properties.currency(),
                List.copyOf(lines));
    }

    /** The night that starts on a Friday or Saturday. */
    public static boolean isWeekendNight(LocalDate night) {
        DayOfWeek day = night.getDayOfWeek();
        return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY;
    }
}
