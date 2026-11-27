# Especificación: Cierre del backend

**Módulo:** cierre  
**Sprint:** 5  
**Estado:** aprobada (plan aprobado por el equipo; frontend en `localhost` por ahora)

## 1. Objetivo

Cerrar el backend: documentar la API para el frontend, hacer configurable el CORS y verificar la suite completa. No añade reglas de negocio.

## 2. Alcance

1. `docs/endpoints-api.md`: contrato de los 27 endpoints existentes.
2. CORS configurable por propiedad `cafeitsh.cors.origenes-permitidos` (lista separada por comas).
3. Revisión de seguridad de rutas y pruebas de CORS.
4. Verificación: `mvnw.cmd test` y `mvnw.cmd package`; bitácora y documentos de sprints sincronizados.

## 3. Reglas

- El valor por defecto de `cafeitsh.cors.origenes-permitidos` conserva los orígenes `localhost` actuales (`3000`, `5173`, `8080`, `8081`), porque el frontend aún no tiene dominio. Al desplegar, se sustituye por el dominio real mediante configuración, sin tocar el código.
- Métodos permitidos: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`. Headers: `Authorization`, `Content-Type`. Sin `PATCH` porque la API no lo usa.
- Nunca se usa `*` como origen permitido.
- Sin migraciones, sin dependencias nuevas, sin cambios en estados, transiciones ni endpoints.

## 4. Criterios de aceptación

- Un origen permitido recibe `Access-Control-Allow-Origin` en el preflight; uno no permitido recibe 403.
- La lista de orígenes se puede reemplazar por propiedad.
- `docs/endpoints-api.md` cubre todos los endpoints con roles, entrada, salida y errores.
- `mvnw.cmd test` y `mvnw.cmd package` en verde.

## 5. Fuera de alcance

Estadísticas o reportes adicionales, el proyecto Flutter y HTTPS (se configura al desplegar).
