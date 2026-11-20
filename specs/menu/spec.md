# Especificación: Menú (categorías y productos)

**Módulo:** menu
**Sprint:** 1
**Estado:** pendiente de aprobación

## 1. Objetivo

Permitir consultar el menú de la cafetería (público, sin autenticación) y
administrar categorías y productos desde el rol ADMIN.

## 2. Descripción funcional

- Cualquier visitante puede ver las categorías activas y los productos
  disponibles, sin necesidad de token.
- Un ADMIN crea, edita y desactiva categorías y productos.
- Los productos se organizan dentro de una categoría obligatoria.

## 3. Requisitos

1. Consulta pública de menú: categorías activas y sus productos disponibles.
2. Filtrado opcional de productos por categoría.
3. CRUD de categorías por ADMIN.
4. CRUD de productos por ADMIN, con categoría obligatoria.
5. Borrado lógico (desactivación), nunca físico: un producto ya pedido debe
   seguir existiendo para que los pedidos anteriores no se rompan.
6. La API es la única fuente de verdad; el frontend solo muestra lo que el
   backend devuelve.

## 4. Reglas de negocio

- Categorías: nombre único (case-insensitive), se desactivan con `activa = false`.
- Un producto pertenece exactamente a una categoría existente y activa.
- Un producto tiene `precio >= 0` y `disponible` (booleano).
- **Desactivar una categoría oculta sus productos en la consulta pública**
  (no es necesario desactivarlos uno por uno).
- Un producto desactivado no aparece en el menú público, pero **sigue
  referenciable por los pedidos existentes** (no se borra la fila).
- Un ADMIN no puede desactivar una categoría que tenga productos activos:
  primero se desactivan o migran sus productos (evita productos huérfanos
  visibles).
- Los nombres se guardan tal cual se escriben (respetando mayúsculas), la
  comparación de unicidad normaliza a minúsculas.

> **Nota de alcance:** no hay control de inventario, ni precio promocional,
> ni imágenes de producto en este sprint. Si los PDFs de pantallas muestran
> imágenes, se agrega una columna `imagen_url` en la migración V3 con
> aprobación del equipo.

## 5. Datos de entrada y salida

### Consulta pública

- `GET /api/menu/categorias` → 200:

```json
[{"id": 1, "nombre": "Bebidas"}]
```

- `GET /api/menu/productos` → 200 lista de productos disponibles en categorías
  activas:

```json
[{"id": 3, "nombre": "Café americano", "descripcion": "...",
  "precio": 25.00, "disponible": true,
  "categoria": {"id": 1, "nombre": "Bebidas"}}]
```

- `GET /api/menu/productos?categoria=1` → filtrado por categoría; categoría
  inexistente o inactiva → 404.

### Gestión (rol ADMIN)

- `GET /api/categorias` → 200 listado completo (incluye inactivas) ·
  `POST /api/categorias` → 201 · `PUT /api/categorias/{id}` → 200 ·
  `PUT /api/categorias/{id}/desactivar` → 200
- `GET /api/productos` → 200 listado completo (incluye no disponibles) ·
  `POST /api/productos` → 201 · `PUT /api/productos/{id}` → 200 ·
  `PUT /api/productos/{id}/desactivar` → 200

> Los GET de gestión son la lectura del CRUD: el frontend admin necesita
> ver también los registros desactivados para poder reactivarlos (el menú
> público solo muestra los activos).

Entrada de categoría: `{"nombre": "Bebidas"}` (y `{"activa": true/false}` en actualización).
Entrada de producto:

```json
{"nombre": "...", "descripcion": "...", "precio": 25.00,
 "disponible": true, "categoriaId": 1}
```

Errores: 400 validaciones · 403 rol insuficiente · 404 inexistente ·
409 nombre duplicado · 401 sin token. Formato de error igual al Sprint 0
(`RespuestaError` en español).

## 6. Validaciones

- `nombre`: obligatorio, 2 a 100 caracteres.
- `descripcion`: opcional, máximo 500 caracteres.
- `precio`: obligatorio, `>= 0`, hasta 2 decimales.
- `categoriaId`: obligatorio y debe existir estar activa.
- `disponible` / `activa`: booleanos obligatorios en actualización.
- Todo validado en el backend.

## 7. Criterios de aceptación

1. `GET /api/menu/categorias` y `GET /api/menu/productos` responden sin token.
2. Un producto en categoría inactiva no aparece en la consulta pública.
3. Un CLIENTE/CAJA/COCINA en cualquier endpoint de gestión → 403.
4. ADMIN puede crear, editar y desactivar categorías y productos.
5. Categoría con productos activos no se puede desactivar → 400 con mensaje claro.
6. Nombre de categoría duplicado → 409.
7. Precio negativo o categoría inexistente → 400 con detalle de campos.
8. La desactivación no borra filas: los pedidos futuros/previos siguen
   referenciando el producto.
9. `mvnw.cmd test` y `mvnw.cmd package` en verde.

## 8. Consideraciones de seguridad

- Endpoints de gestión protegidos con `hasRole("ADMIN")` (ya configurado en
  `ConfiguracionSeguridad` para `/api/categorias/**` y `/api/productos/**`
  — agregar esas rutas a la configuración existente).
- La consulta pública no expone campos internos (fechas, estados de borrado).
- Validación de datos en el backend, sin confiar en el frontend.

## 9. Pruebas necesarias

- Unitarias: reglas de negocio (desactivación con productos activos,
  pertenencia a categoría activa, unicidad normalizada).
- Integración (MockMvc contra MySQL real): consulta pública sin token,
  403 por rol, CRUD completo, 409, 400 con detalle, ocultamiento al
  desactivar categoría.

## 10. Configuración requerida

Sin variables nuevas. Reutiliza `DB_PASSWORD` y `JWT_SECRET` del Sprint 0.

Migración: `V2__crear_tablas_menu.sql` (tablas `categorias` y `productos`).
