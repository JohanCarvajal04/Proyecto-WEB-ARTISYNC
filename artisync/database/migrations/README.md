# database/migrations/

Esta carpeta es la **referencia documental** de los scripts Flyway exigida por la
tabla de estructura del repositorio en la guía (sección 2.6.1).

La ubicación que Spring Boot **realmente ejecuta** es:
`backend/src/main/resources/db/migration/` (definida por
`spring.flyway.locations: classpath:db/migration` en `application.yml`).

Mantén ambos archivos sincronizados manualmente, o reemplaza esta carpeta por un
symlink si tu sistema operativo lo permite:

```bash
rm -rf database/migrations
ln -s ../backend/src/main/resources/db/migration database/migrations
```
