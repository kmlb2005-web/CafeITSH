# Plan: Backend cafeITSH — por sprints

> Plan original acordado con el equipo. Reproducido tal cual fue aprobado.
> **Estado (19/11/2026):** Sprint 0 ✅ y Sprint 1 ✅ completados — detalle en `docs/bitacora-desarrollo.md`.

**Regla de ritmo:** cada módulo = un sprint. Al terminar un sprint entrego reporte + documentación, y **espero tu autorización explícita** antes de tocar el siguiente. Sin autorización, no avanzo.

**Nota para el equipo:** los sprints 2 en adelante los ejecutarán los compañeros. Cada sección incluye el alcance mínimo, las dependencias y **las preguntas que deben resolverse con el patrocinador antes de redactar la spec** (regla del proyecto: no se inventan requisitos ni reglas de negocio).

---

## Sprint 0 — Base transversal (previo a los módulos) ✅ completado

- Integrar **Flyway** (dependencias + `application.yaml` + `V1__crear_esquema_inicial.sql`).
- Estructura de paquetes (`config/ controller/ service/ repository/ entity/ dto/ security/ exception/`).
- Manejo centralizado de errores en español.
- Seguridad base: BCrypt + **JWT** (login `POST /api/auth/login`, filtro, autorización por rol).
- Spec de `autenticacion-usuarios/` aprobada antes de implementar.
- Criterio de cierre: `mvnw.cmd test` + `mvnw.cmd package` en verde.

## Sprint 1 — Menú ✅ completado

Spec → aprobación → CRUD categorías/productos + consulta pública → pruebas → reporte → **espero tu autorización**.

---

## Sprint 2 — Pedidos

Spec → aprobación → creación con regla ≥10 min, máquina de estados (Pendiente → Pagado → En preparación → Listo → Entregado / Cancelado), consulta por rol → pruebas → reporte → **espero tu autorización**.

- **Alcance mínimo:**
  - Migración `V3` con las tablas `pedidos` y `pedido_items` (el diseño tentativo ya está en la bitácora del Sprint 0; `pagos` puede quedar para el Sprint 3).
  - `POST /api/pedidos` (rol CLIENTE): valida la regla de **≥10 minutos de anticipación** y captura los ítems con el precio vigente al momento de pedir.
  - Consulta de pedidos por rol (cliente ve los suyos; caja/cocina ven los que les corresponda).
  - Transición inicial **Pendiente** al crear el pedido; el resto de transiciones se reparten en Sprints 3 y 4.
- **Depende de:** Sprint 0 (usuarios y roles), Sprint 1 (productos disponibles y categorías).
- **Preguntas abiertas (resolver con el patrocinador antes de la spec):**
  - ¿Se puede cancelar un pedido antes de pagarlo? ¿Quién y hasta qué estado?
  - ¿Hay anticipación **máxima** (hasta cuánto tiempo antes se puede pedir)?
  - ¿El cliente elige fecha/hora de entrega o se calcula automáticamente?
  - ¿Se puede modificar un pedido ya confirmado (agregar/quitar ítems)?
  - ¿Hay límite de ítems o de monto por pedido?
- **Criterio de cierre:** `mvnw.cmd test` + `mvnw.cmd package` en verde, bitácora actualizada.

## Sprint 3 — Caja

Spec → aprobación → pago, cancelación, corte básico → pruebas → reporte → **espero tu autorización**.

- **Alcance mínimo:**
  - Transición **Pagado** registrada por rol CAJA.
  - Cancelación de pedidos según las reglas que se definan en la spec.
  - Corte de caja básico (reporte del día).
  - Migración para la tabla `pagos` si la spec la requiere.
- **Depende de:** Sprint 2 (tabla `pedidos` y estados).
- **Preguntas abiertas:**
  - ¿Qué métodos de pago se aceptan (efectivo, transferencia, otro)? **Nunca datos de tarjeta.**
  - ¿El corte incluye solo pedidos pagados del día, o también cancelados? ¿Se puede cortar por rango de fechas?
  - ¿Quién puede cancelar un pedido ya pagado y cómo queda registrada la cancelación?
  - ¿Hay reembolsos o ajustes manuales?
- **Criterio de cierre:** igual que Sprint 2.

## Sprint 4 — Cocina

Spec → aprobación → transiciones En preparación → Listo → pruebas → reporte → **espero tu autorización**.

- **Alcance mínimo:**
  - Transiciones **En preparación** y **Listo** gestionadas por rol COCINA.
  - Vista de pedidos pendientes de preparación.
  - Definir quién marca **Entregado** (puede ser caja, cocina o el cliente al recoger) según la spec.
- **Depende de:** Sprint 2 (pedidos) y Sprint 3 (estado Pagado como puerta de entrada a cocina).
- **Preguntas abiertas:**
  - ¿Un pedido **no pagado** puede entrar a preparación? (respuesta esperada: no, pero confirmar)
  - ¿Se puede saltar de un estado a otro distinto al siguiente?
  - ¿Quién marca **Entregado** y en qué momento?
  - ¿Hay notificaciones al cliente (pantalla de espera, etc.) o solo consulta?
- **Criterio de cierre:** igual que Sprint 2.

## Sprint 5 — Cierre

Suite verde completa + `docs/endpoints-api.md` (documentación de endpoints para el frontend) → recién ahí pasamos a ver el frontend.

- **Alcance mínimo:** suite completa en verde, `docs/endpoints-api.md` con todos los endpoints y sus contratos, revisión de seguridad (CORS restringido para producción), documentación sincronizada.
- **Depende de:** Sprints 2, 3 y 4.
- **Preguntas abiertas:** si se añaden funcionalidades transversales (estadísticas, reportes) decidir si entran aquí o en un sprint extra.

---

## Documentación por sprint

Al cerrar cada sprint, se crea o actualiza en `docs/`:

**`docs/bitacora-desarrollo.md`** — una bitácora con una sección por sprint:

```markdown
## Sprint N — <módulo> (fecha)

- **Objetivo:**
- **Spec asociada:** specs/<modulo>/
- **Archivos creados:** ...
- **Archivos modificados:** ...
- **Endpoints agregados:** (método + ruta)
- **Pruebas ejecutadas:** comando y resultado
- **Decisiones tomadas:**
- **Pendientes / notas para el siguiente sprint:**
```

Así cualquier sesión futura (o el equipo) ve de un vistazo qué se hizo, en qué orden y con qué resultado. Además, el reporte final de cada sprint entregado en el chat refleja el mismo contenido que queda escrito en la bitácora — nunca se afirman pruebas que no se ejecutaron.
