# AGENTS.md

Sistema de pedidos anticipados para la cafetería del ITSH. Proyecto académico en equipo, guiado por especificaciones (Spec-Driven Development).

## Fuentes de verdad (en este orden)

1. `.specify/memory/contitution.md` — constitución del proyecto (el nombre real del archivo tiene el typo `contitution`).
2. `docs/AGENTS.md` — reglas detalladas: proceso, seguridad, roles, flujos de pedido.
3. `specs/` — especificación del módulo a implementar. **Si no existe spec, no implementar.**
4. Código existente.

Si un documento contradice al código, informar la diferencia y esperar decisión.

## Reglas que cambian cómo trabajas

- **Idioma obligatorio:** todo código, nombres de variables/clases/métodos, comentarios, mensajes de usuario y documentación en español (nombres técnicos de tecnologías se conservan en inglés).
- Presentar plan y esperar aprobación antes de implementar. No inventar requisitos ni reglas de negocio.
- Sin autorización: cambiar arquitectura o tecnologías, añadir dependencias, alterar estados/transiciones de pedidos, borrar datos o volúmenes de Docker, cambiar la estrategia de migraciones o el mecanismo de autenticación existentes.
- Nunca citar ni commitar el contenido de `database/.env`.

## Estado real del repositorio

- `backend/`: Spring Boot **4.1.2** + Java 17. Estructura de paquetes implementada
  (`config/ controller/ service/ repository/ entity/ dto/ security/ exception/`).
  Módulos completos: autenticación/usuarios (Sprint 0), menú de
  categorías/productos (Sprint 1) y pedidos de creación/consulta (Sprint 2); los demás módulos aún no existen.
- `frontend/`: **vacío** — el proyecto Flutter todavía no se creó. Los comandos de Flutter más abajo aplican cuando exista.
- `specs/`: `autenticacion-usuarios/` (Sprint 0), `menu/` (Sprint 1), `pedidos/` (Sprint 2). `docs/`: `AGENTS.md`, `bitacora-desarrollo.md`, `sprints.md` (plan por sprints), `guia-colaborador.md` (estado del sistema y patrones del código), PDFs de pantallas.
- Sin CI, sin linter, sin formatter, sin codegen. No inventar comandos de lint que no existen.
- Avance y decisiones registrados en `docs/bitacora-desarrollo.md` (revisar antes de continuar).

## Backend — comandos

Ejecutar desde `backend/` en Windows con el wrapper:

```cmd
mvnw.cmd test        # pruebas
mvnw.cmd package     # empaquetar
mvnw.cmd spring-boot:run   # arranca en :8081
mvnw.cmd test -Dtest=BackendApplicationTests   # una sola prueba
```

### Requisitos previos (los tests los exigen)

1. MySQL arriba: `docker compose up -d` desde `database/` (contenedor `cafeitsh-mysql`, puerto de host **3307**, no 3306).
2. Variables de entorno en la shell antes de cualquier comando Maven:
   - `DB_PASSWORD` — valor en `database/.env`. **Sin ella todos los tests fallan.**
   - `JWT_SECRET` — secreto de firma JWT, mínimo 32 caracteres.
     **Requerida tanto para correr la app como para `mvnw.cmd test`**: los tests
     cargan el contexto completo y sin ella fallan con
     "Could not resolve placeholder 'JWT_SECRET'".
   - Opcionales: `ADMIN_CORREO` y `ADMIN_PASSWORD` para crear el primer administrador
     al arrancar con la tabla vacía (si no, solo se registra un aviso).
3. `spring.jpa.hibernate.ddl-auto: validate` contra MySQL real: no hay H2 ni Testcontainers.
   El esquema lo gestiona **Flyway** (migraciones en `backend/src/main/resources/db/migraciones/`);
   nunca editar una migración ya aplicada, siempre crear V(n+1).

Verificación obligatoria antes de dar una tarea por terminada: `mvnw.cmd test` y `mvnw.cmd package`. No afirmar que pasaron sin ejecutarlos.

## Frontend (cuando exista)

Desde `frontend/`: `flutter pub get`, `flutter run`, `flutter analyze`, `flutter test`, `dart format .`
URL del backend: `http://localhost:8081`; emulador Android: `http://10.0.2.2:8081`. Centralizar la URL de la API en un solo lugar.

## Base de datos

- Orquestada con `database/docker-compose.yml` + `database/.env` → MySQL 8, BD `cafeitsh_db`, usuario `cafe_admin` (la aplicación nunca usa root).
- Flyway ya está integrado (Sprint 0): todo cambio de esquema es una migración
  nueva `V(n+1)__descripcion.sql` en `backend/src/main/resources/db/migraciones/`,
  documentada en la bitácora. Nunca editar una migración ya aplicada.

## Ciclo por tarea

1. Leer spec y documentos relacionados → 2. revisar código existente → 3. proponer plan y archivos a modificar → 4. esperar aprobación → 5. implementar → 6. ejecutar pruebas → 7. informar archivos cambiados, pruebas ejecutadas con su resultado y pendientes.

Reglas de negocio recordatorio (detalle en `docs/AGENTS.md`): pedidos anticipados del mismo día (`America/Mexico_City`) dentro del horario de servicio configurable (apertura/cierre, tiempo de preparación y margen de cierre; la hora de entrega no exige alineación a grilla), inmutables al crearse en `PENDIENTE` (Sprint 2 sin transiciones de estado); estados Pendiente → Pagado → En preparación → Listo → Entregado, más Cancelado. La autorización por roles se verifica en el backend, nunca solo ocultando pantallas en Flutter.
