package com.thomascook.hotelbooking;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.thomascook.hotelbooking.config.AppTime;
import java.time.DayOfWeek;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end API tests: HTTP contract, validation messages, error format and the full booking lifecycle. */
class BookingApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final LocalDate monday = TestDates.next(DayOfWeek.MONDAY);
    private final LocalDate friday = TestDates.next(DayOfWeek.FRIDAY);

    private static String bookingJson(long hotelId, LocalDate in, LocalDate out, String name, String email) {
        return """
                {"hotelId": %d, "checkInDate": "%s", "checkOutDate": "%s", "guestName": "%s", "guestEmail": "%s"}
                """.formatted(hotelId, in, out, name, email);
    }

    private static String lookupJson(String reference, String email) {
        return """
                {"bookingReference": "%s", "guestEmail": "%s"}
                """.formatted(reference, email);
    }

    private String createBooking(long hotelId, LocalDate in, LocalDate out, String email) throws Exception {
        String body = mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(hotelId, in, out, "Asha Rao", email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.bookingReference");
    }

    // ---- browse & search ----

    @Test
    void listsSeededHotelsWithTheFieldsTheUiNeeds() throws Exception {
        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(6))))
                .andExpect(jsonPath("$[0].name", notNullValue()))
                .andExpect(jsonPath("$[0].location", notNullValue()))
                .andExpect(jsonPath("$[0].description", notNullValue()))
                .andExpect(jsonPath("$[0].amenities", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].pricePerNight", notNullValue()))
                .andExpect(jsonPath("$[0].availableRooms").doesNotExist());
    }

    @Test
    void filtersByLocationIgnoringCase() throws Exception {
        mockMvc.perform(get("/api/hotels").param("location", "mUMbai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Marine Drive Grand")));
    }

    @Test
    void searchWithDatesReportsRoomsLeftAndSoldOut() throws Exception {
        LocalDate out = monday.plusDays(3);
        for (int i = 0; i < 3; i++) {
            createBooking(THREE_ROOM_HOTEL_ID, monday, out, "guest" + i + "@example.com");
        }

        mockMvc.perform(get("/api/hotels").param("location", "udaipur")
                        .param("checkInDate", monday.toString()).param("checkOutDate", out.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].availableRooms", is(0)));
        mockMvc.perform(get("/api/hotels").param("location", "mumbai")
                        .param("checkInDate", monday.toString()).param("checkOutDate", out.toString()))
                .andExpect(jsonPath("$[0].availableRooms", is(20)));
    }

    @Test
    void searchWithOnlyOneDateIsRejected() throws Exception {
        mockMvc.perform(get("/api/hotels").param("checkInDate", monday.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("checkOutDate")));
    }

    @Test
    void unknownHotelIs404AndBadIdIs400() throws Exception {
        mockMvc.perform(get("/api/hotels/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
        mockMvc.perform(get("/api/hotels/abc")).andExpect(status().isBadRequest());
    }

    // ---- pricing ----

    @Test
    void quoteForWeekdayStayIsNightsTimesRate() throws Exception {
        mockMvc.perform(get("/api/hotels/6/quote")
                        .param("checkInDate", monday.toString()).param("checkOutDate", monday.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nights", is(3)))
                .andExpect(jsonPath("$.weekendNights", is(0)))
                .andExpect(jsonPath("$.totalAmount", is(37500.0)))
                .andExpect(jsonPath("$.nightlyBreakdown", hasSize(3)));
    }

    @Test
    void quoteForWeekendStayAddsFifteenPercentOnFridayAndSaturday() throws Exception {
        mockMvc.perform(get("/api/hotels/6/quote")
                        .param("checkInDate", friday.toString()).param("checkOutDate", friday.plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekendNights", is(2)))
                .andExpect(jsonPath("$.baseAmount", is(25000.0)))
                .andExpect(jsonPath("$.surchargeAmount", is(3750.0)))
                .andExpect(jsonPath("$.totalAmount", is(28750.0)))
                .andExpect(jsonPath("$.nightlyBreakdown[0].dayOfWeek", is("FRIDAY")))
                .andExpect(jsonPath("$.nightlyBreakdown[0].weekendNight", is(true)))
                .andExpect(jsonPath("$.nightlyBreakdown[0].price", is(14375.0)));
    }

    @Test
    void backendIgnoresAnyClientSuppliedPrice() throws Exception {
        String body = """
                {"hotelId": 6, "checkInDate": "%s", "checkOutDate": "%s", "guestName": "Asha", "guestEmail": "asha@example.com",
                 "totalAmount": 1, "pricePerNight": 1}
                """.formatted(monday, monday.plusDays(1));

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price.totalAmount", is(12500.0)));
    }

    // ---- validation & error format ----

    @Test
    void pastCheckInReturnsStructuredFieldError() throws Exception {
        LocalDate yesterday = AppTime.today().minusDays(1);

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(6, yesterday, yesterday.plusDays(2), "Asha", "asha@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Check-in date must be today or a future date")))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("checkInDate")));
    }

    @Test
    void checkoutBeforeCheckInIsRejected() throws Exception {
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(6, monday, monday.minusDays(1), "Asha", "asha@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("checkOutDate")))
                .andExpect(jsonPath("$.fieldErrors[0].message", is("Check-out date must be after the check-in date")));
    }

    @Test
    void multipleInvalidFieldsAreAllReported() throws Exception {
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(6, monday, monday.plusDays(1), " ", "nope")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Validation failed")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("guestName")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("guestEmail")));
    }

    @Test
    void malformedDateInBodyIsAFieldError() throws Exception {
        String body = """
                {"hotelId": 6, "checkInDate": "31-12-2030", "checkOutDate": "2030-12-31", "guestName": "A", "guestEmail": "a@b.com"}
                """;

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("checkInDate")))
                .andExpect(jsonPath("$.fieldErrors[0].message", containsString("yyyy-MM-dd")));
    }

    @Test
    void brokenJsonIs400() throws Exception {
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    // ---- booking lifecycle ----

    @Test
    void fullLifecycleBookLookupCancelAndRelease() throws Exception {
        LocalDate out = monday.plusDays(3);
        String reference = createBooking(THREE_ROOM_HOTEL_ID, monday, out, "Asha@Example.com");
        assertThat(reference).matches("TC-\\d{4}-[A-HJKMNP-Z2-9]{6}");

        // lookup works with the right email (any letter case) ...
        mockMvc.perform(post("/api/bookings/lookup").contentType(MediaType.APPLICATION_JSON)
                        .content(lookupJson(reference.toLowerCase(), "ASHA@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.hotelName", is("Lake Pichola Residency")))
                .andExpect(jsonPath("$.price.totalAmount", is(37500.0)));
        // ... and not with a wrong one
        mockMvc.perform(post("/api/bookings/lookup").contentType(MediaType.APPLICATION_JSON)
                        .content(lookupJson(reference, "someone.else@example.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("No booking found for that reference and email address.")));

        // booking took one of the 3 rooms
        mockMvc.perform(get("/api/hotels/6/availability")
                        .param("checkInDate", monday.toString()).param("checkOutDate", out.toString()))
                .andExpect(jsonPath("$.availableRooms", is(2)));

        // cancel -> room released
        mockMvc.perform(post("/api/bookings/cancel").contentType(MediaType.APPLICATION_JSON)
                        .content(lookupJson(reference, "asha@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.cancellable", is(false)))
                .andExpect(jsonPath("$.cancelledAt", notNullValue()));
        mockMvc.perform(get("/api/hotels/6/availability")
                        .param("checkInDate", monday.toString()).param("checkOutDate", out.toString()))
                .andExpect(jsonPath("$.availableRooms", is(3)));

        // cancelling again is a conflict
        mockMvc.perform(post("/api/bookings/cancel").contentType(MediaType.APPLICATION_JSON)
                        .content(lookupJson(reference, "asha@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("Conflict")));
    }

    @Test
    void bookingBeyondCapacityReturns409() throws Exception {
        LocalDate out = monday.plusDays(2);
        for (int i = 0; i < 3; i++) {
            createBooking(THREE_ROOM_HOTEL_ID, monday, out, "guest" + i + "@example.com");
        }

        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(THREE_ROOM_HOTEL_ID, monday, out, "Late Larry", "larry@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("no rooms are available")));
    }

    // ---- voucher ----

    @Test
    void voucherIsADownloadableHtmlDocumentWithTheNightlyBreakdown() throws Exception {
        String reference = createBooking(THREE_ROOM_HOTEL_ID, friday, friday.plusDays(2), "asha@example.com");

        mockMvc.perform(post("/api/bookings/voucher").contentType(MediaType.APPLICATION_JSON)
                        .content(lookupJson(reference, "asha@example.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("voucher-" + reference + ".html")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString(reference)))
                .andExpect(content().string(containsString("Lake Pichola Residency")))
                .andExpect(content().string(containsString("Weekend surcharge (15%")))
                .andExpect(content().string(containsString("28,750.00")));
    }

    @Test
    void voucherRequiresTheMatchingEmail() throws Exception {
        String reference = createBooking(THREE_ROOM_HOTEL_ID, monday, monday.plusDays(1), "asha@example.com");

        mockMvc.perform(post("/api/bookings/voucher").contentType(MediaType.APPLICATION_JSON)
                        .content(lookupJson(reference, "intruder@example.com")))
                .andExpect(status().isNotFound());
    }
}
