package com.thomascook.hotelbooking.dto.response;

import com.thomascook.hotelbooking.entity.Hotel;
import java.math.BigDecimal;
import java.util.List;

/**
 * Hotel card/detail. {@code availableRooms} is null when no dates were requested,
 * otherwise rooms left for that stay (never negative).
 */
public record HotelResponse(
        Long id,
        String name,
        String location,
        String description,
        String roomType,
        int maxGuestsPerRoom,
        List<String> amenities,
        BigDecimal pricePerNight,
        int totalRooms,
        Integer availableRooms) {

    public static HotelResponse from(Hotel hotel, Integer availableRooms) {
        return new HotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getLocation(),
                hotel.getDescription(),
                hotel.getRoomType(),
                hotel.getMaxGuestsPerRoom(),
                List.copyOf(hotel.getAmenities()),
                hotel.getPricePerNight(),
                hotel.getTotalRooms(),
                availableRooms);
    }
}
