package com.thomascook.hotelbooking.repository;

import com.thomascook.hotelbooking.entity.Hotel;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    /** Case-insensitive "contains" search; an empty string matches every hotel. */
    List<Hotel> findByLocationContainingIgnoreCaseOrderByNameAsc(String location);

    /**
     * Loads the hotel with SELECT ... FOR UPDATE. Must be called inside a transaction: concurrent booking
     * attempts for the same hotel queue on this row, so the availability check + insert is serialized.
     * How long a waiter blocks is bounded by MySQL's innodb_lock_wait_timeout (default 50s); on expiry Spring raises
     * a PessimisticLockingFailureException, which the global handler maps to 503.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hotel h where h.id = :id")
    Optional<Hotel> findByIdForUpdate(@Param("id") Long id);
}
