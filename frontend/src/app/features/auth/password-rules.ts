import { AbstractControl, ValidationErrors } from '@angular/forms';

/** Mirrors AuthService.validatePasswordStrength on the server. */
export const passwordRules: { label: string; test: (value: string) => boolean }[] = [
  { label: 'At least 8 characters', test: v => v.length >= 8 },
  { label: 'An uppercase and a lowercase letter', test: v => /[A-Z]/.test(v) && /[a-z]/.test(v) },
  { label: 'A number', test: v => /\d/.test(v) },
  { label: 'A symbol such as ! @ # $', test: v => /[^A-Za-z0-9]/.test(v) }
];

export function strongPassword(control: AbstractControl): ValidationErrors | null {
  const value = String(control.value ?? '');
  return passwordRules.every(rule => rule.test(value)) ? null : { weakPassword: true };
}
