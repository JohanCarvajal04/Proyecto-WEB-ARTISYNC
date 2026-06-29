# Proyecto WEB-ARTISYNC

# Registro
POST http://localhost:8080/api/auth/registro

{
"nombres": "Juan",
"apellidos": "Pérez",
"correo": "juan@example.com",
"contrasena": "MiPassword123"
}

# Login
POST http://localhost:8080/api/auth/login
{
"correo": "juan@example.com",
"contrasena": "MiPassword123"
}

# Logout
POST http://localhost:8080/api/auth/logout
Authorization: Bearer <tu_token>
