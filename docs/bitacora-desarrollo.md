# Bitácora de desarrollo

Registro de los sprints terminados del backend. Cada sprint se cierra con
sus pruebas ejecutadas y queda pendiente de autorización para el siguiente.

Estado general: **Sprint 0 y Sprint 1 completados** · Frontend pendiente (sin proyecto Flutter aún).

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
