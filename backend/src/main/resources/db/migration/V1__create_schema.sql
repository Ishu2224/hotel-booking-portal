-- Hotel Booking Portal: base schema (MySQL 8+)
-- Kept free of engine/charset clauses so MySQL 8 defaults (InnoDB, utf8mb4) apply.

CREATE TABLE hotels (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    name                VARCHAR(150)  NOT NULL,
    location            VARCHAR(150)  NOT NULL,
    description         VARCHAR(1000) NOT NULL,
    room_type           VARCHAR(100)  NOT NULL,
    price_per_night     DECIMAL(10,2) NOT NULL,
    total_rooms         INT           NOT NULL,
    max_guests_per_room INT           NOT NULL,
    CONSTRAINT pk_hotels PRIMARY KEY (id),
    CONSTRAINT uk_hotels_name_location UNIQUE (name, location),
    CONSTRAINT chk_hotels_price CHECK (price_per_night > 0),
    CONSTRAINT chk_hotels_total_rooms CHECK (total_rooms > 0),
    CONSTRAINT chk_hotels_max_guests CHECK (max_guests_per_room > 0)
);

CREATE INDEX idx_hotels_location ON hotels (location);

CREATE TABLE hotel_amenities (
    hotel_id BIGINT      NOT NULL,
    amenity  VARCHAR(80) NOT NULL,
    CONSTRAINT pk_hotel_amenities PRIMARY KEY (hotel_id, amenity),
    CONSTRAINT fk_hotel_amenities_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id) ON DELETE CASCADE
);

CREATE TABLE bookings (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    booking_reference         VARCHAR(20)   NOT NULL,
    hotel_id                  BIGINT        NOT NULL,
    guest_name                VARCHAR(100)  NOT NULL,
    guest_email               VARCHAR(254)  NOT NULL,
    check_in_date             DATE          NOT NULL,
    check_out_date            DATE          NOT NULL,
    nights                    INT           NOT NULL,
    -- Pricing snapshot: later price/surcharge changes never alter an existing booking.
    price_per_night           DECIMAL(10,2) NOT NULL,
    weekend_surcharge_percent DECIMAL(5,2)  NOT NULL,
    total_amount              DECIMAL(12,2) NOT NULL,
    status                    VARCHAR(20)   NOT NULL,
    created_at                DATETIME(6)   NOT NULL,
    cancelled_at              DATETIME(6)   NULL,
    CONSTRAINT pk_bookings PRIMARY KEY (id),
    CONSTRAINT uk_bookings_reference UNIQUE (booking_reference),
    CONSTRAINT fk_bookings_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT chk_bookings_dates CHECK (check_out_date > check_in_date),
    CONSTRAINT chk_bookings_nights CHECK (nights > 0),
    CONSTRAINT chk_bookings_total CHECK (total_amount >= 0),
    CONSTRAINT chk_bookings_status CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

-- Serves the overlap/availability query: hotel + status + date range.
CREATE INDEX idx_bookings_availability ON bookings (hotel_id, status, check_in_date, check_out_date);
