import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { EMPTY, Subject, catchError, debounceTime, finalize, forkJoin, map, merge, startWith, switchMap, tap } from 'rxjs';
import { AppError } from '../../core/app-error';
import { BookingApi } from '../../core/booking-api';
import { HotelApi } from '../../core/hotel-api';
import { Hotel, PriceBreakdown } from '../../core/models';
import { MAX_STAY_NIGHTS, addDays, nightsBetween, stayProblem, todayIso } from '../../shared/dates';
import { HotelScene } from '../../shared/hotel-scene';
import { InrPipe } from '../../shared/inr.pipe';
import { PriceBreakdownView } from '../../shared/price-breakdown';

type ControlName = 'checkInDate' | 'checkOutDate' | 'guestName' | 'guestEmail';

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

@Component({
  selector: 'app-hotel-detail-page',
  imports: [ReactiveFormsModule, RouterLink, HotelScene, InrPipe, PriceBreakdownView],
  templateUrl: './hotel-detail.page.html',
  styleUrl: './hotel-detail.page.css',
})
export class HotelDetailPage implements OnInit {
  // Bound from the route: /hotels/:id?checkInDate=...&checkOutDate=...
  readonly id = input.required<string>();
  readonly checkInDate = input<string>();
  readonly checkOutDate = input<string>();

  private readonly hotelApi = inject(HotelApi);
  private readonly bookingApi = inject(BookingApi);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly refresh$ = new Subject<void>();

  protected readonly today = todayIso();
  protected readonly hotel = signal<Hotel | null>(null);
  protected readonly loadError = signal<string | null>(null);
  protected readonly checking = signal(false);
  protected readonly left = signal<number | null>(null);
  protected readonly quote = signal<PriceBreakdown | null>(null);
  protected readonly dateMessage = signal<string | null>(null);
  protected readonly submitting = signal(false);
  protected readonly bookingError = signal<AppError | null>(null);

  protected readonly form = new FormGroup({
    checkInDate: new FormControl('', { nonNullable: true }),
    checkOutDate: new FormControl('', { nonNullable: true }),
    guestName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)],
    }),
    guestEmail: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(EMAIL), Validators.maxLength(254)],
    }),
  });

  ngOnInit(): void {
    this.form.patchValue(
      { checkInDate: this.checkInDate() ?? '', checkOutDate: this.checkOutDate() ?? '' },
      { emitEvent: false },
    );
    const hotelId = Number(this.id());

    this.hotelApi
      .get(hotelId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (h) => this.hotel.set(h),
        error: (e: AppError) => this.loadError.set(e.status === 404 ? 'We couldn’t find that hotel.' : e.message),
      });

    // Keep a check-out that would fall on or before the new check-in one night after it.
    this.form.controls.checkInDate.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((checkIn) => {
      const checkOut = this.form.controls.checkOutDate.value;
      if (checkIn && (!checkOut || checkOut <= checkIn)) {
        this.form.controls.checkOutDate.setValue(addDays(checkIn, 1));
      }
    });

    // Whenever the dates change, ask the backend for rooms left and the exact price.
    merge(this.form.controls.checkInDate.valueChanges, this.form.controls.checkOutDate.valueChanges, this.refresh$)
      .pipe(
        startWith(null),
        debounceTime(180),
        switchMap(() => this.loadStay(hotelId)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private loadStay(hotelId: number) {
    const { checkInDate, checkOutDate } = this.form.getRawValue();
    if (!checkInDate || !checkOutDate) {
      this.clearStay(null);
      return EMPTY;
    }
    const problem = stayProblem(checkInDate, checkOutDate);
    if (problem) {
      this.clearStay(problem);
      return EMPTY;
    }
    this.dateMessage.set(null);
    this.checking.set(true);
    return forkJoin({
      avail: this.hotelApi.availability(hotelId, checkInDate, checkOutDate),
      quote: this.hotelApi.quote(hotelId, checkInDate, checkOutDate),
    }).pipe(
      tap(({ avail, quote }) => {
        this.left.set(avail.availableRooms);
        this.quote.set(quote);
      }),
      catchError((e: AppError) => {
        this.clearStay(e.message);
        return EMPTY;
      }),
      finalize(() => this.checking.set(false)),
      map(() => undefined),
    );
  }

  private clearStay(message: string | null): void {
    this.dateMessage.set(message);
    this.left.set(null);
    this.quote.set(null);
    this.checking.set(false);
  }

  /** Few rooms left, relative to the size of the hotel: worth nudging the guest. */
  protected scarce(): boolean {
    const left = this.left();
    const total = this.hotel()?.totalRooms ?? 0;
    return left !== null && left > 0 && left < total && (left <= 2 || left / total <= 0.25);
  }

  protected nights(): number {
    const { checkInDate, checkOutDate } = this.form.getRawValue();
    return checkInDate && checkOutDate && checkOutDate > checkInDate ? nightsBetween(checkInDate, checkOutDate) : 0;
  }

  protected stepNights(delta: number): void {
    const { checkInDate } = this.form.getRawValue();
    if (!checkInDate) {
      return;
    }
    const next = Math.min(MAX_STAY_NIGHTS, Math.max(1, (this.nights() || 1) + delta));
    this.form.controls.checkOutDate.setValue(addDays(checkInDate, next));
  }

  protected backParams(): Record<string, string | null> {
    const { checkInDate, checkOutDate } = this.form.getRawValue();
    return { checkInDate: checkInDate || null, checkOutDate: checkOutDate || null };
  }

  protected errorFor(name: ControlName): string | null {
    const control = this.form.controls[name];
    if (!control.errors || !(control.touched || control.dirty)) {
      return null;
    }
    if (control.errors['server']) {
      return control.errors['server'] as string;
    }
    if (name === 'guestName') {
      return control.errors['maxlength'] ? 'Keep the name under 100 characters.' : 'Enter the guest’s name.';
    }
    if (name === 'guestEmail') {
      return control.errors['required'] ? 'Enter an email address.' : 'Enter a valid email address, like name@example.com.';
    }
    return null;
  }

  protected submit(): void {
    this.bookingError.set(null);
    this.form.markAllAsTouched();
    const value = this.form.getRawValue();
    if (!value.checkInDate || !value.checkOutDate) {
      this.dateMessage.set('Choose your check-in and check-out dates first.');
      return;
    }
    if (this.dateMessage() || this.form.invalid || this.left() === 0 || this.checking() || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.bookingApi
      .create({
        hotelId: Number(this.id()),
        checkInDate: value.checkInDate,
        checkOutDate: value.checkOutDate,
        guestName: value.guestName.trim(),
        guestEmail: value.guestEmail.trim(),
      })
      .pipe(
        finalize(() => this.submitting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (booking) => {
          this.bookingApi.remember(booking.bookingReference, booking.guestEmail);
          this.router.navigate(['/confirmation', booking.bookingReference], { state: { booking } });
        },
        error: (e: AppError) => this.showBookingError(e),
      });
  }

  /** Field-level problems go under their inputs; anything else (409 sold out, 503, network) becomes a banner. */
  private showBookingError(error: AppError): void {
    let unplaced = error.fieldErrors.length === 0;
    for (const fieldError of error.fieldErrors) {
      const control = this.form.get(fieldError.field);
      if (control) {
        control.setErrors({ server: fieldError.message });
        control.markAsTouched();
      } else {
        unplaced = true;
      }
    }
    if (unplaced || error.status !== 400) {
      this.bookingError.set(error);
    }
    if (error.status === 409) {
      this.refresh$.next(); // somebody took the last room: show the new availability
    }
    this.cdr.markForCheck();
  }
}
