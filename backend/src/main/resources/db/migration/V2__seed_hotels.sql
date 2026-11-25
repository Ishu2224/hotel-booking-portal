-- Sample data: 8 hotels across Indian cities, one room category each.
-- Lake Pichola Residency has only 3 rooms so "sold out" and the concurrency rule are easy to demo.

INSERT INTO hotels (id, name, location, description, room_type, price_per_night, total_rooms, max_guests_per_room) VALUES
(1, 'Marine Drive Grand', 'Mumbai, Maharashtra',
 'A landmark hotel on the Queen''s Necklace with sweeping Arabian Sea views, minutes from Churchgate station, Colaba and the business district.',
 'Deluxe Sea-View Room', 8500.00, 20, 3),
(2, 'Calangute Sands Resort', 'Calangute, Goa',
 'A relaxed beachside resort a short walk from Calangute beach, with a lagoon pool, shacks nearby and easy access to Baga and Anjuna.',
 'Premium Pool-View Room', 6200.00, 15, 3),
(3, 'Amer Haveli Palace', 'Jaipur, Rajasthan',
 'A restored haveli below Amer Fort with hand-painted frescoes, courtyard dining and evening folk performances.',
 'Heritage Suite', 9800.00, 10, 3),
(4, 'Indiranagar Urban Suites', 'Bengaluru, Karnataka',
 'Modern serviced suites in Indiranagar, close to cafes, microbreweries and the metro. Built for business travellers and longer stays.',
 'Executive Studio Suite', 5400.00, 12, 2),
(5, 'Solang Pine Lodge', 'Manali, Himachal Pradesh',
 'A cosy timber lodge among pine forests near Solang Valley, with snow-peak views, a bonfire deck and adventure-sports desk.',
 'Mountain-View Cottage Room', 4200.00, 6, 4),
(6, 'Lake Pichola Residency', 'Udaipur, Rajasthan',
 'A boutique lakefront hotel with only three royal rooms, rooftop dining facing the City Palace and sunset boat rides.',
 'Lake-View Royal Room', 12500.00, 3, 2),
(7, 'Fort Kochi Heritage Inn', 'Kochi, Kerala',
 'A restored Indo-Portuguese building in Fort Kochi, steps from the Chinese fishing nets, galleries and spice-route cafes.',
 'Colonial Heritage Room', 5800.00, 8, 3),
(8, 'Rishikesh Riverside Retreat', 'Rishikesh, Uttarakhand',
 'A peaceful retreat on the banks of the Ganga offering sunrise yoga, rafting trips and Ganga aarti within walking distance.',
 'Riverside Deluxe Room', 3800.00, 10, 3);

INSERT INTO hotel_amenities (hotel_id, amenity) VALUES
(1, 'Free Wi-Fi'), (1, 'Air conditioning'), (1, 'Infinity pool'), (1, 'Spa'), (1, 'Restaurant'), (1, 'Airport transfer'),
(2, 'Free Wi-Fi'), (2, 'Air conditioning'), (2, 'Lagoon pool'), (2, 'Beach access'), (2, 'Bar'), (2, 'Breakfast included'),
(3, 'Free Wi-Fi'), (3, 'Air conditioning'), (3, 'Courtyard restaurant'), (3, 'Cultural evenings'), (3, 'Spa'), (3, 'Parking'),
(4, 'Free Wi-Fi'), (4, 'Air conditioning'), (4, 'Gym'), (4, 'Co-working lounge'), (4, 'Kitchenette'), (4, 'Parking'),
(5, 'Free Wi-Fi'), (5, 'Room heater'), (5, 'Bonfire deck'), (5, 'Mountain view'), (5, 'Breakfast included'), (5, 'Adventure desk'),
(6, 'Free Wi-Fi'), (6, 'Air conditioning'), (6, 'Rooftop restaurant'), (6, 'Lake view'), (6, 'Boat rides'), (6, 'Breakfast included'),
(7, 'Free Wi-Fi'), (7, 'Air conditioning'), (7, 'Courtyard cafe'), (7, 'Ayurvedic massage'), (7, 'Bicycle rental'), (7, 'Breakfast included'),
(8, 'Free Wi-Fi'), (8, 'Yoga deck'), (8, 'River view'), (8, 'Rafting desk'), (8, 'Restaurant'), (8, 'Parking');
