package com.thomascook.hotelbooking.repository;

import com.thomascook.hotelbooking.entity.Booking;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByBookingReference(String bookingReference);

    @EntityGraph(attributePaths = "hotel")
    Optional<Booking> findByBookingReferenceAndGuestEmailIgnoreCase(String bookingReference, String guestEmail);

    /**
     * CONFIRMED bookings overlapping [checkIn, checkOut) for one hotel.
     * Half-open overlap rule: existing.checkIn < requested.checkOut AND existing.checkOut > requested.checkIn,
     * so back-to-back stays (checkout day == next check-in day) do NOT overlap.
     */
    @Query("""
            select count(b) from Booking b
            where b.hotel.id = :hotelId
              and b.status = com.thomascook.hotelbooking.entity.BookingStatus.CONFIRMED
              and b.checkInDate < :checkOut
              and b.checkOutDate > :checkIn
            """)
    long countOverlappingConfirmed(@Param("hotelId") Long hotelId,
                                   @Param("checkIn") LocalDate checkIn,
                                   @Param("checkOut") LocalDate checkOut);

    /** Same rule as above for all hotels at once (avoids N+1 queries on the search page). */
    @Query("""
            select b.hotel.id as hotelId, count(b) as bookedRooms
            from Booking b
            where b.status = com.thomascook.hotelbooking.entity.BookingStatus.CONFIRMED
              and b.checkInDate < :checkOut
              and b.checkOutDate > :checkIn
            group by b.hotel.id
            """)
    List<HotelBookedCount> countOverlappingConfirmedByHotel(@Param("checkIn") LocalDate checkIn,
                                                            @Param("checkOut") LocalDate checkOut);
}
