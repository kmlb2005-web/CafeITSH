# Especificación: Caja

**Módulo:** caja  
**Sprint:** 3  
**Estado:** aprobada

## 1. Objetivo

Implementar el flujo de caja para pedidos anticipados: registro del pago por CAJA (transición `PENDIENTE -> PAGADO`), cancelación controlada según reglas aprobadas, y corte básico del día. El módulo debe dejar trazabilidad de quién realizó cada operación, cuándo y por qué, sin manejar datos de tarjetas ni reembolsos automáticos.

## 2. Descripción funcional

- Un usuario con rol **CAJA** o **ADMIN** registra el pago de un pedido en estado `PENDIENTE`, generando un registro de pago y cambiando el pedido a `PAGADO`.
- Los métodos de pago aceptados son únicamente **EFECTIVO** y **TRANSFERENCIA**.
- CAJA o ADMIN pueden cancelar pedidos en estados permitidos, registrando motivo, actor y fecha.
- CAJA o ADMIN pueden consultar el corte básico del día operativo (`America/Mexico_City`), que incluye solo pagos registrados en el día, con desglose por método y contador informativo de cancelados.
- El módulo no implementa reembolsos automáticos, ajustes manuales de monto ni integración bancaria. Tampoco implementa las transiciones de cocina/entrega (Sprint 4).

## 3. Requisitos

1. Migración `V4` con tabla `pagos` y campos de auditoría de cancelación/reembolso manual en `pedidos` (sin modificar V1–V3).
2. Ampliar el catálogo de intención de pago del pedido para aceptar `EFECTIVO_EN_CAJA` y `TRANSFERENCIA`.
3. `POST /api/pedidos/{id}/pago` (CAJA/ADMIN): registra pago y transiciona `PENDIENTE -> PAGADO`.
4. `POST /api/pedidos/{id}/cancelar` (autenticado, reglas por rol/estado): cancela pedidos permitidos con trazabilidad.
5. `GET /api/caja/corte?fecha=YYYY-MM-DD` (CAJA/ADMIN): devuelve corte básico del día solicitado (por defecto, día actual en `America/Mexico_City`).
6. Validación y autorización exclusivamente en backend.
7. Sin endpoints DELETE y sin edición de ítems/montos del pedido.

## 4. Reglas de negocio

### 4.1 Métodos de pago

- Métodos aceptados: **EFECTIVO** y **TRANSFERENCIA**. **Nunca se capturan ni almacenan datos de tarjetas bancarias.**
- **Intención de pago del pedido** (`metodoPagoPreferido`, opcional, definida en Sprint 2): se amplía el catálogo a `EFECTIVO_EN_CAJA` y `TRANSFERENCIA`. Es una preferencia del cliente y **no equivale a pago confirmado**.
- **Pago real:** lo registra CAJA/ADMIN al cobrar/verificar. El método real queda en el registro de `pagos`.
- Para transferencia, el sistema solo registra que CAJA/ADMIN verificó la operación manualmente. No hay integración bancaria ni validación automática de fondos. Puede almacenarse una referencia opcional no sensible (folio/comprobante interno) sin datos bancarios del cliente.

### 4.2 Registro de pago (`PENDIENTE -> PAGADO`)

- Solo se puede pagar un pedido en estado `PENDIENTE`.
- Roles permitidos: **CAJA** y **ADMIN**.
- Al registrar el pago:
  - se crea un registro en `pagos` con `pedido_id`, `monto`, `metodo_pago`, `fecha_pago`, `registrado_por` y `referencia` opcional;
  - el pedido pasa a estado `PAGADO`;
  - el monto del pago debe coincidir con el `total` del pedido (no se permiten ajustes manuales de monto).
- Si el pedido ya no está en `PENDIENTE` (pagado, cancelado, etc.), se rechaza con 409/conflicto de negocio y mensaje claro.
- La validación de estado y la actualización deben ejecutarse de forma segura ante concurrencia (transacción; si ya cambió de estado, no aplicar la transición).

### 4.3 Cancelación

- La cancelación requiere `motivoCancelacion` obligatorio (texto corto) y registra `canceladoEn` y `canceladoPor`.
- **CLIENTE:** puede cancelar únicamente sus propios pedidos en estado `PENDIENTE`.
- **CAJA y ADMIN:** pueden cancelar pedidos en estado `PENDIENTE` o `PAGADO`, solo **antes de `EN_PREPARACION`**.
- **No cancelable** en `EN_PREPARACION`, `LISTO`, `ENTREGADO` ni `CANCELADO`.
- Un pedido `PAGADO` que cocina no pueda preparar se maneja como cancelación operativa por CAJA/ADMIN en `PAGADO` antes de `EN_PREPARACION`, con motivo obligatorio (p. ej. “Sin capacidad de preparación”).
- Esta regla amplía la definición previa de Sprint 2 (que limitaba la cancelación a `PENDIENTE`); la ampliación queda aprobada explícitamente para Sprint 3.

### 4.4 Reembolsos y ajustes

- **No hay reembolsos automáticos ni ajustes manuales de monto** en Sprint 3.
- Al cancelar un pedido `PAGADO`, el sistema debe permitir registrar constancia de reembolso manual:
  - `requiereReembolso` (booleano, opcional/derivable por operación; recomendado explícito en solicitud de cancelación),
  - `reembolsoManualRegistrado` (booleano, por defecto `false`),
  - `observacionReembolso` (texto opcional).
- El reembolso se ejecuta fuera del sistema; CAJA/ADMIN deja constancia. La existencia de pago/reembolso no cambia el hecho de que el pedido queda `CANCELADO`.

### 4.5 Corte básico de caja

- Corte del **día operativo** en zona `America/Mexico_City`.
- Por defecto, fecha actual; se permite consultar por `fecha=YYYY-MM-DD`.
- **Incluye solo pedidos/pagos registrados como `PAGADO` en el día** (fecha de pago en la zona operativa).
- El corte devuelve:
  - fecha del corte;
  - total cobrado;
  - cantidad de pagos;
  - desglose por método (`EFECTIVO`, `TRANSFERENCIA`) con cantidad y monto;
  - contador informativo de pedidos cancelados en el día (cantidad; opcionalmente monto de pedidos pagados cancelados como referencia), **sin sumarlos al ingreso neto**.
- **Sin rangos de fechas** en Sprint 3.

### 4.6 Estados y transiciones

- Catálogo fijo: `PENDIENTE`, `PAGADO`, `EN_PREPARACION`, `LISTO`, `ENTREGADO`, `CANCELADO` (sin cambios).
- Transiciones implementadas en Sprint 3:
  - `PENDIENTE -> PAGADO` (CAJA/ADMIN).
  - `PENDIENTE -> CANCELADO` (CLIENTE dueño, CAJA o ADMIN).
  - `PAGADO -> CANCELADO` (CAJA o ADMIN, solo antes de `EN_PREPARACION`).
- Transiciones de Sprint 4 (`PAGADO -> EN_PREPARACION -> LISTO`, y `ENTREGADO`) no se implementan aquí.

## 5. Datos de entrada y salida

### POST /api/pedidos/{id}/pago (CAJA/ADMIN)

Entrada:
```json
{
  "metodoPago": "EFECTIVO",
  "referencia": "FOLIO-OPCIONAL"
}
```

- `metodoPago`: obligatorio, `EFECTIVO` o `TRANSFERENCIA`.
- `referencia`: opcional, texto corto no sensible (máx. recomendado 100), sin datos bancarios/tarjeta.

Salida 200 (ejemplo):
```json
{
  "id": 10,
  "estado": "PAGADO",
  "total": 50.00,
  "pago": {
    "id": 3,
    "metodoPago": "EFECTIVO",
    "monto": 50.00,
    "fechaPago": "2026-11-20T14:20:00",
    "registradoPor": { "id": 2, "nombre": "Caja", "correo": "caja@cafeitsh.mx" },
    "referencia": null
  }
}
```

### POST /api/pedidos/{id}/cancelar (autenticado, según reglas)

Entrada:
```json
{
  "motivoCancelacion": "Cliente solicitó cancelación",
  "requiereReembolso": false,
  "reembolsoManualRegistrado": false,
  "observacionReembolso": null
}
```

- `motivoCancelacion`: obligatorio, no vacío.
- `requiereReembolso`: opcional (recomendado obligatorio en cancelación de `PAGADO`; por defecto `false` en `PENDIENTE`).
- `reembolsoManualRegistrado`: opcional, por defecto `false`.
- `observacionReembolso`: opcional.

Salida 200 (ejemplo): pedido con estado `CANCELADO` y campos de auditoría de cancelación.

### GET /api/caja/corte?fecha=2026-11-20 (CAJA/ADMIN)

Salida 200 (ejemplo):
```json
{
  "fecha": "2026-11-20",
  "zonaHoraria": "America/Mexico_City",
  "totalCobrado": 150.00,
  "cantidadPagos": 3,
  "desglosePorMetodo": [
    { "metodoPago": "EFECTIVO", "cantidad": 2, "monto": 100.00 },
    { "metodoPago": "TRANSFERENCIA", "cantidad": 1, "monto": 50.00 }
  ],
  "cancelados": {
    "cantidad": 1,
    "montoPagadoCancelado": 50.00
  }
}
```

### Errores

Formato uniforme `RespuestaError` en español: `{"mensaje", "timestamp", "campos"?}`.
- 400: validación, método inválido, motivo vacío, fecha inválida.
- 401: sin autenticación.
- 403: rol no autorizado o cliente cancelando pedido ajeno/no permitido.
- 404: pedido no encontrado.
- 409: transición inválida por estado actual/concurrencia (p. ej. ya pagado, ya cancelado, ya en preparación).

## 6. Validaciones

- `metodoPago` obligatorio y en catálogo permitido.
- `monto` del pago igual al total del pedido.
- `motivoCancelacion` obligatorio, texto no vacío, longitud razonable.
- `fecha` de corte con formato `YYYY-MM-DD`.
- Transición permitida solo desde estados válidos y por rol autorizado.
- Verificación de propiedad para CLIENTE al cancelar.
- No aceptar ni persistir datos de tarjeta/bancarios sensibles.

## 7. Criterios de aceptación

1. CAJA/ADMIN puede registrar pago de pedido `PENDIENTE` por EFECTIVO o TRANSFERENCIA y el pedido queda `PAGADO` con registro en `pagos`.
2. Pagar un pedido no `PENDIENTE` devuelve 409.
3. CLIENTE puede cancelar su pedido `PENDIENTE`; no puede cancelar ajeno ni `PAGADO`.
4. CAJA/ADMIN puede cancelar `PENDIENTE` o `PAGADO` antes de `EN_PREPARACION`; cancelar `EN_PREPARACION/LISTO/ENTREGADO` devuelve 409.
5. Cancelación de `PAGADO` permite registrar constancia de reembolso manual sin reembolso automático.
6. Corte básico devuelve solo pagos del día solicitado (`America/Mexico_City`), con totales y desglose por método; cancelados no suman al ingreso neto.
7. Sin endpoints DELETE, sin ajustes manuales de monto, sin datos de tarjeta.
8. `mvnw.cmd test` y `mvnw.cmd package` en verde.

## 8. Consideraciones de seguridad

- Autorización por rol y propiedad en backend (`ConfiguracionSeguridad` + servicio).
- Reglas en `requestMatchers` de `ConfiguracionSeguridad` más validaciones de rol y propiedad en el servicio para operaciones sensibles.
- No exponer contraseñas, tokens, SQL ni stack traces.
- No registrar datos sensibles en logs/mensajes.
- Operaciones de transición en transacción para reducir condiciones de carrera.

## 9. Pruebas necesarias

- **Unitarias (servicio):** pago válido, pago en estado inválido, método inválido, monto distinto, cancelación por rol/estado, cancelación pagado con constancia de reembolso, pedido pagado no preparable (cancelación operativa), cálculo de corte por día/zona/desglose.
- **Integración (MockMvc, MySQL real):** endpoints de pago/cancelación/corte, 401/403/404/409/400, visibilidad por rol, persistencia de `pagos` y auditoría de cancelación.
- Datos de prueba con sufijo único (`System.nanoTime()`), siguiendo convención existente.

## 10. Configuración y migración

- Variables existentes: `DB_PASSWORD`, `JWT_SECRET`.
- Zona de negocio: `America/Mexico_City` (ya usada en pedidos).
- Migración `V4__crear_tabla_pagos_y_auditoria_cancelacion.sql`:
  - crear tabla `pagos` (FK a `pedidos`, FK/valor de usuario registrador, método, monto, fecha, referencia opcional);
  - agregar columnas de auditoría en `pedidos` para cancelación/reembolso manual (p. ej. `cancelado_en`, `cancelado_por_usuario_id`, `motivo_cancelacion`, `requiere_reembolso`, `reembolso_manual_registrado`, `observacion_reembolso`), todas compatibles con registros existentes;
  - no modificar V1–V3.

## 11. Fuera de alcance (Sprint 3)

- Reembolsos automáticos, integración bancaria, conciliación con pasarelas.
- Ajustes manuales de monto, parcialidades, propinas o múltiples pagos por pedido.
- Corte por rango de fechas, exportaciones, reportes avanzados.
- Transiciones de cocina/entrega (`EN_PREPARACION`, `LISTO`, `ENTREGADO`) — Sprint 4.
- Edición de pedidos, endpoints DELETE.
- Captura o almacenamiento de datos de tarjetas bancarias.
