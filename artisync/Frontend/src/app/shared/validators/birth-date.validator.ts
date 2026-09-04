import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Rango de años hacia atrás considerado razonable para una fecha de nacimiento. */
export const BIRTH_DATE_MAX_AGE_YEARS = 120;

/** Edad mínima exigida en los formularios donde la fecha de nacimiento es obligatoria. */
export const BIRTH_DATE_MIN_AGE_YEARS = 18;

/**
 * Rechaza fechas futuras y fechas anteriores a BIRTH_DATE_MAX_AGE_YEARS años atrás.
 * No exige presencia del valor (combinar con Validators.required si aplica).
 */
export function birthDateRangeValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;

  const date = new Date(control.value);
  if (isNaN(date.getTime())) return { invalidDate: true };

  const today = new Date();
  today.setHours(0, 0, 0, 0);
  if (date > today) return { futureDate: true };

  const minDate = new Date(today);
  minDate.setFullYear(minDate.getFullYear() - BIRTH_DATE_MAX_AGE_YEARS);
  if (date < minDate) return { tooOld: true };

  return null;
}

/** Exige una edad mínima (por defecto BIRTH_DATE_MIN_AGE_YEARS) a partir de la fecha de nacimiento. */
export function minAgeValidator(minAge: number = BIRTH_DATE_MIN_AGE_YEARS): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;

    const birthDate = new Date(control.value);
    if (isNaN(birthDate.getTime())) return null;

    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    return age >= minAge ? null : { underage: true };
  };
}

/** Fecha ISO (yyyy-MM-dd) más antigua aceptada, para el atributo [min] de un input[type=date]. */
export function minBirthDateIso(yearsBack: number = BIRTH_DATE_MAX_AGE_YEARS): string {
  const d = new Date();
  d.setFullYear(d.getFullYear() - yearsBack);
  return d.toISOString().split('T')[0];
}

/** Fecha ISO (yyyy-MM-dd) más reciente aceptada, para el atributo [max] de un input[type=date]. */
export function maxBirthDateIso(minAge: number = BIRTH_DATE_MIN_AGE_YEARS): string {
  const d = new Date();
  d.setFullYear(d.getFullYear() - minAge);
  return d.toISOString().split('T')[0];
}
