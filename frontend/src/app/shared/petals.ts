import { Component, signal } from '@angular/core';

interface Petal {
  dx: number;
  dy: number;
  rot: number;
  delay: number;
  size: number;
  color: string;
}

const COLORS = ['#f5a623', '#ffc247', '#e8890c', '#0b7a7c', '#ff7a93'];

/** A one-time burst of marigold petals when a booking is confirmed. Removes itself afterwards. */
@Component({
  selector: 'app-petals',
  template: `
    @if (alive()) {
      <div class="burst" aria-hidden="true">
        @for (p of petals; track $index) {
          <i
            [style.--dx.px]="p.dx"
            [style.--dy.px]="p.dy"
            [style.--rot.deg]="p.rot"
            [style.--size.px]="p.size"
            [style.animation-delay.ms]="p.delay"
            [style.background]="p.color"
          ></i>
        }
      </div>
    }
  `,
  styles: `
    .burst {
      position: fixed;
      left: 50%;
      top: 34%;
      width: 0;
      height: 0;
      z-index: 60;
      pointer-events: none;
    }
    i {
      position: absolute;
      width: var(--size);
      height: calc(var(--size) * 1.6);
      border-radius: 60% 40% 60% 40%;
      opacity: 0;
      animation: fly 2.1s cubic-bezier(0.15, 0.7, 0.3, 1) forwards;
    }
    @keyframes fly {
      0% {
        opacity: 0;
        transform: translate(0, 0) rotate(0) scale(0.4);
      }
      12% {
        opacity: 1;
      }
      100% {
        opacity: 0;
        transform: translate(var(--dx), var(--dy)) rotate(var(--rot)) scale(1);
      }
    }
    @media (prefers-reduced-motion: reduce) {
      .burst {
        display: none;
      }
    }
  `,
})
export class Petals {
  protected readonly alive = signal(true);
  protected readonly petals: Petal[] = Array.from({ length: 34 }, (_, i) => {
    const angle = (Math.PI * 2 * i) / 34 + Math.random() * 0.4;
    const dist = 140 + Math.random() * 260;
    return {
      dx: Math.cos(angle) * dist,
      dy: Math.sin(angle) * dist * 0.75 + 140,
      rot: (Math.random() - 0.5) * 720,
      delay: Math.random() * 160,
      size: 7 + Math.random() * 7,
      color: COLORS[i % COLORS.length],
    };
  });

  constructor() {
    setTimeout(() => this.alive.set(false), 2600);
  }
}
