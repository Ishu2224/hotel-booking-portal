import { Pipe, PipeTransform } from '@angular/core';

const whole = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const paise = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2 });

/** 12500 -> ₹12,500, 185.18 -> ₹185.18 (Indian digit grouping). */
@Pipe({ name: 'inr' })
export class InrPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '';
    }
    return Number.isInteger(value) ? whole.format(value) : paise.format(value);
  }
}
