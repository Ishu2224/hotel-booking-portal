import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, from, map, switchMap, throwError } from 'rxjs';
import { toAppError } from './app-error';
import { Booking, CreateBookingPayload } from './models';

export interface VoucherFile {
  blob: Blob;
  filename: string;
}

const LAST_BOOKING_KEY = 'padav.lastBooking';

@Injectable({ providedIn: 'root' })
export class BookingApi {
  private readonly http = inject(HttpClient);

  create(payload: CreateBookingPayload): Observable<Booking> {
    return this.http.post<Booking>('/api/bookings', payload).pipe(catchError((e) => throwError(() => toAppError(e))));
  }

  lookup(bookingReference: string, guestEmail: string): Observable<Booking> {
    return this.http
      .post<Booking>('/api/bookings/lookup', { bookingReference, guestEmail })
      .pipe(catchError((e) => throwError(() => toAppError(e))));
  }

  cancel(bookingReference: string, guestEmail: string): Observable<Booking> {
    return this.http
      .post<Booking>('/api/bookings/cancel', { bookingReference, guestEmail })
      .pipe(catchError((e) => throwError(() => toAppError(e))));
  }

  /** The voucher is rendered by the backend; errors arrive as a Blob, so unwrap them into AppError. */
  voucher(bookingReference: string, guestEmail: string): Observable<VoucherFile> {
    return this.http
      .post('/api/bookings/voucher', { bookingReference, guestEmail }, { observe: 'response', responseType: 'blob' })
      .pipe(
        map((res) => ({
          blob: res.body as Blob,
          filename: filenameFrom(res.headers.get('Content-Disposition')) ?? `voucher-${bookingReference}.html`,
        })),
        catchError((err: unknown) => {
          if (err instanceof HttpErrorResponse && err.error instanceof Blob) {
            return from(err.error.text()).pipe(
              map((text) => parseJson(text)),
              switchMap((body) =>
                throwError(() => toAppError(new HttpErrorResponse({ status: err.status, error: body }))),
              ),
            );
          }
          return throwError(() => toAppError(err));
        }),
      );
  }

  /** Lets the confirmation page survive a refresh: only reference + email are kept, for this tab only. */
  remember(reference: string, email: string): void {
    try {
      sessionStorage.setItem(LAST_BOOKING_KEY, JSON.stringify({ reference, email }));
    } catch {
      /* storage unavailable: the confirmation page simply falls back to the lookup form */
    }
  }

  recall(): { reference: string; email: string } | null {
    try {
      const raw = sessionStorage.getItem(LAST_BOOKING_KEY);
      return raw ? (JSON.parse(raw) as { reference: string; email: string }) : null;
    } catch {
      return null;
    }
  }
}

function filenameFrom(header: string | null): string | null {
  const match = header?.match(/filename="?([^";]+)"?/i);
  return match ? match[1] : null;
}

function parseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}
