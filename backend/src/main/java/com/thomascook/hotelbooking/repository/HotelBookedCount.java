package com.thomascook.hotelbooking.repository;

/** Projection: number of overlapping CONFIRMED bookings per hotel for a requested date range. */
public interface HotelBookedCount {

    Long getHotelId();

    Long getBookedRooms();
}
