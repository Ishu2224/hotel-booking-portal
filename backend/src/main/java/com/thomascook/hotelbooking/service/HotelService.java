package com.thomascook.hotelbooking.service;

import com.thomascook.hotelbooking.dto.request.HotelSearchRequest;
import com.thomascook.hotelbooking.dto.request.StayRequest;
import com.thomascook.hotelbooking.dto.response.HotelResponse;
import com.thomascook.hotelbooking.dto.response.PriceBreakdownResponse;
import com.thomascook.hotelbooking.entity.Hotel;
import com.thomascook.hotelbooking.exception.ResourceNotFoundException;
import com.thomascook.hotelbooking.repository.HotelRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelService {

    private final HotelRepository hotelRepository;
    private final AvailabilityService availabilityService;
    private final PricingService pricingService;

    /** Hotels matching the (optional) location text; with dates, each result also carries its available rooms. */
    public List<HotelResponse> search(HotelSearchRequest request) {
        String location = request.location() == null ? "" : request.location().trim();
        List<Hotel> hotels = hotelRepository.findByLocationContainingIgnoreCaseOrderByNameAsc(location);

        if (request.checkInDate() == null) {
            return hotels.stream().map(h -> HotelResponse.from(h, null)).toList();
        }
        Map<Long, Long> booked = availabilityService.bookedRoomsByHotel(request.checkInDate(), request.checkOutDate());
        return hotels.stream()
                .map(h -> HotelResponse.from(h,
                        AvailabilityService.remaining(h.getTotalRooms(), booked.getOrDefault(h.getId(), 0L))))
                .toList();
    }

    public HotelResponse getHotel(Long id) {
        return HotelResponse.from(findHotel(id), null);
    }

    public HotelResponse getHotelWithAvailability(Long id, StayRequest stay) {
        Hotel hotel = findHotel(id);
        int available = availabilityService.availableRooms(hotel, stay.checkInDate(), stay.checkOutDate());
        return HotelResponse.from(hotel, available);
    }

    public PriceBreakdownResponse quote(Long id, StayRequest stay) {
        return pricingService.quote(findHotel(id), stay.checkInDate(), stay.checkOutDate());
    }

    private Hotel findHotel(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found."));
    }
}
