# Proyecto WEB-ARTISYNC

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.PENDING.svg)](https://doi.org/10.5281/zenodo.PENDING)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/badge/version-v0.9.0--rc-blue)](https://github.com/JohanCarvajal04/Proyecto-WEB-ARTISYNC)

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
