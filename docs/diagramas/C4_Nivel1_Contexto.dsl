Nivel 1: Contexto del Sistema (System Context) - Artisync PFC

Este documento describe el **Nivel 1 (Contexto del Sistema)** de la metodología **C4 Model** (desarrollada por Simon Brown) para la **Plataforma de Gestión para Artistas y Creadores de Contenido (Artisync / PFC)**.

El diagrama de contexto sitúa al sistema **Artisync** como una **caja negra** central y muestra los límites globales del sistema, identificando a los **actores (usuarios que interactúan)** y los **sistemas y servicios externos** con los que el sistema se comunica para cumplir con sus requisitos de negocio.

---

## 1. Identificación de Elementos del Nivel 1

### 1.1 El Sistema Central (Caja Negra)
| Sistema | Descripción y Alcance |
| :--- | :--- |
| **Artisync (Plataforma PFC)** | Plataforma web integral y colaborativa que conecta a artistas creativos y creadores de contenido con clientes, gestionando catálogos de servicios, cotizaciones personalizadas, flujos de trabajo en hitos, contratos legales, depósitos en garantía (escrow), comunidad social y comunicación en tiempo real. |

---

### 1.2 Usuarios y Actores (People / Actors)
| Actor / Rol | Tipo | Descripción y Responsabilidades en el Sistema |
| :--- | :--- | :--- |
| **Cliente / Buscador de Talento** | Usuario Final | Persona física, marca o empresa interesada en buscar, explorar y contratar servicios creativos. Explora portafolios verificados, solicita cotizaciones, contrata proyectos, realiza depósitos en garantía, aprueba hitos de trabajo y califica los entregables finales. |
| **Artista / Creador de Contenido** | Usuario Final / Creativo | Profesional creativo independiente (ilustrador, músico, editor, diseñador, animador) que se registra en la plataforma, verifica su perfil, publica su portafolio y catálogo de servicios con tarifas, gestiona pedidos activos, sube entregables por hito y recibe pagos liberados. |
| **Administrador de la Plataforma** | Usuario Interno / Moderador | Personal técnico o administrativo de Artisync con acceso privilegiado. Gestiona el alta y bloqueo de cuentas de usuario, administra roles y permisos granulares (