# Guion de Exposición: Seguridad de BD ArtiSync

## 1. Portada
**[Diapositiva: Portada]**
"Buenos días a todos (o jurado/profesor). Nosotros somos el equipo conformado por Niurca Bone, Johan Carvajal, Bryan Figueroa y Jhon Rios. El día de hoy vamos a presentar nuestro Análisis de Seguridad a nivel de Base de Datos enfocado en los Usuarios, Roles y Privilegios en PostgreSQL, aplicado a la plataforma ArtiSync."

## 2. Índice
**[Diapositiva: Contenido]**
"Para esta presentación, abarcaremos los siguientes puntos: comenzaremos con una introducción a ArtiSync y los objetivos de nuestro análisis. Luego, revisaremos la estructura de la base de datos, la identificación de los usuarios, y cómo estructuramos los roles bajo el modelo RBAC. Posteriormente, detallaremos los privilegios del sistema y sobre objetos, mostraremos fragmentos clave del código SQL, las técnicas de endurecimiento (hardening) aplicadas, la estrategia de seguridad en doble capa, y finalmente, nuestras conclusiones."

## 3. Introducción
**[Diapositiva: ¿Qué es ArtiSync?]**
"Para entrar en contexto, ¿qué es ArtiSync? Es una plataforma web de marketplace creativo. Su objetivo principal es conectar a artistas digitales con clientes que buscan servicios creativos personalizados. 
A nivel tecnológico, es robusta: utilizamos Spring Boot 3.2 en el backend, Angular 17 en el frontend, y PostgreSQL 16 para nuestra base de datos. Todo esto gestionado mediante 7 módulos principales que van desde la seguridad y gestión de perfiles, hasta la logística de pedidos, chat y pasarelas de pago (Escrow)."

**[Diapositiva: Objetivos del Análisis]**
"El objetivo central de nuestro análisis es diseñar e implementar un modelo de seguridad robusto directamente en el motor de base de datos. 
Nos basamos en un pilar fundamental: **el Principio de Mínimo Privilegio (PoLP)**. Esto significa que cada usuario y cada rol de nuestro sistema va a recibir estrictamente los permisos necesarios para poder trabajar, ni uno más ni uno menos. Para lograr esto, identificamos a nuestros usuarios, definimos roles, y asignamos privilegios tanto a nivel de sistema como a nivel de tablas específicas."

## 4. Base de Datos
**[Diapositiva: Base de Datos: 37 Tablas en 7 Módulos]**
"Nuestra base de datos es compleja y cuenta con 37 tablas divididas en los 7 módulos del negocio. Por ejemplo, en el módulo de Seguridad tenemos las tablas de roles y sesiones; en Pedidos gestionamos flujos de trabajo; y en Finanzas manejamos contratos y transacciones. Proteger cada módulo de accesos no autorizados es vital."

**[Diapositiva: Creación de la Base de Datos]**
"Iniciamos con la creación de la base de datos `artisyncbd`, asegurando una codificación estándar en UTF-8 y estableciendo comentarios descriptivos para mantener una excelente documentación dentro del propio gestor."

## 5. Usuarios
**[Diapositiva: Usuarios del Sistema]**
"Al analizar las necesidades del sistema, identificamos 9 tipos de usuarios en total. Se dividen en dos grandes grupos:
1. **Usuarios de negocio (6):** Como el Administrador, el Creador (artista), el Cliente, el Moderador (que revisa contenido), el personal de Soporte y el Auditor Financiero.
2. **Usuarios técnicos (3):** La propia Aplicación Backend (Spring Boot), un usuario para tareas de Backup automatizado y un usuario de Solo Lectura para herramientas de analítica y reportes como Grafana."

## 6. Definición de Roles
**[Diapositiva: Modelo RBAC: Roles de Grupo y Usuarios de Login]**
"Para manejar esta diversidad sin volver caótica la administración, aplicamos el modelo RBAC (Control de Acceso Basado en Roles).
En PostgreSQL creamos dos tipos de entidades:
- **Roles de Grupo (sin acceso de inicio de sesión o NOLOGIN):** Son contenedores lógicos de permisos, como `rol_app_backend` o `rol_moderador`.
- **Usuarios de Login:** Son las cuentas reales con contraseñas seguras, como `artisync_app` o `artisync_soporte`. A estos usuarios simplemente se les hereda (GRANT) el rol de grupo que les corresponde."

**[Diapositiva: Diagrama de Jerarquía de Roles]**
"Aquí podemos ver visualmente nuestra jerarquía. Noten cómo los usuarios de login en la parte inferior heredan los permisos de sus respectivos roles de grupo. Destaca que el `rol_administrador` hereda todos los permisos operativos del `rol_app_backend` y añade sus propios privilegios de administración."

**[Diapositivas de Código: Roles de Grupo y Usuarios de Login]**
"*(Brevemente)* Aquí mostramos la sintaxis SQL. Creamos los roles con cláusulas como `NOLOGIN`, `NOSUPERUSER` y `NOCREATEDB` para asegurar que, por defecto, nadie tenga poder administrativo extra. Luego, creamos las cuentas de conexión asigando límites de conexiones simultáneas y fechas de expiración de validez para mayor seguridad."

## 7. Privilegios
**[Diapositiva: Privilegios del Sistema por Rol]**
"A nivel global o de sistema, fuimos muy restrictivos. Solo el Administrador puede crear bases de datos y roles. Al usuario de Backup le dimos el rol predefinido `pg_read_all_data` (necesario en PostgreSQL 16 para volcados completos) y el resto solo tiene permisos para conectarse (`LOGIN` y `CONNECT`)."

**[Diapositivas: Privilegios sobre Objetos]**
"A nivel de tablas (Objetos), la segregación es granular:
- El **Backend** tiene control DML (Select, Insert, Update, Delete) sobre todas las tablas porque es el ORM quien gestiona la data transaccional.
- El **Administrador** añade poderes DDL (crear y alterar estructuras).
- El **Moderador** solo puede leer perfiles y servicios, pero solo puede editar y borrar comentarios o sorteos.
- **Soporte y el Auditor** son roles estrictamente de lectura (`SELECT`), pero segmentados: soporte ve tickets y chats, mientras que el auditor solo ve contratos y pagos."

**[Diapositiva: Matriz Resumen de Privilegios]**
"Esta matriz resume perfectamente nuestro trabajo: se ve claramente como un Auditor no tiene acceso a los módulos de Catálogo o Comunicación, y cómo el Backend sí interactúa con todos los módulos."

**[Diapositivas de Código: Privilegios]**
"*(Brevemente)* En código, logramos esto revocando primero todo el acceso al rol `PUBLIC` (una buena práctica crucial). Luego fuimos otorgando permisos tabla por tabla según el rol, e incluso usamos `ALTER DEFAULT PRIVILEGES` para asegurar que las tablas que se creen a futuro mantengan este esquema de seguridad."

## 8. Hardening (Endurecimiento)
**[Diapositiva: Hardening: Revocación de Privilegios]**
"No nos quedamos solo con asignar permisos. Aplicamos *Hardening*. Revocamos el privilegio de conexión a todo el mundo y lo habilitamos solo a quienes debían. Además, bloqueamos el comando `TRUNCATE` en el backend y moderación para prevenir eliminaciones masivas de información (accidentales o maliciosas)."

## 9. Arquitectura de Seguridad
**[Diapositiva: Relación: Roles de BD vs Roles de Aplicación]**
"Es importante mencionar que ArtiSync funciona con seguridad en **doble capa**.
- En la **Capa de Aplicación** (con Spring Security y JWT), validamos qué puede hacer el cliente o el artista (ej. 'editar MIS servicios').
- En la **Capa de Base de Datos** (lo que expusimos hoy), aseguramos que, incluso si la API es vulnerada, un atacante que obtenga las credenciales del Moderador jamás podrá modificar una tabla de finanzas, estableciendo una sólida defensa perimetral de datos."

## 10. Conclusiones
**[Diapositiva: Conclusiones]**
"En conclusión:
1. Logramos perfilar correctamente **9 tipos de usuarios**.
2. Diseñamos **7 roles de grupo** con jerarquías claras.
3. Aseguramos el sistema revocando accesos administrativos innecesarios.
4. Cumplimos a cabalidad el **principio de mínimo privilegio**, blindando cada módulo del sistema de manera independiente.
5. Dejamos un sistema preparado para el entorno real, protegido tanto en la API como en la capa física de los datos.

Muchas gracias por su atención. Estamos abiertos a cualquier pregunta."

---
*Nota:* Distribución recomendada para 4 integrantes:
- **Integrante 1:** Introducción, BD y Objetivos (Diapositivas 1 a 4).
- **Integrante 2:** Identificación de Usuarios, Modelo RBAC y Jerarquía (Diapositivas 5 a 8).
- **Integrante 3:** Privilegios de Sistema, de Objetos y Matriz (Diapositivas 9 a 16).
- **Integrante 4:** Código SQL, Hardening, Doble Capa y Conclusiones (Diapositivas 17 al final).
