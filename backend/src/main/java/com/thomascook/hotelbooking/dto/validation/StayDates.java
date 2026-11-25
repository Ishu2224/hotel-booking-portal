package com.thomascook.hotelbooking.dto.validation;

import java.time.LocalDate;

/** Implemented by request records carrying a stay interval so one validator can check them all. */
public interface StayDates {

    LocalDate checkInDate();

    LocalDate checkOutDate();
}
