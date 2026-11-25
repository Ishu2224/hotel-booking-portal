package com.thomascook.hotelbooking.controller;

import com.thomascook.hotelbooking.dto.request.BookingLookupRequest;
import com.thomascook.hotelbooking.dto.request.CreateBookingRequest;
import com.thomascook.hotelbooking.dto.response.BookingResponse;
import com.thomascook.hotelbooking.service.BookingService;
import com.thomascook.hotelbooking.service.VoucherService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final VoucherService voucherService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.createBooking(request);
    }

    /** POST (not GET) so the guest's email travels in the body instead of the URL / access logs. */
    @PostMapping("/lookup")
    public BookingResponse lookup(@Valid @RequestBody BookingLookupRequest request) {
        return bookingService.lookup(request);
    }

    @PostMapping("/cancel")
    public BookingResponse cancel(@Valid @RequestBody BookingLookupRequest request) {
        return bookingService.cancel(request);
    }

    /** Downloadable, printable HTML voucher. Requires the same reference + email proof as lookup. */
    @PostMapping("/voucher")
    public ResponseEntity<String> voucher(@Valid @RequestBody BookingLookupRequest request) {
        BookingResponse booking = bookingService.lookup(request);
        String filename = "voucher-" + booking.bookingReference() + ".html";
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(voucherService.render(booking));
    }
}
