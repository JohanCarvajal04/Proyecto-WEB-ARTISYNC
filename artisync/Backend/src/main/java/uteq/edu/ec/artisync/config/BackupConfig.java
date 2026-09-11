package uteq.edu.ec.artisync.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.jdbc.health.DataSourceHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.concurrent.Executor;

/**
 * Infraestructura propia del módulo de respaldos: un DataSource dedicado
 * (conecta como artisync_backup, separado del pool principal de la app que
 * conecta como artisync_app) para el COPY incremental, y un executor acotado
 * para las ejecuciones @Async de respaldo.
 *
 * No existe hoy ningún AsyncConfigurer/ThreadPoolTaskExecutor propio en el
 * proyecto (@Async de EmailService usa el SimpleAsyncTaskExecutor ilimitado
 * por defecto de Spring). Para respaldos eso es un riesgo real y nuevo que el
 * envío de correos nunca tuvo: dos pg_dump/COPY concurrentes competirían por
 * I/O de disco y carga de BD. Un executor de 1 hilo serializa las ejecuciones.
 */
@Configuration
@RequiredArgsConstructor
public class BackupConfig {

    private final BackupProperties respaldoProperties;

    /**
     * CRÍTICO: declarar cualquier otro bean DataSource (respaldoDataSource,
     * más abajo) desactiva por completo el DataSourceAutoConfiguration de
     * Spring Boot -- su @ConditionalOnMissingBean(DataSource.class) deja de
     * cumplirse en cuanto existe OTRO DataSource en el contexto -- así que el
     * datasource principal basado en spring.datasource.* (artisync_app)
     * jamás se crea, y todo autowiring de DataSource sin calificador
     * (incluida la EntityManagerFactory de JPA que usa cada repositorio del
     * proyecto, login incluido) cae sobre el único DataSource que encuentre.
     * Comprobado en pg_stat_activity: sin este bean, la app entera termina
     * conectada como el rol de SOLO LECTURA artisync_backup (cero conexiones
     * artisync_app), y cualquier login falla con "permission denied" al no
     * poder ni insertar en auditoria_eventos. Este bean reconstruye
     * exactamente el datasource que DataSourceAutoConfiguration habría
     * creado y lo marca @Primary para que gane sobre respaldoDataSource en
     * cualquier punto de inyección sin @Qualifier.
     */
    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name}") String driverClassName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setPoolName("artisync-app-pool");
        return new HikariDataSource(config);
    }

    @Bean(name = "respaldoDataSource")
    public DataSource respaldoDataSource() {
        BackupProperties.Db db = respaldoProperties.getDb();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://%s:%d/%s".formatted(db.getHost(), db.getPuerto(), db.getNombre()));
        config.setUsername(db.getUsuario());
        config.setPassword(db.getPassword());
        config.setMaximumPoolSize(2);
        config.setPoolName("respaldo-pool");
        // Sin fail-fast: a diferencia del datasource principal (arriba), este
        // pool es de un subsistema secundario (respaldos) que no debe impedir
        // que arranque el resto de la app si Postgres aún no está listo o no
        // es alcanzable -- p. ej. en tests unitarios (src/test/resources/
        // application.properties usa H2 y no define respaldo.db.*, así que
        // este bean se construiría con host/password vacíos/por defecto).
        // Sin esto, HikariCP valida una conexión al crear el pool y aborta el
        // arranque de TODO el ApplicationContext ante cualquier fallo.
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }

    /**
     * Spring Boot registra por defecto un HealthContributor por cada bean
     * DataSource del contexto (DataSourceHealthContributorAutoConfiguration,
     * @ConditionalOnMissingBean(name = {"dbHealthIndicator",
     * "dbHealthContributor"})). Sin este bean, respaldoDataSource entraría
     * también en /actuator/health: un pg_dump/respaldo temporalmente
     * inalcanzable haría que TODA la aplicación reporte DOWN (503), aunque el
     * resto siga funcionando con normalidad. Al declarar aquí el bean
     * "dbHealthContributor" a mano, el autoconfigurado de Spring Boot se
     * desactiva (mismo mecanismo @ConditionalOnMissingBean que ya obliga a
     * declarar el datasource principal arriba) y el health check vuelve a
     * cubrir solo el datasource principal, como antes de este módulo.
     */
    @Bean(name = "dbHealthContributor")
    public HealthContributor dbHealthContributor(DataSource dataSource) {
        return new DataSourceHealthIndicator(dataSource);
    }

    @Bean(name = "respaldoTaskExecutor")
    public Executor respaldoTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("respaldo-async-");
        executor.initialize();
        return executor;
    }
}
