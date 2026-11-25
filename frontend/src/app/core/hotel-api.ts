import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { toAppError } from './app-error';
import { Hotel, HotelSearchQuery, PriceBreakdown } from './models';

const fail = (e: unknown) => throwError(() => toAppError(e));

@Injectable({ providedIn: 'root' })
export class HotelApi {
  private readonly http = inject(HttpClient);

  search(query: HotelSearchQuery): Observable<Hotel[]> {
    let params = new HttpParams();
    if (query.location?.trim()) {
      params = params.set('location', query.location.trim());
    }
    if (query.checkInDate && query.checkOutDate) {
      params = params.set('checkInDate', query.checkInDate).set('checkOutDate', query.checkOutDate);
    }
    return this.http.get<Hotel[]>('/api/hotels', { params }).pipe(catchError(fail));
  }

  get(id: number): Observable<Hotel> {
    return this.http.get<Hotel>(`/api/hotels/${id}`).pipe(catchError(fail));
  }

  availability(id: number, checkInDate: string, checkOutDate: string): Observable<Hotel> {
    const params = new HttpParams().set('checkInDate', checkInDate).set('checkOutDate', checkOutDate);
    return this.http.get<Hotel>(`/api/hotels/${id}/availability`, { params }).pipe(catchError(fail));
  }

  quote(id: number, checkInDate: string, checkOutDate: string): Observable<PriceBreakdown> {
    const params = new HttpParams().set('checkInDate', checkInDate).set('checkOutDate', checkOutDate);
    return this.http.get<PriceBreakdown>(`/api/hotels/${id}/quote`, { params }).pipe(catchError(fail));
  }
}
