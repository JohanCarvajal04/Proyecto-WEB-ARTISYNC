package uteq.edu.ec.artisync.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

/** Valida el rango de {@link LocalDate} anotado con {@link ValidBirthDate}. */
public class BirthDateValidator implements ConstraintValidator<ValidBirthDate, LocalDate> {

    private int minAge;
    private int maxAgeYears;

    /**
     * Lee los límites configurados en la anotación {@link ValidBirthDate}.
     * @param constraintAnnotation instancia de la anotación sobre el campo validado
     */
    @Override
    public void initialize(ValidBirthDate constraintAnnotation) {
        this.minAge = constraintAnnotation.minAge();
        this.maxAgeYears = constraintAnnotation.maxAgeYears();
    }

    /**
     * Verifica que la fecha no sea futura, no exceda la antigüedad máxima
     * configurada y, si aplica, cumpla la edad mínima requerida.
     * @param value fecha de nacimiento a validar, {@code null} se considera válido
     * @param context contexto de validación, usado para personalizar el mensaje de error
     * @return {@code true} si la fecha cumple todas las restricciones configuradas
     */
    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocalDate today = LocalDate.now();

        if (value.isAfter(today)) {
            return fail(context, "La fecha de nacimiento no puede ser una fecha futura");
        }

        if (value.isBefore(today.minusYears(maxAgeYears))) {
            return fail(context, "La fecha de nacimiento no es válida");
        }

        if (minAge > 0 && Period.between(value, today).getYears() < minAge) {
            return fail(context, "Debes tener al menos " + minAge + " años");
        }

        return true;
    }

    private boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
