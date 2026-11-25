package com.thomascook.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.thomascook.hotelbooking.config.PricingProperties;
import com.thomascook.hotelbooking.dto.response.NightlyPriceLine;
import com.thomascook.hotelbooking.dto.response.PriceBreakdownResponse;
import com.thomascook.hotelbooking.service.PricingService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit tests: dates are fixed (Dec 2026: 3=Thu, 4=Fri, 5=Sat, 6=Sun, 7=Mon, 10=Thu). */
class PricingServiceTest {

    private static final BigDecimal PRICE = new BigDecimal("5000.00");
    private static final BigDecimal SURCHARGE = new BigDecimal("15");

    private PricingService pricing;

    @BeforeEach
    void setUp() {
        pricing = new PricingService(new PricingProperties(SURCHARGE, "INR"));
    }

    private static LocalDate dec(int day) {
        return LocalDate.of(2026, 12, day);
    }

    @Test
    void fixtureDatesHaveExpectedWeekdays() {
        assertThat(dec(3).getDayOfWeek()).isEqualTo(DayOfWeek.THURSDAY);
        assertThat(dec(4).getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(dec(5).getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
        assertThat(dec(7).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void multiNightWeekdayStayIsNightsTimesPriceWithNoSurcharge() {
        PriceBreakdownResponse price = pricing.calculate(PRICE, SURCHARGE, dec(7), dec(10)); // Mon, Tue, Wed nights

        assertThat(price.nights()).isEqualTo(3);
        assertThat(price.weekendNights()).isZero();
        assertThat(price.baseAmount()).isEqualByComparingTo("15000.00");
        assertThat(price.surchargeAmount()).isEqualByComparingTo("0.00");
        assertThat(price.totalAmount()).isEqualByComparingTo("15000.00");
        assertThat(price.currency()).isEqualTo("INR");
    }

    @Test
    void fridayAndSaturdayNightsGetFifteenPercentSurcharge() {
        PriceBreakdownResponse price = pricing.calculate(PRICE, SURCHARGE, dec(4), dec(6)); // Fri, Sat nights

        assertThat(price.nights()).isEqualTo(2);
        assertThat(price.weekendNights()).isEqualTo(2);
        assertThat(price.baseAmount()).isEqualByComparingTo("10000.00");
        assertThat(price.surchargeAmount()).isEqualByComparingTo("1500.00");
        assertThat(price.totalAmount()).isEqualByComparingTo("11500.00");
        assertThat(price.nightlyBreakdown()).allSatisfy(line -> {
            assertThat(line.weekendNight()).isTrue();
            assertThat(line.price()).isEqualByComparingTo("5750.00");
        });
    }

    @Test
    void mixedStayOnlySurchargesFridayAndSaturdayNights() {
        PriceBreakdownResponse price = pricing.calculate(PRICE, SURCHARGE, dec(3), dec(7)); // Thu, Fri, Sat, Sun nights

        assertThat(price.nights()).isEqualTo(4);
        assertThat(price.weekendNights()).isEqualTo(2);
        assertThat(price.totalAmount()).isEqualByComparingTo("21500.00");
        assertThat(price.nightlyBreakdown()).extracting(NightlyPriceLine::weekendNight)
                .containsExactly(false, true, true, false);
        assertThat(price.nightlyBreakdown()).extracting(NightlyPriceLine::dayOfWeek)
                .containsExactly(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    }

    @Test
    void checkoutDayIsNotCharged() {
        // Thursday night only: checking out on Friday morning must not pull in the Friday surcharge.
        PriceBreakdownResponse price = pricing.calculate(PRICE, SURCHARGE, dec(3), dec(4));

        assertThat(price.nights()).isEqualTo(1);
        assertThat(price.weekendNights()).isZero();
        assertThat(price.totalAmount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void singleSaturdayNightIsSurcharged() {
        PriceBreakdownResponse price = pricing.calculate(PRICE, SURCHARGE, dec(5), dec(6));

        assertThat(price.nights()).isEqualTo(1);
        assertThat(price.totalAmount()).isEqualByComparingTo("5750.00");
    }

    @Test
    void surchargeIsRoundedToTwoDecimalsHalfUp() {
        // 1234.56 * 15% = 185.184 -> 185.18
        PriceBreakdownResponse price = pricing.calculate(new BigDecimal("1234.56"), SURCHARGE, dec(4), dec(5));

        assertThat(price.surchargeAmount()).isEqualByComparingTo("185.18");
        assertThat(price.totalAmount()).isEqualByComparingTo("1419.74");
    }

    @Test
    void totalEqualsSumOfNightlyLines() {
        PriceBreakdownResponse price = pricing.calculate(new BigDecimal("3333.33"), SURCHARGE, dec(2), dec(9));

        BigDecimal sum = price.nightlyBreakdown().stream().map(NightlyPriceLine::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(price.nightlyBreakdown()).hasSize(price.nights());
        assertThat(price.totalAmount()).isEqualByComparingTo(sum);
        assertThat(price.totalAmount()).isEqualByComparingTo(price.baseAmount().add(price.surchargeAmount()));
    }

    @Test
    void zeroSurchargeConfigurationChargesBaseRateOnly() {
        PricingService noSurcharge = new PricingService(new PricingProperties(BigDecimal.ZERO, "INR"));

        PriceBreakdownResponse price = noSurcharge.calculate(PRICE, BigDecimal.ZERO, dec(4), dec(6));

        assertThat(price.totalAmount()).isEqualByComparingTo("10000.00");
    }

    @Test
    void rejectsEmptyOrReversedRange() {
        assertThatThrownBy(() -> pricing.calculate(PRICE, SURCHARGE, dec(5), dec(5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pricing.calculate(PRICE, SURCHARGE, dec(6), dec(5)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
