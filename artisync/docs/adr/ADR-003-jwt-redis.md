# ADR-003: Uso de Redis para blacklist de tokens JWT

## Estado
Aceptado

## Contexto
JWT es un mecanismo de autenticación *stateless*: el servidor no almacena el
token después de emitirlo. Esto implica que, si un usuario cierra sesión o un
token es comprometido, el servidor no tiene forma nativa de invalidarlo antes
de su expiración natural.

## Decisión
Se utiliza Redis como almacén en memoria para mantener una lista de revocación
(blacklist) de los `jti` (JWT ID) de los tokens invalidados. Cada entrada se
guarda con un TTL igual al tiempo de vida restante del token, de modo que
Redis libera la entrada automáticamente cuando el token ya habría expirado
de todas formas.

En cada solicitud protegida, `JwtAuthFilter` consulta Redis antes de
autorizar el acceso.

## Alternativas consideradas
- **Base de datos relacional para blacklist**: descartada por latencia; cada
  request protegida requeriría una consulta SQL adicional.
- **Tokens de muy corta duración sin blacklist**: descartada porque no
  resuelve el caso de logout explícito ni el de robo de token detectado.

## Consecuencias
- (+) Logout y revocación inmediata de tokens comprometidos.
- (+) Consulta O(1) en Redis, bajo impacto en latencia.
- (-) Introduce una dependencia de infraestructura adicional (Redis) que debe
  estar disponible para que el sistema de autenticación funcione.
