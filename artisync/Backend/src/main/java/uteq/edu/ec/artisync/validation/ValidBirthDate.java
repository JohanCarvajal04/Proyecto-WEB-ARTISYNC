package uteq.edu.ec.artisync.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restringe un {@link java.time.LocalDate} a un rango de fecha de nacimiento razonable:
 * no futura, no anterior a {@code maxAgeYears} años, y edad mínima {@code minAge}
 * (0 desactiva la comprobación de edad mínima). No aplica {@code @NotNull}: un valor
 * nulo se considera válido, combinar con {@code @NotNull} cuando el campo es obligatorio.
 */
@Documented
@Constraint(validatedBy = BirthDateValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBirthDate {

    String message() default "La fecha de nacimiento no es válida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Edad mínima requerida en años. 0 desactiva la comprobación. */
    int minAge() default 0;

    /** Antigüedad máxima aceptada en años. */
    int maxAgeYears() default 120;
}
