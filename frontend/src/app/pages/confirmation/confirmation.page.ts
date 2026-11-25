import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AppError } from '../../core/app-error';
import { BookingApi } from '../../core/booking-api';
import { saveBlob } from '../../core/download';
import { Booking } from '../../core/models';
import { ToastService } from '../../core/toast';
import { BookingTicket } from '../../shared/booking-ticket';
import { Petals } from '../../shared/petals';

@Component({
  selector: 'app-confirmation-page',
  imports: [RouterLink, BookingTicket, Petals],
  templateUrl: './confirmation.page.html',
  styleUrl: './confirmation.page.css',
})
export class ConfirmationPage implements OnInit {
  readonly ref = input.required<string>();

  private readonly bookingApi = inject(BookingApi);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly booking = signal<Booking | null>(null);
  protected readonly error = signal<AppError | null>(null);
  protected readonly downloading = signal(false);
  /** True when we arrived straight from the booking form: that is when the petals fly. */
  protected readonly fresh = signal(false);
  protected readonly firstName = computed(() => this.booking()?.guestName.trim().split(/\s+/)[0] ?? '');

  ngOnInit(): void {
    const reference = this.ref();
    const fromNavigation = history.state?.booking as Booking | undefined;
    if (fromNavigation?.bookingReference === reference) {
      this.booking.set(fromNavigation);
      this.fresh.set(true);
    }

    const saved = this.bookingApi.recall();
    if (saved?.reference === reference) {
      // Always confirm with the server: the state we navigated with could be out of date.
      this.bookingApi
        .lookup(saved.reference, saved.email)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (b) => this.booking.set(b),
          error: (e: AppError) => {
            if (!this.booking()) {
              this.error.set(e);
            }
          },
        });
    } else if (!fromNavigation || fromNavigation.bookingReference !== reference) {
      this.router.navigate(['/manage'], { queryParams: { ref: reference }, replaceUrl: true });
    }
  }

  protected download(): void {
    const b = this.booking();
    if (!b || this.downloading()) {
      return;
    }
    this.downloading.set(true);
    this.bookingApi
      .voucher(b.bookingReference, b.guestEmail)
      .pipe(
        finalize(() => this.downloading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (file) => {
          saveBlob(file.blob, file.filename);
          this.toast.show('Voucher downloaded. Open it to print or save as PDF.');
        },
        error: (e: AppError) => this.toast.show(e.message, 'error'),
      });
  }

  protected async copyReference(): Promise<void> {
    const b = this.booking();
    if (!b) {
      return;
    }
    try {
      await navigator.clipboard.writeText(b.bookingReference);
      this.toast.show('Reference copied.');
    } catch {
      this.toast.show('Couldn’t copy automatically. Select the reference on the ticket instead.', 'info');
    }
  }
}
