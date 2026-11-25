import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, of, switchMap, tap } from 'rxjs';
import { toAppError } from '../../core/app-error';
import { HotelApi } from '../../core/hotel-api';
import { Hotel } from '../../core/models';
import { addDays, dayMonth, nightsBetween, stayProblem, todayIso } from '../../shared/dates';
import { HotelScene } from '../../shared/hotel-scene';
import { InrPipe } from '../../shared/inr.pipe';

interface Filters {
  location: string;
  checkInDate: string;
  checkOutDate: string;
}

@Component({
  selector: 'app-search-page',
  imports: [ReactiveFormsModule, RouterLink, HotelScene, InrPipe],
  templateUrl: './search.page.html',
  styleUrl: './search.page.css',
})
export class SearchPage {
  private readonly api = inject(HotelApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly today = todayIso();
  protected readonly cities = ['Mumbai', 'Goa', 'Jaipur', 'Bengaluru', 'Manali', 'Udaipur', 'Kochi', 'Rishikesh'];

  protected readonly form = new FormGroup({
    location: new FormControl('', { nonNullable: true }),
    checkInDate: new FormControl('', { nonNullable: true }),
    checkOutDate: new FormControl('', { nonNullable: true }),
  });

  protected readonly hotels = signal<Hotel[] | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly formError = signal<string | null>(null);
  /** The filters that produced the current results (what the URL says). */
  protected readonly applied = signal<Filters>({ location: '', checkInDate: '', checkOutDate: '' });
  protected readonly skeletons = [1, 2, 3, 4, 5, 6];

  constructor() {
    // The URL is the source of truth: submitting the form just navigates, and this stream loads the results.
    this.route.queryParamMap
      .pipe(
        tap((params) => {
          const filters: Filters = {
            location: params.get('location') ?? '',
            checkInDate: params.get('checkInDate') ?? '',
            checkOutDate: params.get('checkOutDate') ?? '',
          };
          this.form.setValue(filters, { emitEvent: false });
          this.applied.set(filters);
          this.formError.set(null);
          this.loading.set(true);
          this.loadError.set(null);
        }),
        switchMap(() =>
          this.api.search(this.applied()).pipe(
            catchError((e) => {
              this.loadError.set(toAppError(e).message);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((result) => {
        this.hotels.set(result);
        this.loading.set(false);
      });

    // Picking a check-in date pulls check-out forward if it would otherwise be before it.
    this.form.controls.checkInDate.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((checkIn) => {
      const checkOut = this.form.controls.checkOutDate.value;
      if (checkIn && (!checkOut || checkOut <= checkIn)) {
        this.form.controls.checkOutDate.setValue(addDays(checkIn, 1), { emitEvent: false });
      }
    });
  }

  protected submit(): void {
    const { location, checkInDate, checkOutDate } = this.form.getRawValue();
    if (!!checkInDate !== !!checkOutDate) {
      this.formError.set('Pick both a check-in and a check-out date, or leave both empty.');
      return;
    }
    if (checkInDate && checkOutDate) {
      const problem = stayProblem(checkInDate, checkOutDate);
      if (problem) {
        this.formError.set(problem);
        return;
      }
    }
    this.formError.set(null);
    this.router.navigate(['/'], {
      queryParams: { location: location.trim() || null, checkInDate: checkInDate || null, checkOutDate: checkOutDate || null },
    });
  }

  protected pickCity(city: string): void {
    this.form.controls.location.setValue(city);
    this.submit();
  }

  protected clearAll(): void {
    this.form.reset({ location: '', checkInDate: '', checkOutDate: '' });
    this.formError.set(null);
    this.router.navigate(['/']);
  }

  protected hasFilters(): boolean {
    const f = this.applied();
    return !!(f.location || f.checkInDate);
  }

  protected detailParams(): Record<string, string | null> {
    const f = this.applied();
    return { checkInDate: f.checkInDate || null, checkOutDate: f.checkOutDate || null };
  }

  protected summary(count: number): string {
    const f = this.applied();
    const stays = `${count} ${count === 1 ? 'stay' : 'stays'}`;
    const place = f.location ? ` matching “${f.location}”` : '';
    if (f.checkInDate && f.checkOutDate) {
      const n = nightsBetween(f.checkInDate, f.checkOutDate);
      return `${stays}${place} for ${n} ${n === 1 ? 'night' : 'nights'}, ${dayMonth(f.checkInDate)} to ${dayMonth(f.checkOutDate)}`;
    }
    return `${stays}${place}`;
  }

  protected availability(hotel: Hotel): { text: string; tone: 'ok' | 'low' | 'out' } | null {
    const left = hotel.availableRooms;
    if (left === null) {
      return null;
    }
    if (left === 0) {
      return { text: 'Sold out for these dates', tone: 'out' };
    }
    if (left <= 3) {
      return { text: left === 1 ? 'Last room left' : `Only ${left} rooms left`, tone: 'low' };
    }
    return { text: `${left} rooms free`, tone: 'ok' };
  }
}
