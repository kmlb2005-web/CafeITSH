# cafeITSH ☕

Sistema de pedidos anticipados para la cafetería del Instituto Tecnológico
Superior de Huauchinango (ITSH).

Proyecto académico desarrollado en equipo, guiado por especificaciones
(Spec-Driven Development).

## Estado actual

| Sprint | Módulo | Estado |
|---|---|---|
| 0 | Flyway, seguridad JWT y autenticación/usuarios | ✅ Completado |
| 1 | Menú: categorías y productos | ✅ Completado |
| 2 | Pedidos (horario/ventana configurable; sin transiciones en este sprint) | ✅ Completado |
| 3 | Caja (pago, cancelación, corte) | ✅ Completado |
| 4 | Cocina (En preparación → Listo → Entregado) | ✅ Completado |
| 5 | Cierre (documentación de endpoints y revisión de CORS) | ⏳ Pendiente |

El plan del backend está en [docs/sprints.md](docs/sprints.md). El frontend
se abordará después del Sprint 5 y requerirá su propia spec aprobada.

## Tecnologías

| Componente | Tecnología |
|---|---|
| Backend | Java 17 + Spring Boot 4.1.2 (Maven Wrapper) |
| Base de datos | MySQL 8 en Docker Compose; migraciones con Flyway |
| Autenticación | JWT (jjwt 0.13.0), contraseñas con BCrypt |
| Frontend | Flutter/Dart (pendiente de crear) |

## Estructura del proyecto

```text
cafeITSH/
├── AGENTS.md            # Reglas operativas del proyecto (léer antes de trabajar)
├── README.md
├── docs/                # Reglas, plan de sprints, bitácora, PDFs de pantallas
├── specs/               # Especificación de cada módulo (fuente de verdad)
├── .specify/memory/     # Constitución del proyecto
├── database/            # docker-compose.yml + .env (este último NUNCA se sube)
├── backend/             # API REST de Spring Boot (puerto 8081)
└── frontend/            # Proyecto Flutter (pendiente de crear)
```

## Requisitos

- Git
- Java 17
- Docker Desktop
- (Más adelante) Flutter SDK, para el frontend

## Puesta en marcha

### 1. Clonar el repositorio

```cmd
git clone https://github.com/kmlb2005-web/CafeITSH.git
cd CafeITSH
```

### 2. Configurar la base de datos

```cmd
cd database
copy .env.example .env
rem Edita .env y pon tus valores reales (no lo subas a Git)
docker compose up -d
```

Esto levanta MySQL 8 en el puerto **3307** con la base `cafeitsh_db`
(usuario `cafe_admin`, nunca root desde la aplicación).

### 3. Variables de entorno del backend

En la misma ventana de terminal antes de correr Maven:

```cmd
set DB_PASSWORD=<valor de MYSQL_PASSWORD de tu database/.env>
set JWT_SECRET=<secreto de al menos 32 caracteres>
```

Opcionales para crear el primer ADMIN al arrancar con la tabla vacía:
`ADMIN_CORREO` y `ADMIN_PASSWORD`.

### 4. Arrancar el backend

```cmd
cd backend
mvnw.cmd spring-boot:run
```

La API queda disponible en `http://localhost:8081`.

### 5. Pruebas (obligatorio antes de cerrar una tarea)

```cmd
cd backend
mvnw.cmd test
mvnw.cmd package
```

Requieren MySQL arriba (paso 2) y las variables del paso 3.

## Trabajo en equipo por ramas

El modelo es **`main` + una rama por sprint**:

- `main` = código estable; siempre pasa `mvnw.cmd test` y `mvnw.cmd package`.
- Cada sprint se desarrolla en su propia rama `sprint-N-nombre`
  (por ejemplo `sprint-2-carrito`).

Flujo por sprint:

1. `git pull origin main` — siempre empezar de lo más reciente.
2. Leer `docs/sprints.md` (el plan) y `specs/<tu-sprint>/spec.md`.
3. **Sin spec aprobada no se implementa.**
4. `git checkout -b sprint-N-nombre origin/main`
5. Implementar y ejecutar `mvnw.cmd test` + `mvnw.cmd package`.
6. `git add .` → `git commit -m "Sprint N: <descripción>"` →
   `git push -u origin sprint-N-nombre`
7. Integrar a `main` (merge o PR) y actualizar `docs/bitacora-desarrollo.md`.

### Reglas duras del repositorio

- **Nunca subir `database/.env`** ni ningún secreto (excluido en `.gitignore`).
- Código, comentarios y documentación en **español**.
- No editar migraciones de Flyway ya aplicadas: siempre crear una `V(n+1)` nueva.
- Consultar `AGENTS.md` (raíz) y `docs/AGENTS.md` para el detalle de las reglas.

## Documentación

- [AGENTS.md](AGENTS.md) — estado real del repo y comandos rápidos
- [docs/AGENTS.md](docs/AGENTS.md) — reglas detalladas del proyecto
- [docs/sprints.md](docs/sprints.md) — plan acordado de sprints (compartido por todo el equipo)
- [docs/bitacora-desarrollo.md](docs/bitacora-desarrollo.md) — historial de sprints cerrados
- [specs/](specs/) — especificaciones funcionales
- `.specify/memory/contitution.md` — constitución del proyecto
