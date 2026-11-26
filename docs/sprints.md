# Plan: Backend cafeITSH — por sprints

> Plan original acordado con el equipo. Reproducido tal cual fue aprobado.
> **Estado (25/11/2026):** Sprints 0, 1, 2, 3 y 4 completados y verificados; Sprint 5 pendiente — detalle en `docs/bitacora-desarrollo.md`.

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

## Sprint 2 — Pedidos ✅ completado

Spec → aprobación → creación/consulta de pedidos con ventana de entrega dentro del horario de servicio (misma zona `America/Mexico_City`, mismo día, parámetros configurables de apertura/cierre, lead y margen; sin exigir alineación a grilla) → pruebas → reporte.

- **Alcance implementado (ver `specs/pedidos/spec.md` y bitácora):**
  - Migración `V3` con `pedidos` y `pedido_items` (sin `pagos`, diferido a Sprint 3).
  - `POST /api/pedidos` (CLIENTE): crea en `PENDIENTE`, valida horario/ventana, captura precio vigente y total.
  - `GET /api/pedidos` y `GET /api/pedidos/{id}`: visibilidad por rol (CLIENTE propios; CAJA/COCINA/ADMIN todos).
  - Sin endpoints de edición, DELETE ni transiciones de estado (Sprints 3-4).
- **Depende de:** Sprint 0 (usuarios y roles), Sprint 1 (productos/categorías).
- **Decisiones registradas:** horario/lead/margen configurables (inicial 08:00–20:00, 15/15 min), inmutabilidad al crear, sin límites de ítems/monto, intención `EFECTIVO_EN_CAJA` opcional, cancelación solo en `PENDIENTE` diferida a Sprint 3 (detalle en spec/bitácora).
- **Criterio de cierre:** `mvnw.cmd test` (71) + `mvnw.cmd package` en verde, bitácora actualizada.

## Sprint 3 — Caja ✅ completado

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

## Sprint 4 — Cocina ✅ completado

Plan original: Spec → aprobación → transiciones En preparación → Listo → pruebas → reporte.

**Cierre (25/11/2026):** spec aprobada en `specs/cocina/spec.md`; implementados
`GET /api/cocina/pedidos` y las transiciones a `EN_PREPARACION`, `LISTO` y
`ENTREGADO`. Reglas aprobadas: solo desde `PAGADO`, sin saltos, COCINA/ADMIN en
preparación y listo, y COCINA/CAJA/ADMIN en entrega. Verificado con 99 pruebas en
verde y `mvnw.cmd package` BUILD SUCCESS contra MySQL real (Docker, puerto 3307).

- **Alcance implementado:**
  - `GET /api/cocina/pedidos`: pedidos `PAGADO` ordenados por hora de entrega.
  - `POST /api/pedidos/{id}/en-preparacion` y `/listo` (COCINA/ADMIN).
  - `POST /api/pedidos/{id}/entregado` (COCINA/CAJA/ADMIN).
  - Sin migración nueva, sin notificaciones y sin cambios en cancelación de caja.
- **Ajustes de cierre:** bloqueo pesimista en pago/cancelación/transiciones,
  corte que no suma pagos de pedidos cancelados, errores 401/403 con la misma
  forma `{mensaje, timestamp}` y cobertura de ADMIN en la vista de cocina.
- **Depende de:** Sprint 2 (pedidos) y Sprint 3 (estado Pagado como puerta de entrada a cocina).
- **Decisiones cerradas:**
  - No: un pedido no pagado no puede entrar a preparación; solo se acepta `PAGADO`.
  - No: no se permiten saltos de estado.
  - `ENTREGADO` lo registra COCINA, CAJA o ADMIN desde `LISTO`, al entregar el pedido.
  - Solo consulta: sin notificaciones automáticas al cliente en este sprint.
- **Criterio de cierre:** `mvnw.cmd test` (99 pruebas) y `mvnw.cmd package` en verde, bitácora actualizada.

## Sprint 5 — Cierre ⏳ pendiente

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
