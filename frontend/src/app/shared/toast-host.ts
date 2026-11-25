import { Component, inject } from '@angular/core';
import { ToastService } from '../core/toast';

@Component({
  selector: 'app-toast-host',
  template: `
    <div class="stack" role="status" aria-live="polite">
      @for (t of toasts.toasts(); track t.id) {
        <button type="button" class="toast" [class]="t.kind" (click)="toasts.dismiss(t.id)">
          <span class="dot" aria-hidden="true"></span>{{ t.message }}
        </button>
      }
    </div>
  `,
  styles: `
    .stack {
      position: fixed;
      z-index: 100;
      left: 50%;
      bottom: 1.4rem;
      transform: translateX(-50%);
      display: grid;
      gap: 0.5rem;
      width: min(420px, 100% - 2rem);
      pointer-events: none;
    }
    .toast {
      pointer-events: auto;
      display: flex;
      align-items: center;
      gap: 0.7rem;
      padding: 0.85rem 1.1rem;
      border: 0;
      border-radius: 14px;
      background: var(--indigo-deep);
      color: #fff;
      font-weight: 600;
      text-align: left;
      cursor: pointer;
      box-shadow: var(--shadow-lift);
      animation: pop 0.45s var(--ease);
    }
    .dot {
      flex: none;
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: var(--marigold);
    }
    .ok .dot {
      background: #5fe0b4;
    }
    .error .dot {
      background: #ff7a93;
    }
    @keyframes pop {
      from {
        opacity: 0;
        transform: translateY(14px) scale(0.96);
      }
    }
  `,
})
export class ToastHost {
  protected readonly toasts = inject(ToastService);
}
