package com.thomascook.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;

import com.thomascook.hotelbooking.config.AppTime;
import com.thomascook.hotelbooking.dto.request.CreateBookingRequest;
import com.thomascook.hotelbooking.dto.request.HotelSearchRequest;
import com.thomascook.hotelbooking.dto.request.StayRequest;
import com.thomascook.hotelbooking.dto.validation.StayDatesValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StayDatesValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final LocalDate today = AppTime.today();

    private CreateBookingRequest booking(LocalDate in, LocalDate out) {
        return new CreateBookingRequest(1L, in, out, "Asha Rao", "asha@example.com");
    }

    private static Set<String> invalidFields(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void validFutureStayHasNoViolations() {
        assertThat(validator.validate(booking(today.plusDays(3), today.plusDays(5)))).isEmpty();
    }

    @Test
    void checkInTodayIsAllowed() {
        assertThat(validator.validate(booking(today, today.plusDays(1)))).isEmpty();
    }

    @Test
    void pastCheckInIsRejectedOnCheckInDate() {
        var violations = validator.validate(booking(today.minusDays(1), today.plusDays(2)));

        assertThat(invalidFields(violations)).containsExactly("checkInDate");
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactly("Check-in date must be today or a future date");
    }

    @Test
    void checkoutBeforeCheckInIsRejectedOnCheckOutDate() {
        var violations = validator.validate(booking(today.plusDays(5), today.plusDays(3)));

        assertThat(invalidFields(violations)).containsExactly("checkOutDate");
        assertThat(violations).extracting(ConstraintViolation::getMessage)
                .containsExactly("Check-out date must be after the check-in date");
    }

    @Test
    void checkoutOnSameDayAsCheckInIsRejected() {
        var violations = validator.validate(booking(today.plusDays(3), today.plusDays(3)));

        assertThat(invalidFields(violations)).containsExactly("checkOutDate");
    }

    @Test
    void stayLongerThanMaximumIsRejected() {
        LocalDate in = today.plusDays(2);

        assertThat(validator.validate(booking(in, in.plusDays(StayDatesValidator.MAX_STAY_NIGHTS)))).isEmpty();
        assertThat(invalidFields(validator.validate(booking(in, in.plusDays(StayDatesValidator.MAX_STAY_NIGHTS + 1)))))
                .containsExactly("checkOutDate");
    }

    @Test
    void missingDatesAreReportedByNotNullNotByTheStayRule() {
        var violations = validator.validate(booking(null, null));

        assertThat(invalidFields(violations)).containsExactlyInAnyOrder("checkInDate", "checkOutDate");
    }

    @Test
    void guestDetailsAreValidated() {
        var request = new CreateBookingRequest(1L, today.plusDays(1), today.plusDays(2), " ", "not-an-email");

        assertThat(invalidFields(validator.validate(request))).containsExactlyInAnyOrder("guestName", "guestEmail");
    }

    @Test
    void searchAllowsNoDatesAtAll() {
        assertThat(validator.validate(new HotelSearchRequest("Goa", null, null))).isEmpty();
    }

    @Test
    void searchRejectsASingleDate() {
        var onlyCheckIn = validator.validate(new HotelSearchRequest(null, today.plusDays(2), null));
        var onlyCheckOut = validator.validate(new HotelSearchRequest(null, null, today.plusDays(2)));

        assertThat(invalidFields(onlyCheckIn)).containsExactly("checkOutDate");
        assertThat(invalidFields(onlyCheckOut)).containsExactly("checkInDate");
    }

    @Test
    void searchAppliesTheSameDateRules() {
        var violations = validator.validate(new HotelSearchRequest("Goa", today.minusDays(1), today.minusDays(3)));

        assertThat(invalidFields(violations)).containsExactlyInAnyOrder("checkInDate", "checkOutDate");
    }

    @Test
    void quoteRequestRequiresBothDates() {
        assertThat(invalidFields(validator.validate(new StayRequest(null, null))))
                .containsExactlyInAnyOrder("checkInDate", "checkOutDate");
    }
}
