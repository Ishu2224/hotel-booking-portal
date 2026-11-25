import { HttpErrorResponse } from '@angular/common/http';
import { FieldError } from './models';

/** The one error type the UI deals with: built from the backend's consistent error JSON. */
export class AppError extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly fieldErrors: FieldError[] = [],
  ) {
    super(message);
  }

  get isNetwork(): boolean {
    return this.status === 0;
  }

  fieldMessage(field: string): string | undefined {
    return this.fieldErrors.find((e) => e.field === field)?.message;
  }
}

export function toAppError(err: unknown): AppError {
  if (err instanceof AppError) {
    return err;
  }
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return new AppError(
        0,
        'We can’t reach the booking server. Check that the backend is running on port 8080, then try again.',
      );
    }
    const body = err.error as { message?: unknown; fieldErrors?: unknown } | null;
    if (body && typeof body === 'object' && typeof body.message === 'string') {
      const fieldErrors = Array.isArray(body.fieldErrors) ? (body.fieldErrors as FieldError[]) : [];
      return new AppError(err.status, body.message, fieldErrors);
    }
    return new AppError(err.status, 'Something went wrong on our side. Please try again.');
  }
  return new AppError(-1, 'Something went wrong. Please try again.');
}
