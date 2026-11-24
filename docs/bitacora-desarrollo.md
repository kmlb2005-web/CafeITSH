# Bitácora de desarrollo

Registro de los sprints terminados del backend. Cada sprint se cierra con
sus pruebas ejecutadas y queda pendiente de autorización para el siguiente.

Estado general: **Sprints 0, 1, 2 y 3 completados** · Sprint 4 implementado, pendiente de verificación completa · Frontend pendiente (sin proyecto Flutter aún).

---

## Sprint 0 — Base transversal: Flyway, seguridad JWT y autenticación (17/11/2026)

### Objetivo

Integrar Flyway, establecer la estructura de paquetes del backend, implementar
la autenticación con JWT y la gestión de usuarios administrados por rol ADMIN.

### Spec asociada

`specs/autenticacion-usuarios/spec.md` (creada en este sprint)

### Archivos creados

| Archivo | Contenido |
|---|---|
| `specs/autenticacion-usuarios/spec.md` | Especificación del módulo |
| `backend/src/main/resources/db/migraciones/V1__crear_tabla_usuarios.sql` | Migración inicial (tabla `usuarios`) |
| `backend/.../entity/Rol.java`, `entity/Usuario.java` | Entidad y catálogo de roles |
| `backend/.../repository/UsuarioRepository.java` | Acceso a datos |
| `backend/.../dto/` (7 archivos) | SolicitudRegistro, SolicitudLogin, SolicitudCreacionUsuario, SolicitudActualizacionUsuario, RespuestaUsuario, RespuestaLogin, RespuestaError |
| `backend/.../security/ServicioJwt.java` | Generación/validación de tokens (jjwt 0.13.0) |
| `backend/.../security/ServicioDetallesUsuario.java` | UserDetailsService con roles y cuenta desactivada |
| `backend/.../security/FiltroJwt.java` | Filtro Bearer por petición |
| `backend/.../config/ConfiguracionSeguridad.java` | SecurityFilterChain, CORS dev, BCrypt |
| `backend/.../config/InicializadorBaseDatos.java` | Crea el primer ADMIN si la tabla está vacía |
| `backend/.../service/ServicioAutenticacion.java` | Registro (solo CLIENTE) e inicio de sesión |
| `backend/.../service/ServicioUsuario.java` | CRUD de usuarios con reglas de negocio |
| `backend/.../controller/ControladorAutenticacion.java` | `/api/auth/registro`, `/login`, `/me` |
| `backend/.../controller/ControladorUsuario.java` | `/api/usuarios` (ADMIN) |
| `backend/.../exception/ExcepcionNegocio.java`, `ManejadorErrores.java` | Errores centralizados en español |
| `backend/src/test/.../security/ServicioJwtTests.java` | 6 pruebas unitarias de JWT |
| `backend/src/test/.../controller/ControladorAutenticacionTests.java` | 9 pruebas de integración MockMvc |

### Archivos modificados

- `backend/pom.xml`: `spring-boot-starter-security`, `spring-boot-starter-flyway`,
  `flyway-mysql`, `jjwt-api/impl/jackson 0.13.0` (justificación comentada en el pom).
- `backend/src/main/resources/application.yaml`: Flyway (`classpath:db/migraciones`),
  `jwt.secreto` (${JWT_SECRET}), `jwt.expiracion-minutos: 480`.

### Endpoints agregados

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/auth/registro` | público (rol CLIENTE) |
| POST | `/api/auth/login` | público |
| GET | `/api/auth/me` | autenticado |
| GET | `/api/usuarios` | ADMIN |
| POST | `/api/usuarios` | ADMIN |
| PUT | `/api/usuarios/{id}` | ADMIN |

### Pruebas ejecutadas

```cmd
cd backend
set DB_PASSWORD=...   (de database/.env)
set JWT_SECRET=...    (>=32 caracteres)
mvnw.cmd test      → Tests run: 16, Failures: 0, Errors: 0, Skipped: 0  BUILD SUCCESS
mvnw.cmd package   → BUILD SUCCESS (jar backend-0.0.1-SNAPSHOT.jar)
```

La migración `V1` se aplicó en la BD real (`flyway_schema_history.success = 1`).

### Decisiones tomadas

1. **Flyway desde el inicio** (aprobado por el equipo): migraciones en
   `src/main/resources/db/migraciones/`, nunca editar una migración ya aplicada.
2. **JWT con jjwt 0.13.0** (aprobado): secreto solo por variable de entorno
   `JWT_SECRET`, expiración 480 minutos.
3. **Primer admin** por `InicializadorBaseDatos`: solo si la tabla está vacía y
   existe `ADMIN_PASSWORD` (opcional `ADMIN_CORREO`, por defecto
   `admin@cafeitsh.mx`). Sin contraseña por defecto en el repositorio.
4. **Sin borrado físico de usuarios**: desactivación (`activo=false`); un ADMIN
   no puede desactivar su propia cuenta.
5. **Jackson**: Spring Boot 4 autoconfigura Jackson 3 (`tools.jackson`); en los
   tests se instancia Jackson 2 directamente (llega con `jjwt-jackson`).
6. **Registro público solo CLIENTE** → decisión **cerrada y aprobada** por el
   equipo: se mantiene el registro público de clientes (spec §4).

### Pendientes / notas para el siguiente sprint

- ~~Definir si se mantiene el registro público de clientes~~ → cerrado: se mantiene.
- CORS solo permite orígenes de desarrollo (`localhost:3000/5173/8080/8081`);
  restringir antes de producción.
- Los tests de integración crean usuarios de prueba con sufijo único; corren
  contra la BD real `cafeitsh_db` (no hay BD de pruebas separada ni Testcontainers).

---

## Sprint 1 — Menú: categorías y productos (19/11/2026)

### Objetivo

Exponer el menú público (categorías y productos visibles) y el CRUD
administrativo de categorías y productos, con reglas de disponibilidad y
sin borrado físico.

### Spec asociada

`specs/menu/spec.md` (creada y aprobada en este sprint; §5 se actualizó
durante la implementación para documentar los GET de administración).

### Archivos creados

| Archivo | Contenido |
|---|---|
| `specs/menu/spec.md` | Especificación del módulo |
| `backend/.../resources/db/migraciones/V2__crear_tablas_menu.sql` | Tablas `categorias` y `productos` |
| `backend/.../entity/Categoria.java`, `entity/Producto.java` | Entidades (`activa`, `disponible`) |
| `backend/.../repository/CategoriaRepository.java`, `ProductoRepository.java` | Acceso a datos y consultas JPQL del menú público |
| `backend/.../dto/` (7 archivos) | SolicitudCategoria, SolicitudActualizacionCategoria, SolicitudProducto, SolicitudActualizacionProducto, RespuestaCategoria, RespuestaCategoriaResumen, RespuestaProducto |
| `backend/.../service/ServicioMenu.java` | Menú público (solo lo visible) |
| `backend/.../service/ServicioCategoria.java` | CRUD de categorías con reglas |
| `backend/.../service/ServicioProducto.java` | CRUD de productos con reglas |
| `backend/.../controller/ControladorMenu.java` | Endpoints públicos `/api/menu/**` |
| `backend/.../controller/ControladorCategoria.java` | `/api/categorias` (ADMIN) |
| `backend/.../controller/ControladorProducto.java` | `/api/productos` (ADMIN) |
| `backend/src/test/.../service/ServicioCategoriaTests.java` | 8 pruebas unitarias |
| `backend/src/test/.../service/ServicioProductoTests.java` | 8 pruebas unitarias |
| `backend/src/test/.../controller/ControladorMenuTests.java` | 13 pruebas de integración MockMvc |

### Archivos modificados

- `backend/.../config/ConfiguracionSeguridad.java`: `GET /api/menu/**`
  público; `/api/categorias/**` y `/api/productos/**` requieren rol ADMIN.
- `backend/.../exception/ExcepcionNegocio.java`: mapa opcional `campos`
  para 400 con detalle (spec §6).
- `backend/.../exception/ManejadorErrores.java`: manejo de
  `MethodArgumentTypeMismatchException` (400 con `campos.<param>`) y de
  `NoResourceFoundException` (404; antes el handler genérico devolvía 500
  en rutas inexistentes).

### Endpoints agregados

| Método | Ruta | Acceso |
|---|---|---|
| GET | `/api/menu/categorias` | público (solo `activa=true`) |
| GET | `/api/menu/productos` | público (solo `disponible=true` en categorías activas) |
| GET | `/api/menu/productos?categoria=id` | público; categoría inexistente/inactiva → 404 |
| GET | `/api/categorias` | ADMIN (lista completa, incluye inactivas) |
| POST | `/api/categorias` | ADMIN → 201 |
| PUT | `/api/categorias/{id}` | ADMIN (incluye reactivación con `activa:true`) |
| PUT | `/api/categorias/{id}/desactivar` | ADMIN |
| GET | `/api/productos` | ADMIN (lista completa, incluye no disponibles) |
| POST | `/api/productos` | ADMIN → 201 |
| PUT | `/api/productos/{id}` | ADMIN (incluye reactivación con `disponible:true`) |
| PUT | `/api/productos/{id}/desactivar` | ADMIN |

No hay endpoints DELETE: la desactivación es borrado lógico por bandera.

### Pruebas ejecutadas

```cmd
cd backend
set DB_PASSWORD=...   (de database/.env)
set JWT_SECRET=...    (>=32 caracteres)
mvnw.cmd test      → Tests run: 45, Failures: 0, Errors: 0, Skipped: 0  BUILD SUCCESS
mvnw.cmd package   → BUILD SUCCESS (jar backend-0.0.1-SNAPSHOT.jar)
```

Desglose: 16 de Sprint 0 + 16 unitarias (servicios) + 13 de integración
de menú. La migración `V2` quedó aplicada (Flyway reporta versión 2).

### Decisiones tomadas

1. **Banderas de estado**: producto `disponible`, categoría `activa`.
   "Desactivar" = poner la bandera en `false`; nunca `DELETE`.
2. **Categoría con productos disponibles no se puede desactivar** → 400
   (hay que desactivarlos o moverlos primero).
3. **GET de administración** (`GET /api/categorias`, `GET /api/productos`):
   implícitos en el requisito de "CRUD" y documentados en la spec §5 —
   el panel admin debe ver también los registros desactivados.
4. **El menú público no expone `activa`**: DTO nuevo
   `RespuestaCategoriaResumen(id, nombre)`; los endpoints de admin sí
   usan `RespuestaCategoria(id, nombre, activa)`.
5. **Errores de negocio con detalle**: `ExcepcionNegocio` con mapa
   `campos` (ej. `categoriaId` inválido → 400 con el detalle, spec §6).
6. **`?categoria=abc` → 400** con `campos.categoria` vía handler de
   `MethodArgumentTypeMismatchException`.
7. **Rutas inexistentes → 404**: nuevo handler de
   `NoResourceFoundException` (descubierto con las pruebas: el handler
   genérico las devolvía como 500 "Error interno del servidor").
8. **Reactivación** por `PUT` general con `activa:true` /
   `disponible:true`: la spec §5 no define endpoints `/activar`.
9. **Duplicados de nombre** con collation `utf8mb4_unicode_ci`
   (insensible a mayúsculas, sensible a acentos): verificado contra la
   BD real → 409.

### Pendientes / notas para el siguiente sprint

- Imágenes de producto: fuera del Sprint 1. Si se requieren, migración
  `V3` con `imagen_url` nullable (enfoque por URL, ya decidido).
- CORS sigue restringido a orígenes de desarrollo; restringir antes de
  producción.
- El menú público consulta las tablas en cada petición (sin caché): si el
  rendimiento lo pide, evaluar caché en un sprint futuro.

---

## Sprint 2 — Pedidos: creación y consulta (20/11/2026)

### Objetivo

Implementar el registro de pedidos anticipados por el cliente (estado inicial
`PENDIENTE`) con validación de la ventana de entrega dentro del horario de
servicio y captura del precio vigente, y la consulta de pedidos por rol. Las
transiciones de estado (pago, cancelación, preparación, listo, entregado) se
difieren a los Sprints 3 y 4 según el plan.

### Spec asociada

`specs/pedidos/spec.md` (ajustada a la regla de ventana/horario aprobada con la patrocinadora).

### Decisiones de negocio aplicadas (aprobadas/ajustadas)

1. **Ventana de entrega y horario de servicio:** el cliente elige `fechaHoraEntrega`, **mismo día**,
   zona `America/Mexico_City`. El pedido se realiza dentro del horario de servicio
   (`horaApertura <= ahora < horaCierre`) y la entrega debe cumplir
   `entrega >= ahora + tiempoPreparacionMinutos` (lead), `entrega <= horaCierre - margenCierreMinutos`
   y estar dentro del horario de servicio. Si `ahora + tiempoPreparacionMinutos` es anterior a la
   apertura del mismo día, el mínimo efectivo es `horaApertura`. La hora **no** exige alineación a
   una grilla/franja exacta (p. ej. múltiplos de 10 minutos).
2. **Configuración:** `cafeitsh.horario-apertura` (`08:00`), `cafeitsh.horario-cierre` (`20:00`),
   `cafeitsh.tiempo-preparacion-minutos` (`15`), `cafeitsh.margen-cierre-minutos` (`15`) en
   `application.yaml` (horario único diario).
3. **Inmutabilidad:** pedido confirmado = creado en `PENDIENTE`; no se
   editan ítems, cantidades, hora ni total tras la creación.
4. **Sin límites de sistema:** no hay tope de ítems/monto en backend
   (capacidad sujeta a operación/cocina).
5. **Precio vigente:** `precioUnitario` y `subtotal` por ítem y `total`
   del pedido se capturan al crear y no se recalculan.
6. **Método de pago preferido:** intención opcional `EFECTIVO_EN_CAJA`
   (sin datos de tarjeta). No equivale a pago confirmado; el cobro real
   lo registra CAJA en Sprint 3 (`PENDIENTE -> PAGADO`).
7. **Estados y flujo:** catálogo fijo sin cambios. Creación en
   `PENDIENTE`; preparación solo con pedido `PAGADO` (COCINA pasa a
   `EN_PREPARACION` cuando hay capacidad, Sprint 4). No se exponen
   endpoints de transición en este sprint.
8. **Cancelación (regla definida):** solo antes de pagar/preparar
   (`PENDIENTE`), por CLIENTE dueño y/o CAJA/ADMIN; no cancelable en
   `EN_PREPARACION`, `LISTO`, `ENTREGADO`. Endpoint/transición diferidos
   a Sprint 3 (salvo autorización explícita posterior).
9. **Visibilidad:** CLIENTE solo sus pedidos; CAJA, COCINA y ADMIN
   consultan todos.

### Archivos creados

| Archivo | Contenido |
|---|---|
| `specs/pedidos/spec.md` | Especificación del módulo de pedidos |
| `backend/.../resources/db/migraciones/V3__crear_tablas_pedidos.sql` | Tablas `pedidos` y `pedido_items` |
| `backend/.../entity/EstadoPedido.java`, `MetodoPagoPreferido.java`, `Pedido.java`, `PedidoItem.java` | Entidades y catálogos |
| `backend/.../repository/PedidoRepository.java`, `PedidoItemRepository.java` | Acceso a datos y consultas con detalle |
| `backend/.../dto/SolicitudItemPedido.java`, `SolicitudCreacionPedido.java` | DTOs de entrada |
| `backend/.../dto/RespuestaClientePedido.java`, `RespuestaItemPedido.java`, `RespuestaPedido.java` | DTOs de salida |
| `backend/.../service/ServicioPedido.java` | Reglas de ventana/horario, disponibilidad, precio vigente, visibilidad |
| `backend/.../controller/ControladorPedido.java` | Endpoints de creación y consulta |
| `backend/src/test/.../service/ServicioPedidoTests.java` | 17 pruebas unitarias de reglas de ventana y servicio |
| `backend/src/test/.../controller/ControladorPedidoTests.java` | 9 pruebas de integración MockMvc |

### Archivos modificados

- `backend/src/main/resources/application.yaml`: propiedades `cafeitsh.horario-apertura`,
  `cafeitsh.horario-cierre`, `cafeitsh.tiempo-preparacion-minutos`, `cafeitsh.margen-cierre-minutos`.
- `backend/.../config/ConfiguracionSeguridad.java`: `POST /api/pedidos`
  solo CLIENTE; `GET /api/pedidos` y `GET /api/pedidos/**` autenticados
  (la propiedad/rol se valida en servicio).
- `backend/.../exception/ManejadorErrores.java`: manejo de
  `HttpMessageNotReadableException` → 400 en español (cuerpos/tipos
  inválidos).

### Endpoints agregados

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/api/pedidos` | CLIENTE (crea en `PENDIENTE`, 201) |
| GET | `/api/pedidos` | autenticado (CLIENTE: propios; CAJA/COCINA/ADMIN: todos) |
| GET | `/api/pedidos/{id}` | autenticado (CLIENTE solo dueño; 403 si ajeno; 404 si no existe) |

No se agregan endpoints DELETE ni de edición/transición de estado.

### Pruebas ejecutadas

**Implementación inicial (regla anterior 10–20 min, verificada):**
`mvnw.cmd test` → Tests run: 70, Failures: 0, Errors: 0, Skipped: 0 (BUILD SUCCESS);
`mvnw.cmd package` → BUILD SUCCESS, con `V3` aplicada en MySQL real (puerto 3307).

**Ajuste de regla (apertura/cierre/lead/margen configurables, sin alineación a grilla):**
spec, servicio, configuración, pruebas y documentación actualizados y re-verificados en esta sesión:
```cmd
cd backend
set DB_PASSWORD=<valor de database/.env>
set JWT_SECRET=<secreto >= 32 caracteres>
mvnw.cmd test      → Tests run: 71, Failures: 0, Errors: 0, Skipped: 0  BUILD SUCCESS
mvnw.cmd package   → BUILD SUCCESS (jar backend-0.0.1-SNAPSHOT.jar)
```
Pruebas contra MySQL real en Docker (puerto 3307) con `V3` aplicada. Se ajustó una aserción unitaria de texto (`"mismo"`) por codificación de salida en consola Windows; la regla validada se mantiene.

### Decisiones técnicas tomadas

1. **V3 nueva, sin tocar V1/V2:** `pedidos` y `pedido_items` con FKs a
   `usuarios` y `productos`, checks de cantidades/precios e índices de
   consulta; sin tabla `pagos` (Sprint 3).
2. **Validación de ventana en servicio:** cálculo con
   `America/Mexico_City` y parámetros configurables (apertura, cierre, lead, margen);
   método de validación de paquete para pruebas unitarias deterministas.
3. **Precio histórico:** se copia `precio` del producto a
   `precio_unitario`/`subtotal` al crear (no se recalcula).
4. **Consultas con fetch join + DISTINCT** para cargar usuario, ítems y
   productos sin N+1 y sin duplicados por colección.
5. **Integración de pedidos con horario de prueba amplio** (`00:00`–`23:59`, lead/margen 15)
   para disponer de ventana válida al ejecutar en cualquier hora del día, manteniendo la regla
   de negocio en el servicio.

### Pendientes / notas para el siguiente sprint

- Ejecutar `mvnw.cmd test` y `mvnw.cmd package` del ajuste de regla y registrar resultados.
- Ratificar con la patrocinadora las decisiones vigentes (horario/lead/margen configurables,
  sin exigir grilla exacta, cancelación solo en `PENDIENTE` diferida a Sprint 3, método
  `EFECTIVO_EN_CAJA` como intención).
- Sprint 3 (Caja): transición `PENDIENTE -> PAGADO`, cancelación según
  reglas aprobadas, corte básico y tabla `pagos` si aplica; definir
  manejo de pedidos `PAGADO` que cocina no pueda preparar.
- Sprint 4 (Cocina): transiciones `PAGADO -> EN_PREPARACION -> LISTO`
  y definición de quién marca `ENTREGADO`.
- CORS sigue restringido a orígenes de desarrollo.

---

## Sprint 3 — Caja: pago, cancelación y corte básico (23/11/2026)

### Objetivo

Implementar el flujo de caja: registro de pago PENDIENTE -> PAGADO por CAJA/ADMIN, cancelación controlada por rol/estado con auditoría, y corte básico diario en zona America/Mexico_City.

### Spec asociada

specs/caja/spec.md (aprobada).

### Archivos creados

| Archivo | Contenido |
|---|---|
| specs/caja/spec.md | Especificación del módulo de caja |
| ackend/.../db/migraciones/V4__crear_tabla_pagos_y_auditoria_cancelacion.sql | Tabla pagos y campos de auditoría de cancelación/reembolso en pedidos |
| ackend/.../entity/MetodoPago.java, Pago.java | Catálogo de pago real y entidad de pago |
| ackend/.../repository/PagoRepository.java | Consultas de pagos y rango por fecha |
| ackend/.../dto/SolicitudRegistroPago.java, SolicitudCancelacionPedido.java | DTOs de entrada |
| ackend/.../dto/RespuestaPago.java, RespuestaCancelacionPedido.java, RespuestaDesglosePago.java, RespuestaCanceladosCorte.java, RespuestaCorteCaja.java | DTOs de salida |
| ackend/.../service/ServicioCaja.java | Reglas de pago, cancelación, reembolso manual y corte |
| ackend/.../controller/ControladorCaja.java | Endpoints de pago, cancelación y corte |
| ackend/src/test/.../service/ServicioCajaTests.java | Pruebas unitarias |
| ackend/src/test/.../controller/ControladorCajaTests.java | Pruebas de integración MockMvc |

### Archivos modificados

- ackend/.../entity/MetodoPagoPreferido.java: agrega TRANSFERENCIA.
- ackend/.../entity/Pedido.java: campos de cancelación/reembolso manual y relación con pagos.
- ackend/.../dto/RespuestaPedido.java, service/ServicioPedido.java: incluyen pago y cancelacion cuando aplica.
- ackend/.../repository/PedidoRepository.java: consulta de cancelados por rango.
- ackend/.../config/ConfiguracionSeguridad.java: reglas para /api/pedidos/*/pago, /api/pedidos/*/cancelar, /api/caja/**.
- ackend/src/test/.../service/ServicioPedidoTests.java: constructor actualizado con PagoRepository.

### Pruebas ejecutadas

`cmd
cd backend
set DB_PASSWORD=<valor de database/.env>
set JWT_SECRET=<secreto >= 32 caracteres>
mvnw.cmd test      → Tests run: 83, Failures: 0, Errors: 0, Skipped: 0  BUILD SUCCESS
mvnw.cmd package   → BUILD SUCCESS (jar backend-0.0.1-SNAPSHOT.jar)
`

### Decisiones tomadas

- Métodos aceptados: EFECTIVO y TRANSFERENCIA; sin datos de tarjeta ni integración bancaria.
- Intención metodoPagoPreferido ampliada a EFECTIVO_EN_CAJA y TRANSFERENCIA; pago real en tabla pagos.
- Cancelación: CLIENTE solo PENDIENTE propio; CAJA/ADMIN PENDIENTE o PAGADO antes de EN_PREPARACION; no cancelable en estados posteriores. Motivo/actor/fecha obligatorios.
- Sin reembolsos automáticos ni ajustes de monto; se registra constancia manual de reembolso cuando aplica.
- Corte básico diario por fecha de pago en America/Mexico_City: solo pagados del día, desglose por método y contador informativo de cancelados sin sumar al ingreso neto; sin rangos.

---

## Sprint 4 — Cocina: vista de pendientes y transiciones (24/11/2026)

### Estado

**Implementado, no cerrado.** Falta ejecutar la suite completa y `mvnw.cmd package`
con pruebas contra MySQL real; en este entorno no hay Docker, `database/.env` ni
servicio MySQL en el puerto 3307.

### Objetivo

Exponer los pedidos pagados pendientes de preparación e implementar las
transiciones `PAGADO -> EN_PREPARACION -> LISTO -> ENTREGADO`, sin saltos de
estado, con autorización verificada en backend.

### Spec asociada

`specs/cocina/spec.md` (creada y aprobada en este sprint con las decisiones
resueltas con la patrocinadora).

### Archivos creados

| Archivo | Contenido |
|---|---|
| `specs/cocina/spec.md` | Especificación del módulo de cocina |
| `backend/.../service/ServicioCocina.java` | Vista de pendientes y reglas de transición por rol/estado |
| `backend/.../controller/ControladorCocina.java` | Endpoints de vista y transiciones |
| `backend/src/test/.../service/ServicioCocinaTests.java` | 8 pruebas unitarias |
| `backend/src/test/.../controller/ControladorCocinaTests.java` | 5 pruebas de integración MockMvc (escritas, pendientes de ejecutar) |

### Archivos modificados

- `backend/.../repository/PedidoRepository.java`: consulta
  `buscarPendientesPreparacion()` con estados `PAGADO` ordenados por
  `fechaHoraEntrega` y `id`.
- `backend/.../config/ConfiguracionSeguridad.java`: `GET /api/cocina/**` para
  COCINA/ADMIN; `POST /api/pedidos/*/en-preparacion` y `/listo` para
  COCINA/ADMIN; `POST /api/pedidos/*/entregado` para COCINA/CAJA/ADMIN.
- `docs/sprints.md`, `AGENTS.md`, `docs/guia-colaborador.md`: estado y endpoints.

### Endpoints agregados

| Método | Ruta | Acceso |
|---|---|---|
| GET | `/api/cocina/pedidos` | COCINA/ADMIN (pedidos `PAGADO`) |
| POST | `/api/pedidos/{id}/en-preparacion` | COCINA/ADMIN (`PAGADO -> EN_PREPARACION`) |
| POST | `/api/pedidos/{id}/listo` | COCINA/ADMIN (`EN_PREPARACION -> LISTO`) |
| POST | `/api/pedidos/{id}/entregado` | COCINA/CAJA/ADMIN (`LISTO -> ENTREGADO`) |

### Pruebas ejecutadas

```cmd
cd backend
mvnw.cmd -DskipTests compile          → COMPILACIÓN EXITOSA
mvnw.cmd test -Dtest=ServicioCocinaTests → Tests run: 8, Failures: 0, Errors: 0, Skipped: 0  BUILD SUCCESS
mvnw.cmd -DskipTests package          → BUILD SUCCESS (backend-0.0.1-SNAPSHOT.jar)
```

**No ejecutado:** `mvnw.cmd test` completo ni `mvnw.cmd package` con pruebas.
Requiere MySQL en `3307`, `DB_PASSWORD` y `JWT_SECRET`; no estaban disponibles.
Las 5 pruebas de integración de `ControladorCocinaTests` tampoco se ejecutaron.

### Decisiones tomadas

1. **Puerta de cocina:** solo pedidos `PAGADO` pueden pasar a `EN_PREPARACION`.
2. **Sin saltos:** `PAGADO -> EN_PREPARACION -> LISTO -> ENTREGADO`; otro origen
   o destino devuelve 409 y no modifica el pedido.
3. **Roles:** COCINA y ADMIN gestionan preparación/listo; COCINA, CAJA y ADMIN
   registran la entrega.
4. **Vista:** `GET /api/cocina/pedidos` devuelve solo pedidos `PAGADO`, ordenados
   por hora de entrega; sin notificaciones automáticas en este sprint.
5. **Sin migración nueva:** se reutiliza `pedidos.actualizadoEn`; no se añadió
   auditoría específica de cocina.
6. **Cancelación sin cambios:** un pedido `PAGADO` que cocina no pueda preparar
   se cancela desde caja antes de iniciar preparación, según `specs/caja/spec.md`.

### Pendientes / notas

- Ejecutar `mvnw.cmd test` y `mvnw.cmd package` completos con MySQL real.
- Ratificar el cierre del Sprint 4 en la bitácora después de la verificación.
- CORS sigue restringido a orígenes de desarrollo (Sprint 5).
- Frontend y `docs/endpoints-api.md` quedan para Sprint 5.
