# Especificación: Autenticación y usuarios

**Módulo:** autenticacion-usuarios
**Sprint:** 0 (base transversal)
**Estado:** implementada

## 1. Objetivo

Permitir que los usuarios se autentiquen con JWT y que el rol de cada usuario determine qué operaciones puede realizar en el backend. Establecer la base de seguridad sobre la que se construirán los demás módulos.

## 2. Descripción funcional

- Cualquier visitante puede registrarse como **cliente**.
- Un usuario registrado inicia sesión y recibe un token JWT.
- El token se envía en cada petición protegida y el backend valida firma, vigencia y usuario activo.
- Un usuario con rol **ADMIN** administra la lista de usuarios (crear, consultar, actualizar, desactivar).
- El backend es la única autoridad que decide qué puede hacer cada rol.

## 3. Requisitos

1. Registro público limitado al rol CLIENTE.
2. Inicio de sesión que devuelva token JWT con expiración configurable.
3. Endpoints protegidos que exijan token válido.
4. Autorización por rol verificada en el backend.
5. Consulta del usuario autenticado (`/api/auth/me`) para que el frontend conozca su rol.
6. CRUD de usuarios administrado por ADMIN, con desactivación en lugar de borrado físico.

## 4. Reglas de negocio

- Roles permitidos: `ADMIN`, `CAJA`, `COCINA`, `CLIENTE`.
- Un usuario desactivado (`activo = false`) no puede iniciar sesión ni usar tokens vigentes.
- El correo es único para todos los usuarios.
- Un ADMIN no puede desactivar su propia cuenta (evita quedar sin administradores).
- El registro público siempre crea usuarios con rol CLIENTE; otros roles solo se crean desde un ADMIN autenticado.
- No existe borrado físico de usuarios: se desactivan.

**Decisión cerrada (aprobada por el equipo):** el registro público de clientes
se mantiene. Solo crea usuarios con rol CLIENTE; los demás roles los crea un ADMIN.

## 5. Datos de entrada y salida

### POST /api/auth/registro (público)

Entrada: `{"nombre": "...", "correo": "...", "contrasena": "..."}`
Salida 201: `{"id", "nombre", "correo", "rol"}` · Conflictos 409 (correo ya registrado) · 400 (validaciones).

### POST /api/auth/login (público)

Entrada: `{"correo": "...", "contrasena": "..."}`
Salida 200: `{"token", "tipo": "Bearer", "usuario": {"id", "nombre", "correo", "rol", "activo"}}` · 401 (credenciales inválidas o cuenta desactivada).

### GET /api/auth/me (autenticado)

Salida 200: `{"id", "nombre", "correo", "rol", "activo"}` · 401 sin token o inválido.

### Gestión de usuarios (rol ADMIN)

- `GET /api/usuarios` → 200 lista sin contraseñas.
- `POST /api/usuarios` → 201 crea usuario con rol a elección del ADMIN.
- `PUT /api/usuarios/{id}` → 200 actualiza nombre, correo, rol o estado activo.

Respuesta 403 si el rol no es ADMIN; 401 sin token.

### Errores (todos los endpoints)

```json
{"mensaje": "texto en español", "campos": {"campo": "error"} }
```

`campos` solo aparece en errores de validación. Nunca se devuelven stack traces ni consultas SQL.

## 6. Validaciones

- `nombre`: obligatorio, 2 a 100 caracteres.
- `correo`: obligatorio, formato de correo, máximo 255, único (en minúsculas).
- `contrasena`: obligatoria, mínimo 8 caracteres.
- `rol`: obligatorio y dentro del catálogo permitido (solo en creación/actualización por ADMIN).
- Todo lo validado en el backend; el frontend no es autoridad.

## 7. Criterios de aceptación

1. Registro crea usuario CLIENTE con contraseña hasheada con BCrypt (nunca en texto plano).
2. Login devuelve token firmado que `GET /api/auth/me` acepta.
3. Token con firma inválida o expirado → 401.
4. Usuario desactivado no puede iniciar sesión ni autenticar peticiones.
5. Un CLIENTE/CAJA/COCINA en `GET /api/usuarios` → 403.
6. Sin token en endpoint protegido → 401.
7. Correo duplicado → 409 sin filtrar información sensible.
8. `mvnw.cmd test` y `mvnw.cmd package` en verde.

## 8. Consideraciones de seguridad

- Contraseñas solo con BCrypt (`BCryptPasswordEncoder`), jamás en respuestas ni logs.
- Secreto del JWT desde la variable de entorno `JWT_SECRET`; nunca en el repositorio.
- Autorización siempre en el backend (el frontend solo oculta botones).
- Se valida también en endpoints aparentemente públicos (registro/login).
- La configuración CORS permite solo orígenes de desarrollo; se restringirá en producción.

## 9. Pruebas necesarias

- Unitarias: generación/validación de JWT (válido, expirado, firma alterada), reglas de negocio del servicio de usuarios.
- Integración (MockMvc contra MySQL real): registro → login → `me`, errores 401/403/409/400, protección por rol.

## 10. Configuración requerida

| Variable de entorno | Uso |
|---|---|
| `DB_PASSWORD` | conexión MySQL (ya existente) |
| `JWT_SECRET` | secreto de firma JWT, mínimo 32 caracteres |
| `ADMIN_CORREO` (opcional) | correo del primer ADMIN; por defecto `admin@cafeitsh.mx` |
| `ADMIN_PASSWORD` (opcional) | contraseña del primer ADMIN; si no existe ningún usuario y no está definida, se omite la creación y se registra un aviso |

El primer administrador se crea automáticamente solo si la tabla `usuarios` está vacía y `ADMIN_PASSWORD` está definida.
