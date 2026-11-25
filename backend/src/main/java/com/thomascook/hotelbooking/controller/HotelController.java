package com.thomascook.hotelbooking.controller;

import com.thomascook.hotelbooking.dto.request.HotelSearchRequest;
import com.thomascook.hotelbooking.dto.request.StayRequest;
import com.thomascook.hotelbooking.dto.response.HotelResponse;
import com.thomascook.hotelbooking.dto.response.PriceBreakdownResponse;
import com.thomascook.hotelbooking.service.HotelService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    /** GET /api/hotels?location=&checkInDate=yyyy-MM-dd&checkOutDate=yyyy-MM-dd (all optional; dates as a pair). */
    @GetMapping
    public List<HotelResponse> search(@Valid HotelSearchRequest request) {
        return hotelService.search(request);
    }

    @GetMapping("/{id}")
    public HotelResponse getHotel(@PathVariable Long id) {
        return hotelService.getHotel(id);
    }

    /** Hotel details plus rooms left for the requested stay. */
    @GetMapping("/{id}/availability")
    public HotelResponse getAvailability(@PathVariable Long id, @Valid StayRequest stay) {
        return hotelService.getHotelWithAvailability(id, stay);
    }

    /** Night-by-night price preview shown before the guest confirms. */
    @GetMapping("/{id}/quote")
    public PriceBreakdownResponse getQuote(@PathVariable Long id, @Valid StayRequest stay) {
        return hotelService.quote(id, stay);
    }
}
