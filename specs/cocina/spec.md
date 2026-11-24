# Especificación: Cocina

**Módulo:** cocina  
**Sprint:** 4  
**Estado:** aprobada

## 1. Objetivo

Implementar la vista de pedidos pendientes de preparación y las transiciones de cocina y entrega: `PAGADO -> EN_PREPARACION -> LISTO -> ENTREGADO`, sin saltos de estado, con autorización verificada en el backend.

## 2. Descripción funcional

- COCINA o ADMIN consultan los pedidos en estado `PAGADO` pendientes de preparación.
- COCINA o ADMIN inician la preparación de un pedido `PAGADO`.
- COCINA o ADMIN marcan como `LISTO` un pedido `EN_PREPARACION`.
- COCINA, CAJA o ADMIN marcan como `ENTREGADO` un pedido `LISTO`.
- La vista de pendientes es de solo consulta: no hay notificaciones automáticas al cliente en este sprint; el cliente continúa consultando sus pedidos mediante los endpoints existentes.
- El catálogo de estados no se modifica y no se permiten transiciones distintas a las indicadas.

## 3. Requisitos

1. `GET /api/cocina/pedidos` (COCINA/ADMIN): devuelve los pedidos `PAGADO`, ordenados por `fechaHoraEntrega` ascendente.
2. `POST /api/pedidos/{id}/en-preparacion` (COCINA/ADMIN): transiciona `PAGADO -> EN_PREPARACION`.
3. `POST /api/pedidos/{id}/listo` (COCINA/ADMIN): transiciona `EN_PREPARACION -> LISTO`.
4. `POST /api/pedidos/{id}/entregado` (COCINA/CAJA/ADMIN): transiciona `LISTO -> ENTREGADO`.
5. Las reglas de rol y estado se validan en servicio, además de las reglas de seguridad web.
6. No se requiere cuerpo en las solicitudes de transición.
7. No se agregan migraciones Flyway: se reutiliza el campo `actualizadoEn` existente y no se introduce auditoría nueva.
8. Sin endpoints DELETE, sin edición de pedidos y sin notificaciones automáticas.

## 4. Reglas de negocio

### 4.1 Puerta de entrada a cocina

- Solo puede entrar a preparación un pedido en estado `PAGADO`.
- Un pedido `PENDIENTE`, `CANCELADO`, `EN_PREPARACION`, `LISTO` o `ENTREGADO` no puede iniciar preparación.
- La cancelación operativa de un pedido `PAGADO` que cocina no pueda preparar se sigue gestionando desde caja según `specs/caja/spec.md`, antes de iniciar la preparación.

### 4.2 Transiciones permitidas

| Estado actual | Estado nuevo | Roles permitidos |
|---|---|---|
| `PAGADO` | `EN_PREPARACION` | COCINA, ADMIN |
| `EN_PREPARACION` | `LISTO` | COCINA, ADMIN |
| `LISTO` | `ENTREGADO` | COCINA, CAJA, ADMIN |

- No se permiten saltos de estado. Cualquier transición desde otro estado o hacia otro estado se rechaza con 409.
- `ENTREGADO` se registra cuando el pedido termina de entregarse al cliente.
- CLIENTE no puede ejecutar ninguna transición de este módulo.

### 4.3 Vista de pendientes

- La lista contiene únicamente pedidos en estado `PAGADO`.
- Se ordena por `fechaHoraEntrega` ascendente; a igualdad de hora, por `id` ascendente.
- Cada elemento usa el DTO existente `RespuestaPedido`, con cliente, ítems y pago vigente.
- La vista no cambia el estado de los pedidos.

### 4.4 Estados

- Catálogo fijo sin cambios: `PENDIENTE`, `PAGADO`, `EN_PREPARACION`, `LISTO`, `ENTREGADO`, `CANCELADO`.
- Catálogo definido en `backend/src/main/java/com/cafeitsh/backend/entity/EstadoPedido.java`.
- Los estados posteriores a `PAGADO` ya son no cancelables según la especificación de caja.

## 5. Datos de entrada y salida

### GET /api/cocina/pedidos (COCINA/ADMIN)

Salida 200: arreglo de `RespuestaPedido`. Ejemplo resumido:

```json
[
  {
    "id": 10,
    "estado": "PAGADO",
    "fechaHoraEntrega": "2026-11-24T14:00:00",
    "total": 50.00,
    "cliente": { "id": 5, "nombre": "Cliente", "correo": "cliente@cafeitsh.mx" },
    "items": [],
    "pago": {
      "id": 3,
      "metodoPago": "EFECTIVO",
      "monto": 50.00,
      "fechaPago": "2026-11-24T12:20:00"
    }
  }
]
```

### POST /api/pedidos/{id}/en-preparacion, /listo, /entregado

Sin cuerpo. Salida 200: `RespuestaPedido` con el estado actualizado.

### Errores

Formato uniforme `RespuestaError` en español: `{"mensaje", "timestamp", "campos"?}`.

- 401: sin autenticación o token inválido.
- 403: rol no autorizado para la operación o para la vista.
- 404: pedido no encontrado.
- 409: transición inválida por estado actual.

## 6. Validaciones

- El pedido debe existir antes de validar la transición.
- El rol del actor se verifica en servicio para cada operación.
- El estado actual debe coincidir exactamente con el estado de origen de la transición.
- La autorización web complementa la validación de servicio; nunca se confía solo en el frontend.
- Las operaciones de transición se ejecutan en transacción.

## 7. Criterios de aceptación

1. COCINA y ADMIN pueden listar pedidos `PAGADO` ordenados por hora de entrega.
2. CLIENTE y CAJA no pueden acceder a `GET /api/cocina/pedidos`.
3. COCINA/ADMIN pueden ejecutar `PAGADO -> EN_PREPARACION` y `EN_PREPARACION -> LISTO`.
4. COCINA/CAJA/ADMIN pueden ejecutar `LISTO -> ENTREGADO`.
5. CLIENTE no puede ejecutar ninguna transición del módulo.
6. Intentar una transición desde un estado distinto al esperado devuelve 409 y no modifica el pedido.
7. Un pedido inexistente devuelve 404.
8. No se agregan migraciones, notificaciones, edición de pedidos ni endpoints DELETE.
9. `mvnw.cmd test` y `mvnw.cmd package` en verde.

## 8. Consideraciones de seguridad

- `GET /api/cocina/**`: COCINA o ADMIN.
- `POST /api/pedidos/*/en-preparacion` y `/listo`: COCINA o ADMIN.
- `POST /api/pedidos/*/entregado`: COCINA, CAJA o ADMIN.
- Verificación adicional de rol en `ServicioCocina`.
- No exponer contraseñas, tokens, SQL ni stack traces.

## 9. Pruebas necesarias

- **Unitarias (servicio):** listado por estado, transiciones válidas por rol, rechazo de saltos de estado, rechazo por rol, pedido inexistente.
- **Integración (MockMvc, MySQL real):** endpoints de vista y transiciones, códigos 401/403/404/409/200, persistencia del estado final.
- Datos de prueba con sufijo único (`System.nanoTime()`), siguiendo la convención existente.

## 10. Configuración y migración

- Sin variables de entorno nuevas.
- Sin migraciones nuevas: la tabla `pedidos` y el campo `actualizadoEn` ya existen.
- Zona horaria de negocio: `America/Mexico_City` (ya usada en pedidos y caja).

## 11. Fuera de alcance (Sprint 4)

- Notificaciones automáticas al cliente, pantallas Flutter o nuevos canales de comunicación.
- Cambios en el catálogo de estados o en reglas de cancelación de caja.
- Auditoría específica de transiciones de cocina.
- Edición de pedidos, reembolsos, reportes avanzados ni endpoints DELETE.
- Frontend (Sprint 5 y posteriores).
