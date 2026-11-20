# Plan: Backend cafeITSH — por sprints

> Plan original acordado con el equipo. Reproducido tal cual fue aprobado.
> **Estado (19/11/2026):** Sprint 0 ✅ y Sprint 1 ✅ completados — detalle en `docs/bitacora-desarrollo.md`.

**Regla de ritmo:** cada módulo = un sprint. Al terminar un sprint entrego reporte + documentación, y **espero tu autorización explícita** antes de tocar el siguiente. Sin autorización, no avanzo.

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

## Sprint 2 — Pedidos

Spec → aprobación → creación con regla ≥10 min, máquina de estados (Pendiente → Pagado → En preparación → Listo → Entregado / Cancelado), consulta por rol → pruebas → reporte → **espero tu autorización**.

## Sprint 3 — Caja

Spec → aprobación → pago, cancelación, corte básico → pruebas → reporte → **espero tu autorización**.

## Sprint 4 — Cocina

Spec → aprobación → transiciones En preparación → Listo → pruebas → reporte → **espero tu autorización**.

## Sprint 5 — Cierre

Suite verde completa + `docs/endpoints-api.md` (documentación de endpoints para el frontend) → recién ahí pasamos a ver el frontend.

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
