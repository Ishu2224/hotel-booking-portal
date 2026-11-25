import { Component, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { PriceBreakdown } from '../core/models';
import { dayMonth, weekday } from './dates';
import { InrPipe } from './inr.pipe';

/** Night-by-night price table. Every number shown here comes from the backend's breakdown. */
@Component({
  selector: 'app-price-breakdown',
  imports: [InrPipe],
  templateUrl: './price-breakdown.html',
  styleUrl: './price-breakdown.css',
})
export class PriceBreakdownView {
  readonly price = input.required<PriceBreakdown>();

  protected readonly shownTotal = signal(0);
  protected readonly weekday = weekday;
  protected readonly dayMonth = dayMonth;

  private frame = 0;
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    effect(() => {
      const target = this.price().totalAmount;
      untracked(() => this.countTo(target));
    });
    this.destroyRef.onDestroy(() => cancelAnimationFrame(this.frame));
  }

  /** Eases the total from its previous value to the new one so a price change is noticed, not missed. */
  private countTo(target: number): void {
    cancelAnimationFrame(this.frame);
    const from = this.shownTotal();
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduceMotion || from === 0 || from === target) {
      this.shownTotal.set(target);
      return;
    }
    const start = performance.now();
    const duration = 450;
    const step = (now: number) => {
      const t = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - t, 3);
      this.shownTotal.set(t === 1 ? target : Math.round(from + (target - from) * eased));
      if (t < 1) {
        this.frame = requestAnimationFrame(step);
      }
    };
    this.frame = requestAnimationFrame(step);
  }
}
