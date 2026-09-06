package uteq.edu.ec.artisync.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BirthDateValidatorTest {

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    private final BirthDateValidator validator = new BirthDateValidator();

    @BeforeEach
    void setUp() {
        given(context.buildConstraintViolationWithTemplate(org.mockito.ArgumentMatchers.anyString())).willReturn(builder);
    }

    private void inicializar(int minAge, int maxAgeYears) {
        ValidBirthDate anotacion = new ValidBirthDate() {
            @Override public String message() { return "invalida"; }
            @Override public Class<?>[] groups() { return new Class<?>[0]; }
            @Override public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
            @Override public int minAge() { return minAge; }
            @Override public int maxAgeYears() { return maxAgeYears; }
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return ValidBirthDate.class; }
        };
        validator.initialize(anotacion);
    }

    @Test
    @DisplayName("null es valido (se combina con @NotNull cuando el campo es obligatorio)")
    void isValid_null_esValido() {
        inicializar(0, 120);

        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    @DisplayName("fecha futura no es valida")
    void isValid_fechaFutura_noEsValida() {
        inicializar(0, 120);

        assertThat(validator.isValid(LocalDate.now().plusDays(1), context)).isFalse();
    }

    @Test
    @DisplayName("fecha mas antigua que maxAgeYears no es valida")
    void isValid_masAntiguaQueMaxAgeYears_noEsValida() {
        inicializar(0, 120);

        assertThat(validator.isValid(LocalDate.now().minusYears(121), context)).isFalse();
    }

    @Test
    @DisplayName("no alcanza la edad minima requerida")
    void isValid_noAlcanzaEdadMinima_noEsValida() {
        inicializar(18, 120);

        assertThat(validator.isValid(LocalDate.now().minusYears(10), context)).isFalse();
    }

    @Test
    @DisplayName("edad minima desactivada (0) acepta cualquier fecha razonable")
    void isValid_edadMinimaDesactivada_aceptaFechaReciente() {
        inicializar(0, 120);

        assertThat(validator.isValid(LocalDate.now().minusYears(1), context)).isTrue();
    }

    @Test
    @DisplayName("fecha razonable que cumple la edad minima es valida")
    void isValid_cumpleEdadMinima_esValida() {
        inicializar(18, 120);

        assertThat(validator.isValid(LocalDate.now().minusYears(30), context)).isTrue();
    }
}
