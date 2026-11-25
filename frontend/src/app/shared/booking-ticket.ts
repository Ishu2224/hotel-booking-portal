import { Component, computed, input } from '@angular/core';
import { Booking } from '../core/models';
import { dateTime, dayMonth, weekdayLong } from './dates';
import { InrPipe } from './inr.pipe';
import { PriceBreakdownView } from './price-breakdown';

/** The booking, drawn as a ticket: details on the left, reference and status stamp on the stub. */
@Component({
  selector: 'app-booking-ticket',
  imports: [InrPipe, PriceBreakdownView],
  templateUrl: './booking-ticket.html',
  styleUrl: './booking-ticket.css',
})
export class BookingTicket {
  readonly booking = input.required<Booking>();

  protected readonly cancelled = computed(() => this.booking().status === 'CANCELLED');
  protected readonly weekdayLong = weekdayLong;
  protected readonly dayMonth = dayMonth;
  protected readonly dateTime = dateTime;
}
