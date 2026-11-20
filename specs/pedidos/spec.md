# Especificación: Pedidos

**Módulo:** pedidos  
**Sprint:** 2  
**Estado:** aprobada y ajustada (regla de ventana/horario actualizada según acuerdo con patrocinadora)

## 1. Objetivo

Permitir que un cliente autenticado realice un pedido anticipado del menú, con validación de la ventana de entrega dentro del horario de servicio y captura del precio vigente de cada producto al momento de la creación. El pedido nace en estado `PENDIENTE`; las transiciones operativas posteriores se implementan en los sprints siguientes según el plan del proyecto.

## 2. Descripción funcional

- Un usuario con rol **CLIENTE** crea un pedido seleccionando productos disponibles y la hora de entrega deseada.
- El backend valida que la solicitud se realice dentro del horario de servicio, que la hora de entrega sea del mismo día y esté dentro de la ventana permitida, que los productos estén disponibles y pertenezcan a categorías activas, y calcula el total con el precio vigente al momento de crear el pedido.
- Los pedidos son **inmutables** tras su creación: no se pueden modificar ítems, cantidades ni hora de entrega. Un cambio requiere cancelar (cuando la regla lo permita) y crear un nuevo pedido.
- Los usuarios autenticados consultan pedidos según su rol: el cliente consulta solo los propios; CAJA, COCINA y ADMIN consultan todos.
- Este sprint no implementa endpoints de transición de estado (pago, preparación, entrega, cancelación); esos flujos se detallan y desarrollan en Sprint 3 y Sprint 4.

## 3. Requisitos

1. Migración `V3` con tablas `pedidos` y `pedido_items` (sin tabla `pagos`, diferida a Sprint 3).
2. `POST /api/pedidos` (rol CLIENTE): crea pedido en estado `PENDIENTE`, valida la ventana de entrega y persiste ítems con el precio unitario vigente.
3. `GET /api/pedidos` (autenticado): listado filtrado por rol (CLIENTE: propios; CAJA/COCINA/ADMIN: todos).
4. `GET /api/pedidos/{id}` (autenticado): detalle con control de propiedad/rol.
5. Sin endpoints DELETE y sin endpoints de edición de pedidos.
6. Validación y autorización exclusivamente en el backend.

## 4. Reglas de negocio

### 4.1 Horario de servicio y ventana de entrega (aprobada)

- El cliente **elige** la `fechaHoraEntrega` para el **mismo día** de la solicitud.
- Zona horaria de referencia: `America/Mexico_City`.
- El pedido solo puede **realizarse** cuando la hora actual (`ahora`) está dentro del horario de servicio: `horaApertura <= ahora < horaCierre`.
- La hora de entrega debe estar dentro del horario de servicio y cumplir:
  - `entrega >= ahora + tiempoPreparacionMinutos` (tiempo mínimo de preparación/lead time).
  - `entrega <= horaCierre - margenCierreMinutos` (corte antes del cierre para la última entrega).
  - Si `ahora + tiempoPreparacionMinutos` es anterior a la apertura del mismo día, el mínimo efectivo es `horaApertura`.
- La `fechaHoraEntrega` **no** está obligada a coincidir con una cuadrícula/franja exacta (p. ej. múltiplos de 10 minutos); se acepta cualquier hora válida dentro del rango. La operación puede organizarse con franjas orientativas (p. ej. cada 10 minutos) sin que el backend exija alineación.
- Si no existe ningún horario válido (p. ej. ya no hay margen suficiente antes del corte), la creación se rechaza con 400 y mensaje claro en español.

**Parámetros configurables** (`application.yaml`, no secretos):
| Propiedad | Valor inicial | Descripción |
|---|---|---|
| `cafeitsh.horario-apertura` | `08:00` | Hora de apertura (HH:mm), horario único diario |
| `cafeitsh.horario-cierre` | `20:00` | Hora de cierre (HH:mm), horario único diario |
| `cafeitsh.tiempo-preparacion-minutos` | `15` | Anticipación/preparación mínima (lead time) |
| `cafeitsh.margen-cierre-minutos` | `15` | Margen antes del cierre para la última entrega |

**Ejemplo (apertura 08:00, cierre 20:00, lead 15, margen 15, franjas orientativas 10 min):** la última entrega permitida por regla es 19:45 (cierre - margen). A las 11:00 se puede elegir desde 11:15 en adelante (p. ej. 11:20) hasta 19:45; a las 19:20 el primer horario posible es 19:35 (p. ej. 19:40) y la última entrega sigue siendo 19:45; a las 19:31 ya no hay horarios disponibles porque `ahora + 15` supera la última entrega permitida.

### 4.2 Creación e inmutabilidad

- El pedido se crea en estado **`PENDIENTE`**.
- “Pedido confirmado” = pedido creado exitosamente; desde ese momento es inmutable (sin edición de ítems/cantidades/hora/total).
- Debe incluir al menos 1 ítem; cada ítem requiere `productoId` y `cantidad >= 1`.
- No hay límite de ítems ni de monto impuesto por el sistema.
- Cada producto debe existir, estar `disponible = true` y pertenecer a una categoría `activa = true` al momento de crear el pedido.
- El `precioUnitario` y `subtotal` de cada ítem, así como el `total` del pedido, se capturan/calculan con el precio vigente del producto en el momento de la creación y **no se recalculan** después.

### 4.3 Método de pago preferido (intención)

- El pedido puede registrar `metodoPagoPreferido` como intención operativa (catálogo inicial: `EFECTIVO_EN_CAJA`). Campo opcional.
- **No se capturan ni almacenan datos de tarjetas bancarias.**
- La intención no equivale a pago confirmado: el cobro real lo registra CAJA como transición `PENDIENTE → PAGADO` (Sprint 3).

### 4.4 Estados y transiciones

- Estados del sistema (catálogo fijo): `PENDIENTE`, `PAGADO`, `EN_PREPARACION`, `LISTO`, `ENTREGADO`, `CANCELADO`.
- Sprint 2: solo crea en `PENDIENTE` y permite consulta; **no expone endpoints de transición**.
- Flujo aprobado: preparación solo cuando el pedido está `PAGADO`. CAJA registra `PENDIENTE → PAGADO` (Sprint 3). COCINA ejecuta `PAGADO → EN_PREPARACION` cuando hay capacidad/recursos (Sprint 4); la “aprobación por capacidad” es esa transición, no un estado adicional.
- **Cancelación (regla aprobada):** cancelable solo antes de pagar/preparar (estado `PENDIENTE`), por el CLIENTE dueño y/o CAJA/ADMIN. No cancelable en `EN_PREPARACION`, `LISTO`, `ENTREGADO`. La transición/endpoint de cancelación se implementa en **Sprint 3** (salvo autorización explícita posterior); el modelo V3 contempla el estado `CANCELADO`.

### 4.5 Consulta y visibilidad

- CLIENTE: solo sus propios pedidos.
- CAJA, COCINA, ADMIN: pueden consultar todos los pedidos (listado y detalle).

## 5. Datos de entrada y salida

### POST /api/pedidos (CLIENTE, autenticado)

Entrada:
```json
{
  "fechaHoraEntrega": "2026-11-20T14:30:00",
  "metodoPagoPreferido": "EFECTIVO_EN_CAJA",
  "items": [
    { "productoId": 3, "cantidad": 2 }
  ]
}
```

- `fechaHoraEntrega`: obligatoria, fecha/hora local (`yyyy-MM-dd'T'HH:mm:ss`) interpretada en `America/Mexico_City`.
- `metodoPagoPreferido`: opcional; valores permitidos: `EFECTIVO_EN_CAJA`.
- `items`: obligatorio, mínimo 1 elemento. Cada elemento: `productoId` (obligatorio), `cantidad` (obligatoria, entero >= 1).

Salida 201 (ejemplo):
```json
{
  "id": 10,
  "estado": "PENDIENTE",
  "fechaHoraEntrega": "2026-11-20T14:30:00",
  "metodoPagoPreferido": "EFECTIVO_EN_CAJA",
  "total": 50.00,
  "cliente": { "id": 5, "nombre": "Ana", "correo": "ana@correo.mx" },
  "items": [
    {
      "productoId": 3,
      "nombreProducto": "Café americano",
      "cantidad": 2,
      "precioUnitario": 25.00,
      "subtotal": 50.00
    }
  ],
  "creadoEn": "2026-11-20T14:15:00"
}
```

### GET /api/pedidos (autenticado)

- CLIENTE: 200 con lista de sus pedidos.
- CAJA/COCINA/ADMIN: 200 con lista de todos los pedidos.
- Orden: más recientes primero (por `creadoEn` descendente).

### GET /api/pedidos/{id} (autenticado)

- 200 con el detalle si tiene permiso; 403 si es CLIENTE y no es dueño; 404 si no existe.

### Errores

Formato uniforme `RespuestaError` en español: `{"mensaje", "timestamp", "campos"?}`.
- 400: validaciones de formato, solicitud fuera de horario de servicio, ventana de entrega inválida o sin horarios disponibles, ítems vacíos, cantidad inválida, producto no disponible/categoría inactiva, método de pago no permitido.
- 401: sin autenticación válida.
- 403: rol o propiedad no autorizada.
- 404: pedido o producto no encontrado.

## 6. Validaciones

- Solicitud realizada dentro del horario de servicio (`horaApertura <= ahora < horaCierre`).
- `fechaHoraEntrega`: obligatoria, no nula, mismo día, dentro del horario de servicio y dentro de la ventana calculada (regla 4.1).
- `items`: no nulo y no vacío.
- `items[].productoId`: obligatorio y existente.
- `items[].cantidad`: obligatoria, entera, >= 1.
- Producto referenciado: `disponible = true` y su categoría `activa = true`.
- `metodoPagoPreferido`: si se envía, debe pertenecer al catálogo permitido.
- Todas las validaciones se aplican en el backend.

## 7. Criterios de aceptación

1. Un CLIENTE autenticado puede crear un pedido válido y recibe 201 con estado `PENDIENTE`, total correcto e ítems con precio vigente.
2. Crear fuera del horario de servicio o con hora de entrega fuera de la ventana/horario permitido → 400 con mensaje claro.
3. Crear con producto inexistente → 404; con producto no disponible o categoría inactiva → 400.
4. Crear sin ítems o con cantidad < 1 → 400.
5. Sin token → 401. Rol distinto de CLIENTE en `POST /api/pedidos` → 403.
6. `GET /api/pedidos`: CLIENTE ve solo los propios; CAJA/COCINA/ADMIN ven todos.
7. `GET /api/pedidos/{id}`: CLIENTE ajeno → 403; inexistente → 404.
8. No existen endpoints de edición ni DELETE de pedidos; no se implementan transiciones de estado en este sprint.
9. `mvnw.cmd test` y `mvnw.cmd package` en verde.

## 8. Consideraciones de seguridad

- Autorización por rol y por propiedad verificada en el backend (`ConfiguracionSeguridad` y lógica de servicio); nunca solo en frontend.
- No se exponen contraseñas, tokens ni detalles internos (stack traces/SQL).
- No se aceptan ni almacenan datos de tarjetas bancarias.
- Respuestas y mensajes de error en español.

## 9. Pruebas necesarias

- **Unitarias (servicio):** ventana de entrega (horario de servicio, lead time, margen de cierre, mismo día, sin horarios disponibles, horas no alineadas a grilla), producto inexistente/no disponible/categoría inactiva, captura de precio vigente y cálculo de total, pedido sin ítems/cantidad inválida, visibilidad por rol/propiedad.
- **Integración (MockMvc, MySQL real):** creación exitosa (201), validaciones 400, 401/403 por rol y propiedad, 404, listados filtrados por rol.
- Datos de prueba con sufijo único (`System.nanoTime()`), siguiendo convención existente.

## 10. Configuración y migración

- Variables existentes: `DB_PASSWORD`, `JWT_SECRET` (obligatorias para app y tests).
- Propiedades de aplicación (no secretas): `cafeitsh.horario-apertura` (`08:00`), `cafeitsh.horario-cierre` (`20:00`), `cafeitsh.tiempo-preparacion-minutos` (`15`), `cafeitsh.margen-cierre-minutos` (`15`), zona de negocio `America/Mexico_City`.
- Migración: `V3__crear_tablas_pedidos.sql` en `backend/src/main/resources/db/migraciones/` (nunca editar V1/V2).

### Esquema V3

- `pedidos`: `id` (PK, AUTO_INCREMENT), `usuario_id` (FK `usuarios.id`, NOT NULL), `estado` (VARCHAR(30), NOT NULL), `fecha_hora_entrega` (DATETIME, NOT NULL), `metodo_pago_preferido` (VARCHAR(30), NULL), `total` (DECIMAL(10,2), NOT NULL), `creado_en`, `actualizado_en`.
- `pedido_items`: `id` (PK, AUTO_INCREMENT), `pedido_id` (FK `pedidos.id`, NOT NULL), `producto_id` (FK `productos.id`, NOT NULL), `cantidad` (INT, NOT NULL), `precio_unitario` (DECIMAL(10,2), NOT NULL), `subtotal` (DECIMAL(10,2), NOT NULL).

## 11. Fuera de alcance (Sprint 2)

- Transiciones de estado y endpoints de pago/cancelación/preparación/listo/entregado (Sprints 3 y 4).
- Tabla `pagos` y corte de caja (Sprint 3).
- Edición de pedidos, límites dinámicos por capacidad, notificaciones en tiempo real.
- Registro de cancelaciones pagadas/reembolsos.
