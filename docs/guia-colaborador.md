# Guía de colaborador — cafeITSH

Guía rápida para quien se une al proyecto o va a ejecutar un sprint. Complementa
`AGENTS.md` (raíz) y `docs/AGENTS.md` (reglas detalladas); **en caso de
contradicción, esos documentos mandan**.

---

## 1. Prioridad de consulta

1. `AGENTS.md` (raíz) — estado actual del proyecto.
2. `docs/AGENTS.md` — reglas detalladas (proceso, seguridad, roles, pedidos).
3. `specs/<modulo>/spec.md` — especificación del módulo. **Si no existe spec aprobada, no se implementa.**
4. Código existente.
5. Decisiones previamente documentadas (`docs/bitacora-desarrollo.md`, `docs/sprints.md`).

Si un documento contradice al código, **informar la diferencia y esperar decisión**.

## 2. Estado actual del sistema (20/11/2026)

### Stack

| Componente | Tecnología |
|---|---|
| Backend | Spring Boot 4.1.2, Java 17, Maven (`mvnw.cmd`) |
| Seguridad | Spring Security 7 + JWT (jjwt 0.13.0), BCrypt |
| BD | MySQL 8 en Docker (contenedor `cafeitsh-mysql`, host **3307**) |
| Migraciones | Flyway (`backend/src/main/resources/db/migraciones/`) |
| Frontend | Flutter (aún no creado) |

### Sprints completados

- **Sprint 0** ✅ — base transversal: Flyway, seguridad JWT, autenticación y usuarios.
- **Sprint 1** ✅ — menú: categorías y productos.
- **Sprint 2** ✅ — pedidos: creación y consulta (horario/ventana configurable; transiciones diferidas).
- **Sprint 3** ✅ — caja: pago, cancelación y corte básico.
- **Sprints 4–5** pendientes — ver alcance en docs/sprints.md.

### Tablas y migraciones

| Migración | Tablas / cambios |
|---|---|
| V1__crear_tabla_usuarios.sql | usuarios |
| V2__crear_tablas_menu.sql | categorias, productos |
| V3__crear_tablas_pedidos.sql | pedidos, pedido_items |
| V4__crear_tabla_pagos_y_auditoria_cancelacion.sql | pagos y auditoría de cancelación/reembolso en pedidos |
### Roles

`Rol`: **ADMIN, CAJA, COCINA, CLIENTE** (catálogo fijo en `entity/Rol.java`).

### Endpoints existentes

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/auth/registro` | público (registro de cliente) |
| POST | `/api/auth/login` | público → token JWT |
| GET | `/api/auth/me` | autenticado |
| GET | `/api/usuarios` | ADMIN |
| POST | `/api/usuarios` | ADMIN |
| PUT | `/api/usuarios/{id}` | ADMIN |
| GET | `/api/menu/categorias` | público (solo categorías activas) |
| GET | `/api/menu/productos` (`?categoria=id`) | público (solo productos disponibles) |
| GET | `/api/categorias` | ADMIN (lista completa) |
| POST | `/api/categorias` | ADMIN → 201 |
| PUT | `/api/categorias/{id}` | ADMIN |
| PUT | `/api/categorias/{id}/desactivar` | ADMIN |
| GET | `/api/productos` | ADMIN (lista completa) |
| POST | `/api/productos` | ADMIN → 201 |
| PUT | `/api/productos/{id}` | ADMIN |
| PUT | `/api/productos/{id}/desactivar` | ADMIN |
| POST | `/api/pedidos` | CLIENTE (crea pedido en `PENDIENTE`) |
| GET | `/api/pedidos` | autenticado (CLIENTE: propios; CAJA/COCINA/ADMIN: todos) |
| GET | `/api/pedidos/{id}` | autenticado (CLIENTE solo dueño) |
| POST | `/api/pedidos/{id}/pago` | CAJA/ADMIN (registra pago, `PENDIENTE -> PAGADO`) |
| POST | `/api/pedidos/{id}/cancelar` | autenticado (CLIENTE propio `PENDIENTE`; CAJA/ADMIN `PENDIENTE` o `PAGADO` antes de `EN_PREPARACION`) |
| GET | `/api/caja/corte?fecha=YYYY-MM-DD` | CAJA/ADMIN (corte diario, zona `America/Mexico_City`) |
| GET | `/api/prueba` | público (endpoint de prueba heredado; no eliminar) |

> Pedidos: sin endpoints de edición ni DELETE. Sprint 3 agrega pago/cancelación/corte; transiciones de cocina/entrega quedan para Sprint 4. Entrega validada en `America/Mexico_City` dentro del horario de servicio configurable (`cafeitsh.horario-apertura`, `cafeitsh.horario-cierre`, `cafeitsh.tiempo-preparacion-minutos`, `cafeitsh.margen-cierre-minutos`; iniciales `08:00`, `20:00`, `15`, `15`), sin exigir alineación a grilla.

### Pruebas

- **83 pruebas** en verde (`mvnw.cmd test`) contra la BD real — no hay H2 ni Testcontainers.
- Archivos: `ControladorAutenticacionTests` (9), `ControladorMenuTests` (13),
  `ControladorPedidoTests` (9), `ControladorCajaTests` (5), `ServicioJwtTests` (6), `ServicioCategoriaTests` (8),
  `ServicioProductoTests` (8), `ServicioPedidoTests` (17), `ServicioCajaTests` (7), `BackendApplicationTests` (1).

## 3. Cómo se trabaja (ciclo por sprint)

1. Leer la spec y documentos relacionados.
2. Revisar el código existente.
3. Proponer plan y archivos a modificar → **esperar aprobación**.
4. Implementar (solo lo aprobado).
5. Ejecutar `mvnw.cmd test` y `mvnw.cmd package`.
6. Informar archivos cambiados y resultados de pruebas.
7. Actualizar `docs/bitacora-desarrollo.md` (sección del sprint) y el estado en `AGENTS.md`.

Antes de redactar cada spec, resolver las **preguntas abiertas** de `docs/sprints.md`
con el patrocinador: las reglas de negocio no se inventan.

## 4. Cómo ejecutar

Desde `database/` (una vez):

```cmd
docker compose up -d
```

Variables de entorno en la shell **antes** de cualquier comando Maven (desde `backend/`):

| Variable | Cuándo | Detalle |
|---|---|---|
| `DB_PASSWORD` | siempre | valor de `database/.env`. **Sin ella todos los tests fallan.** |
| `JWT_SECRET` | siempre | ≥32 caracteres. Requerida para correr la app y para `mvnw.cmd test`. |
| `ADMIN_CORREO`, `ADMIN_PASSWORD` | opcional | crean el primer admin al arrancar con la tabla `usuarios` vacía. |

```cmd
cd backend
mvnw.cmd test              # pruebas
mvnw.cmd package           # empaquetar
mvnw.cmd spring-boot:run   # arranca en http://localhost:8081
```

## 5. Patrones del código (reutilizar, no duplicar)

- **Capas:** `controller/` delgado → `service/` con las reglas de negocio →
  `repository/` → `entity/`. Los controladores no contienen lógica.
- **DTOs:** `Solicitud*` para entrada, `Respuesta*` para salida. Nunca devolver
  entidades de BD ni contraseñas/token.
- **Errores:** lanzar `ExcepcionNegocio(mensaje, estadoHttp, campos?)` desde los
  servicios; `ManejadorErrores` (`@RestControllerAdvice`) la traduce a
  `RespuestaError {mensaje, timestamp, campos}` en español, sin stack traces.
  Validaciones de entrada con `@Valid` → 400 con detalle por campo.
- **Seguridad:** rutas declaradas en `ConfiguracionSeguridad`
  (`requestMatchers` con `permitAll()` / `hasRole("ADMIN")`). El token se envía en
  `Authorization: Bearer ...`; se valida en `FiltroJwt` con `ServicioJwt`.
  **La autorización se verifica en el backend, nunca solo ocultando pantallas.**
- **Sin borrado físico:** desactivar con flags (`activo` / `activa` / `disponible`);
  no hay endpoints DELETE y no deben agregarse sin autorización.
- **Migraciones:** todo cambio de esquema = archivo nuevo `V(n+1)__descripcion.sql`
  en `backend/src/main/resources/db/migraciones/`. **Nunca editar una migración ya aplicada.**
- **Tests:** MockMvc + contexto completo contra MySQL real; los datos de prueba
  llevan sufijo único (`System.nanoTime()`); en tests, instanciar `ObjectMapper`
  directo (`new ObjectMapper()`) — Spring Boot 4 auto-configura Jackson 3, no hay
  bean Jackson 2.

## 6. Reglas no negociables (resumen)

- Todo en **español** (código, nombres, comentarios, mensajes, docs).
- Sin spec aprobada → no hay implementación; sin autorización → no se cambia el siguiente sprint.
- No cambiar arquitectura/tecnologías ni agregar dependencias sin autorización.
- No introducir datos de tarjetas bancarias; secretos solo en variables de entorno, nunca en Git.
- No borrar datos ni volúmenes de Docker.
- Nunca afirmar pruebas que no se ejecutaron (`mvnw.cmd test` + `mvnw.cmd package`).
- Reglas de negocio de pedidos (resumen en `docs/AGENTS.md` §9 y `specs/pedidos/spec.md`): pedido anticipado del **mismo día** (`America/Mexico_City`) dentro del **horario de servicio configurable** (apertura/cierre, tiempo de preparación/lead y margen antes del cierre; la hora no exige alineación a grilla), inmutable al crearse en `PENDIENTE` (Sprint 2 sin transiciones); estados **Pendiente → Pagado → En preparación → Listo → Entregado** más **Cancelado**; no modificar estados ni transiciones sin aprobación.

## 7. Dónde registrar el progreso

- **`docs/bitacora-desarrollo.md`** — una sección por sprint terminado (plantilla en `docs/sprints.md`).
- **`docs/sprints.md`** — plan general y alcance por sprint.
- Al terminar, sincronizar el estado en `AGENTS.md` (raíz).
