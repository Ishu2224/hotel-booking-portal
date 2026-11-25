package com.thomascook.hotelbooking.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level rule for a stay interval:
 * check-in must be today or later, check-out strictly after check-in, and the stay at most
 * {@link StayDatesValidator#MAX_STAY_NIGHTS} nights. Null handling is configurable (see {@link #datesOptional()}).
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StayDatesValidator.class)
public @interface ValidStayDates {

    String message() default "Invalid stay dates";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * false (default): missing dates are left to @NotNull on the fields.
     * true: both dates may be omitted (search page), but not just one of them.
     */
    boolean datesOptional() default false;
}
