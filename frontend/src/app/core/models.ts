export interface Hotel {
  id: number;
  name: string;
  location: string;
  description: string;
  roomType: string;
  maxGuestsPerRoom: number;
  amenities: string[];
  pricePerNight: number;
  totalRooms: number;
  /** null when no dates were requested; otherwise rooms left for that stay. */
  availableRooms: number | null;
}

export interface NightlyPriceLine {
  date: string;
  dayOfWeek: string;
  weekendNight: boolean;
  basePrice: number;
  surcharge: number;
  price: number;
}

export interface PriceBreakdown {
  checkInDate: string;
  checkOutDate: string;
  nights: number;
  pricePerNight: number;
  weekendNights: number;
  weekendSurchargePercent: number;
  baseAmount: number;
  surchargeAmount: number;
  totalAmount: number;
  currency: string;
  nightlyBreakdown: NightlyPriceLine[];
}

export type BookingStatus = 'CONFIRMED' | 'CANCELLED';

export interface Booking {
  bookingReference: string;
  status: BookingStatus;
  cancellable: boolean;
  hotelId: number;
  hotelName: string;
  hotelLocation: string;
  roomType: string;
  guestName: string;
  guestEmail: string;
  checkInDate: string;
  checkOutDate: string;
  nights: number;
  createdAt: string;
  cancelledAt: string | null;
  price: PriceBreakdown;
}

export interface CreateBookingPayload {
  hotelId: number;
  checkInDate: string;
  checkOutDate: string;
  guestName: string;
  guestEmail: string;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface HotelSearchQuery {
  location?: string;
  checkInDate?: string;
  checkOutDate?: string;
}
