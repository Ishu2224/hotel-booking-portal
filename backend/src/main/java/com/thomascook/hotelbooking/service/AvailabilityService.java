package com.thomascook.hotelbooking.service;

import com.thomascook.hotelbooking.entity.Hotel;
import com.thomascook.hotelbooking.repository.BookingRepository;
import com.thomascook.hotelbooking.repository.HotelBookedCount;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Available rooms = hotel.totalRooms - number of CONFIRMED bookings overlapping [checkIn, checkOut).
 * Cancelled bookings are never counted, so cancelling releases the room immediately.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final BookingRepository bookingRepository;

    public int availableRooms(Hotel hotel, LocalDate checkIn, LocalDate checkOut) {
        long booked = bookingRepository.countOverlappingConfirmed(hotel.getId(), checkIn, checkOut);
        return remaining(hotel.getTotalRooms(), booked);
    }

    /** hotelId -> overlapping CONFIRMED bookings, for every hotel that has at least one. */
    public Map<Long, Long> bookedRoomsByHotel(LocalDate checkIn, LocalDate checkOut) {
        Map<Long, Long> booked = new HashMap<>();
        for (HotelBookedCount row : bookingRepository.countOverlappingConfirmedByHotel(checkIn, checkOut)) {
            booked.put(row.getHotelId(), row.getBookedRooms());
        }
        return booked;
    }

    public static int remaining(int totalRooms, long bookedRooms) {
        return (int) Math.max(0, totalRooms - bookedRooms);
    }
}
