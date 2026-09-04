package uteq.edu.ec.artisync.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

public class BirthDateValidator implements ConstraintValidator<ValidBirthDate, LocalDate> {

    private int minAge;
    private int maxAgeYears;

    @Override
    public void initialize(ValidBirthDate constraintAnnotation) {
        this.minAge = constraintAnnotation.minAge();
        this.maxAgeYears = constraintAnnotation.maxAgeYears();
    }

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
