import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AppError } from '../../core/app-error';
import { BookingApi } from '../../core/booking-api';
import { saveBlob } from '../../core/download';
import { Booking } from '../../core/models';
import { ToastService } from '../../core/toast';
import { BookingTicket } from '../../shared/booking-ticket';

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

@Component({
  selector: 'app-manage-page',
  imports: [ReactiveFormsModule, RouterLink, BookingTicket],
  templateUrl: './manage.page.html',
  styleUrl: './manage.page.css',
})
export class ManagePage implements OnInit {
  /** /manage?ref=TC-2026-XXXXXX pre-fills the reference. */
  readonly ref = input<string>();

  private readonly bookingApi = inject(BookingApi);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly form = new FormGroup({
    reference: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/\S/)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(EMAIL)] }),
  });

  protected readonly booking = signal<Booking | null>(null);
  protected readonly error = signal<AppError | null>(null);
  protected readonly searching = signal(false);
  protected readonly confirmingCancel = signal(false);
  protected readonly cancelling = signal(false);
  protected readonly downloading = signal(false);

  ngOnInit(): void {
    const saved = this.bookingApi.recall();
    const reference = this.ref() ?? saved?.reference ?? '';
    const email = saved && saved.reference === reference ? saved.email : '';
    this.form.patchValue({ reference, email });
  }

  protected fieldError(name: 'reference' | 'email'): string | null {
    const control = this.form.controls[name];
    if (!control.errors || !(control.touched || control.dirty)) {
      return null;
    }
    return name === 'reference' ? 'Enter your booking reference, like TC-2026-K7XM3P.' : 'Enter the email you booked with.';
  }

  protected find(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.searching()) {
      return;
    }
    const { reference, email } = this.form.getRawValue();
    this.searching.set(true);
    this.error.set(null);
    this.confirmingCancel.set(false);
    this.bookingApi
      .lookup(reference.trim(), email.trim())
      .pipe(
        finalize(() => this.searching.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (b) => {
          this.booking.set(b);
          this.bookingApi.remember(b.bookingReference, b.guestEmail);
        },
        error: (e: AppError) => {
          this.booking.set(null);
          this.error.set(e);
        },
      });
  }

  protected cancel(): void {
    const b = this.booking();
    if (!b || this.cancelling()) {
      return;
    }
    this.cancelling.set(true);
    this.bookingApi
      .cancel(b.bookingReference, b.guestEmail)
      .pipe(
        finalize(() => this.cancelling.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (updated) => {
          this.booking.set(updated);
          this.confirmingCancel.set(false);
          this.toast.show('Booking cancelled. The room is available to others again.', 'info');
        },
        error: (e: AppError) => {
          this.confirmingCancel.set(false);
          this.error.set(e);
        },
      });
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
}
