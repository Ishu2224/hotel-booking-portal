package com.thomascook.hotelbooking.dto.validation;

import com.thomascook.hotelbooking.config.AppTime;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class StayDatesValidator implements ConstraintValidator<ValidStayDates, StayDates> {

    public static final int MAX_STAY_NIGHTS = 30;

    private boolean datesOptional;

    @Override
    public void initialize(ValidStayDates annotation) {
        this.datesOptional = annotation.datesOptional();
    }

    @Override
    public boolean isValid(StayDates stay, ConstraintValidatorContext context) {
        if (stay == null) {
            return true;
        }
        LocalDate checkIn = stay.checkInDate();
        LocalDate checkOut = stay.checkOutDate();

        context.disableDefaultConstraintViolation();

        if (datesOptional && (checkIn == null) != (checkOut == null)) {
            addViolation(context, checkIn == null ? "checkInDate" : "checkOutDate",
                    "Check-in and check-out dates must be provided together");
            return false;
        }

        boolean valid = true;
        if (checkIn != null && checkIn.isBefore(AppTime.today())) {
            addViolation(context, "checkInDate", "Check-in date must be today or a future date");
            valid = false;
        }
        if (checkIn != null && checkOut != null) {
            if (!checkOut.isAfter(checkIn)) {
                addViolation(context, "checkOutDate", "Check-out date must be after the check-in date");
                valid = false;
            } else if (checkOut.isAfter(checkIn.plusDays(MAX_STAY_NIGHTS))) {
                addViolation(context, "checkOutDate", "Stays are limited to " + MAX_STAY_NIGHTS + " nights");
                valid = false;
            }
        }
        return valid;
    }

    private static void addViolation(ConstraintValidatorContext context, String property, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(property)
                .addConstraintViolation();
    }
}
