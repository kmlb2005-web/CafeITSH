# Documentación de endpoints — API cafeITSH

Contrato del backend (Spring Boot, puerto `8081`) para el equipo de frontend. Cubre los 27 endpoints existentes al cierre del Sprint 4. Fuente de verdad del comportamiento: las specs en `specs/` y el código; este documento las resume.

## 1. Convenciones

- **Base URL:** `http://localhost:8081` (emulador Android: `http://10.0.2.2:8081`). Centralizar en un solo lugar del frontend.
- **Formato:** JSON (`Content-Type: application/json`), todos los mensajes en español.
- **Autenticación:** `Authorization: Bearer <token>`, token obtenido en `POST /api/auth/login` (vigencia 480 minutos). Sin sesiones.
- **Fechas:** `LocalDateTime` ISO sin zona (`2026-11-27T13:30:00`), en hora de `America/Mexico_City`. Fechas de corte: `yyyy-MM-dd`.
- **Montos:** decimales con 2 posiciones.
- **Roles:** `ADMIN`, `CAJA`, `COCINA`, `CLIENTE`. La autorización se verifica en el backend; el frontend solo oculta pantallas.
- **CORS:** orígenes definidos en `cafeitsh.cors.origenes-permitidos` (hoy solo `localhost`). Métodos `GET, POST, PUT, DELETE, OPTIONS`; headers `Authorization` y `Content-Type`.

### Formato de error

```json
{ "mensaje": "Los datos enviados no son válidos", "timestamp": "2026-11-27T19:30:00Z", "campos": { "correo": "El correo no tiene un formato válido" } }
```

`campos` solo aparece en errores de validación. Códigos usados:

| Código | Significado |
|---|---|
| 400 | Datos inválidos o regla de entrega/horario incumplida |
| 401 | Sin token, token inválido o credenciales incorrectas |
| 403 | Rol insuficiente o pedido ajeno |
| 404 | Recurso inexistente |
| 409 | Conflicto de estado o duplicado |
| 500 | Error interno (sin detalles) |

### Estados del pedido

`PENDIENTE → PAGADO → EN_PREPARACION → LISTO → ENTREGADO`, más `CANCELADO` (desde `PENDIENTE` o `PAGADO`). Sin saltos ni otras transiciones.

## 2. Resumen por rol

| Endpoint | Público | CLIENTE | CAJA | COCINA | ADMIN |
|---|:-:|:-:|:-:|:-:|:-:|
| `POST /api/auth/registro`, `POST /api/auth/login`, `GET /api/prueba` | ✔ | | | | |
| `GET /api/menu/categorias`, `GET /api/menu/productos` | ✔ | | | | |
| `GET /api/auth/me` | | ✔ | ✔ | ✔ | ✔ |
| `POST /api/pedidos` | | ✔ | | | |
| `GET /api/pedidos`, `GET /api/pedidos/{id}` | | propios | todos | todos | todos |
| `POST /api/pedidos/{id}/cancelar` | | propio en `PENDIENTE` | ✔ | | ✔ |
| `POST /api/pedidos/{id}/pago` | | | ✔ | | ✔ |
| `GET /api/caja/corte` | | | ✔ | | ✔ |
| `GET /api/cocina/pedidos`, `POST …/en-preparacion`, `POST …/listo` | | | | ✔ | ✔ |
| `POST /api/pedidos/{id}/entregado` | | | ✔ | ✔ | ✔ |
| `/api/usuarios`, `/api/categorias`, `/api/productos` (todos) | | | | | ✔ |

## 3. Autenticación

### `POST /api/auth/registro` — público
Crea un usuario con rol `CLIENTE`.

Entrada: `{ "nombre": "2–100 car.", "correo": "email, máx. 255", "contrasena": "mín. 8 car." }`
Salida **201**: `RespuestaUsuario` → `{ "id", "nombre", "correo", "rol", "activo" }`
Errores: 400 validación, 409 `El correo ya está registrado`.

### `POST /api/auth/login` — público
Entrada: `{ "correo", "contrasena" }`
Salida **200**: `{ "token": "…", "tipo": "Bearer", "usuario": RespuestaUsuario }`
Errores: 400 validación, 401 `Correo o contraseña incorrectos` (también para cuenta desactivada).

### `GET /api/auth/me` — autenticado
Salida **200**: `RespuestaUsuario` del token. Úsalo para conocer el rol al iniciar la app.

### `GET /api/prueba` — público
Texto de verificación de que el backend responde. No usar en la app.

## 4. Menú público

### `GET /api/menu/categorias` — público
Salida **200**: `[ { "id", "nombre" } ]` (solo categorías activas).

### `GET /api/menu/productos?categoria={id}` — público
`categoria` opcional. Salida **200**: `[ RespuestaProducto ]` (solo productos disponibles) donde
`RespuestaProducto = { "id", "nombre", "descripcion", "precio", "disponible", "categoria": { "id", "nombre" } }`.
Errores: 400 `categoria` no numérica, 404 `Categoría no encontrada`.

## 5. Administración de menú — solo ADMIN

### Categorías (`/api/categorias`)
`RespuestaCategoria = { "id", "nombre", "activa" }`

| Método y ruta | Entrada | Salida | Errores |
|---|---|---|---|
| `GET /api/categorias` | — | **200** lista completa (incluye inactivas) | |
| `POST /api/categorias` | `{ "nombre" (2–100) }` | **201** `RespuestaCategoria` | 400, 409 nombre duplicado |
| `PUT /api/categorias/{id}` | `{ "nombre" (2–100), "activa" (obligatorio) }` | **200** `RespuestaCategoria` | 400, 404, 409 |
| `PUT /api/categorias/{id}/desactivar` | — | **200** `RespuestaCategoria` (idempotente, sin borrado físico) | 404, 409 si tiene productos disponibles |

### Productos (`/api/productos`)

| Método y ruta | Entrada | Salida | Errores |
|---|---|---|---|
| `GET /api/productos` | — | **200** lista completa (incluye no disponibles) | |
| `POST /api/productos` | `{ "nombre" (2–100), "descripcion" (≤500, opcional), "precio" (≥0, 2 decimales), "disponible" (opcional), "categoriaId" }` | **201** `RespuestaProducto` | 400, 404 categoría |
| `PUT /api/productos/{id}` | igual, con `disponible` obligatorio | **200** `RespuestaProducto` | 400, 404 |
| `PUT /api/productos/{id}/desactivar` | — | **200** `RespuestaProducto` (idempotente) | 404 |

## 6. Usuarios — solo ADMIN

| Método y ruta | Entrada | Salida | Errores |
|---|---|---|---|
| `GET /api/usuarios` | — | **200** `[ RespuestaUsuario ]` | |
| `POST /api/usuarios` | `{ "nombre", "correo", "contrasena" (mín. 8), "rol" }` | **201** `RespuestaUsuario` | 400, 409 correo duplicado |
| `PUT /api/usuarios/{id}` | `{ "nombre", "correo", "rol", "activo" }` (todos obligatorios) | **200** `RespuestaUsuario` | 400, 404, 409; 400 si el ADMIN intenta desactivar su propia cuenta |

Sin borrado físico: se desactiva con `activo = false`.

## 7. Pedidos

### Objeto `RespuestaPedido`

```json
{
  "id": 1,
  "estado": "PENDIENTE",
  "fechaHoraEntrega": "2026-11-27T13:30:00",
  "metodoPagoPreferido": "EFECTIVO_EN_CAJA",
  "total": 45.00,
  "cliente": { "id": 7, "nombre": "…", "correo": "…" },
  "items": [ { "productoId": 3, "nombreProducto": "…", "cantidad": 2, "precioUnitario": 22.50, "subtotal": 45.00 } ],
  "creadoEn": "2026-11-27T12:00:00",
  "pago": null,
  "cancelacion": null
}
```

- `pago` (cuando existe): `{ "id", "metodoPago": "EFECTIVO|TRANSFERENCIA", "monto", "fechaPago", "registradoPor": {id,nombre,correo}, "referencia" }`.
- `cancelacion` (cuando existe): `{ "canceladoEn", "canceladoPor": {id,nombre,correo}, "motivoCancelacion", "requiereReembolso", "reembolsoManualRegistrado", "observacionReembolso" }`.

### `POST /api/pedidos` — CLIENTE
Entrada: `{ "fechaHoraEntrega", "metodoPagoPreferido": "EFECTIVO_EN_CAJA|TRANSFERENCIA" (opcional), "items": [ { "productoId", "cantidad" (≥1) } ] }` (al menos un ítem).
Salida **201**: `RespuestaPedido` en `PENDIENTE`, con precios vigentes y total calculado. Inmutable tras crearse.

Reglas de entrega (hora `America/Mexico_City`): mismo día; solicitud dentro del horario de servicio (hoy 08:00–20:00); entrega ≥ ahora + 15 min y ≤ cierre − 15 min; no exige alineación a franjas.
Errores: 400 (validación, horario, ventana de entrega, producto no disponible), 404 producto.

### `GET /api/pedidos` — autenticado
**200** `[ RespuestaPedido ]`. CLIENTE ve solo los suyos; CAJA, COCINA y ADMIN ven todos.

### `GET /api/pedidos/{id}` — autenticado
**200** `RespuestaPedido`. Errores: 403 `No tienes permiso para consultar este pedido` (CLIENTE ajeno), 404.

## 8. Caja — CAJA y ADMIN

### `POST /api/pedidos/{id}/pago`
Entrada: `{ "metodoPago": "EFECTIVO|TRANSFERENCIA", "referencia": "≤100, opcional" }`. Nunca datos de tarjeta.
Salida **200**: `RespuestaPedido` en `PAGADO` con `pago`.
Errores: 400, 403, 404, 409 `Solo se puede registrar el pago de un pedido en estado PENDIENTE`.

### `POST /api/pedidos/{id}/cancelar`
Entrada: `{ "motivoCancelacion": "obligatorio, ≤255", "requiereReembolso": bool (opcional), "reembolsoManualRegistrado": bool (opcional), "observacionReembolso": "≤255, opcional" }`.
Quién puede: CLIENTE dueño solo en `PENDIENTE`; CAJA y ADMIN en `PENDIENTE` o `PAGADO`.
Salida **200**: `RespuestaPedido` en `CANCELADO` con `cancelacion`.
Errores: 400, 403, 404, 409 `El pedido no puede cancelarse en su estado actual`.

### `GET /api/caja/corte?fecha=yyyy-MM-dd`
`fecha` opcional (por defecto hoy en `America/Mexico_City`). Salida **200**:

```json
{
  "fecha": "2026-11-27", "zonaHoraria": "America/Mexico_City",
  "totalCobrado": 120.00, "cantidadPagos": 4,
  "desglosePorMetodo": [ { "metodoPago": "EFECTIVO", "cantidad": 3, "monto": 90.00 }, { "metodoPago": "TRANSFERENCIA", "cantidad": 1, "monto": 30.00 } ],
  "cancelados": { "cantidad": 1, "montoPagadoCancelado": 25.00 }
}
```

Los pagos de pedidos cancelados no suman al total; solo se informan en `cancelados`.

## 9. Cocina

Todas las transiciones devuelven **200** `RespuestaPedido`, sin cuerpo de entrada. Errores: 403 rol, 404, 409 `El pedido no está en el estado requerido para esta operación`.

| Método y ruta | Roles | Transición |
|---|---|---|
| `GET /api/cocina/pedidos` | COCINA, ADMIN | Lista pedidos `PAGADO`, ordenados por `fechaHoraEntrega` (luego `id`) |
| `POST /api/pedidos/{id}/en-preparacion` | COCINA, ADMIN | `PAGADO → EN_PREPARACION` |
| `POST /api/pedidos/{id}/listo` | COCINA, ADMIN | `EN_PREPARACION → LISTO` |
| `POST /api/pedidos/{id}/entregado` | COCINA, CAJA, ADMIN | `LISTO → ENTREGADO` |

No hay notificaciones automáticas: el cliente consulta `GET /api/pedidos/{id}` para ver el estado.

## 10. Notas para el frontend

- Un 401 en cualquier endpoint protegido significa token vencido o ausente: volver al login.
- Mostrar `mensaje` al usuario y `campos` junto a cada campo del formulario.
- No hay endpoints de edición ni borrado de pedidos.
- Validar en el frontend es solo comodidad; el backend repite todas las validaciones.
