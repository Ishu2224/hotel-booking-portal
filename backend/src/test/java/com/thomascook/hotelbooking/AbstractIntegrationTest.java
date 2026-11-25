package com.thomascook.hotelbooking;

import com.thomascook.hotelbooking.repository.BookingRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the full application on in-memory H2 (MySQL mode) using the real Flyway migrations and seed data.
 * Seeded hotel 6 (Lake Pichola Residency) has exactly 3 rooms and costs 12,500 per night.
 * Tests are intentionally NOT @Transactional (the concurrency test needs real, separately committed transactions),
 * so bookings are wiped after every test instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    static final long THREE_ROOM_HOTEL_ID = 6L;

    @Autowired
    protected BookingRepository bookingRepository;

    @AfterEach
    void cleanBookings() {
        bookingRepository.deleteAll();
    }
}
