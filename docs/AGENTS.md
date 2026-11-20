# AGENTS.md

# Proyecto: cafeITSH

Sistema de pedidos anticipados para la cafetería del Instituto Tecnológico Superior de Huauchinango (ITSH).

Proyecto académico desarrollado en equipo.

El objetivo es construir una aplicación que permita consultar el menú, realizar pedidos anticipados y administrar las operaciones de la cafetería mediante diferentes roles de usuario.

---

## 1. Reglas generales para el agente

- Antes de modificar código, revisar la estructura y los archivos existentes del proyecto.
- Leer los documentos de `docs/` y las especificaciones de `specs/` relacionados con la tarea.
- No asumir que una funcionalidad existe si no está implementada.
- No reemplazar código existente sin analizar primero su propósito.
- No modificar archivos ajenos a la tarea solicitada.
- No eliminar configuraciones, dependencias, tablas o datos sin autorización.
- Explicar brevemente qué se va a modificar antes de realizar cambios importantes.
- Mantener el código organizado, modular y fácil de mantener.
- No implementar funcionalidades fuera del alcance aprobado.
- No inventar requisitos ni tomar decisiones importantes de negocio sin documentarlas.

**Idioma obligatorio:** todo el código, nombres de variables, clases, métodos, comentarios, mensajes de usuario y documentación deben mantenerse en español, salvo nombres técnicos propios de las tecnologías.

## 2. Estructura del proyecto

La estructura principal es:

```text
cafeITSH/
│
├── AGENTS.md
├── docs/
├── specs/
│
├── database/
│   ├── .env
│   └── docker-compose.yml
│
├── backend/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   └── src/
│
└── frontend/
    └── (aún no creado — se espera un proyecto Flutter)
```

### Responsabilidades

- `backend/`: lógica del servidor, API REST, autenticación y conexión con MySQL.
- `frontend/`: aplicación móvil y/o interfaz desarrollada con Flutter (directorio vacío por ahora).
- `database/`: configuración de Docker y servicio MySQL.
- `docs/`: documentación general, arquitectura, reglas y decisiones técnicas.
- `specs/`: especificaciones funcionales y técnicas de cada módulo.

Si la estructura real difiere de este documento, inspeccionar primero el repositorio y actualizar la documentación cuando corresponda. No crear estructuras duplicadas.

## 3. Tecnologías oficiales

| Componente | Tecnología |
|---|---|
| Backend | Java 17 |
| Framework backend | Spring Boot 4.1.2 |
| Gestor de dependencias | Maven |
| Frontend | Flutter |
| Lenguaje frontend | Dart |
| Base de datos | MySQL 8 |
| Contenedores | Docker y Docker Compose |
| Control de versiones | Git |

No sustituir estas tecnologías sin autorización.

### Reglas de dependencias

- Preferir las dependencias que ya existen.
- No instalar librerías nuevas sin justificar su necesidad.
- Antes de agregar una dependencia, comprobar si existe una alternativa con las herramientas actuales.
- Utilizar Maven Wrapper (`mvnw.cmd`) para ejecutar el backend.
- Mantener las versiones de dependencias compatibles con Java 17 y Spring Boot.

## 4. Backend — Spring Boot

### Configuración actual

- Puerto HTTP: `8081`.
- Base de datos: `cafeitsh_db`.
- Motor: MySQL 8.
- Host MySQL desde Windows: `localhost`.
- Puerto MySQL publicado: `3307`.
- Usuario de base de datos: `cafe_admin`.

La conexión se configura en:

`backend/src/main/resources/application.yaml`

La contraseña debe obtenerse mediante la variable de entorno `DB_PASSWORD`.

Nunca escribir contraseñas directamente en el código o en archivos versionados.

### Arquitectura

Mantener una separación clara de responsabilidades:

```text
backend/src/main/java/com/cafeitsh/backend/
│
├── config/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── security/
└── BackendApplication.java
```

Esta estructura es una organización objetivo. Antes de crear paquetes, revisar los existentes y evitar duplicaciones.

### Responsabilidades por capa

- `controller/`: recibir solicitudes HTTP y devolver respuestas.
- `service/`: implementar reglas y lógica de negocio.
- `repository/`: operaciones de acceso a datos.
- `entity/`: representar las entidades de persistencia.
- `dto/`: objetos de entrada y salida de la API.
- `config/`: configuración general del backend.
- `security/`: autenticación, autorización y protección de rutas.

### Reglas de implementación

- Utilizar controladores REST.
- Separar la lógica de negocio de los controladores.
- Validar los datos recibidos por la API.
- Manejar excepciones de manera centralizada.
- No devolver entidades de base de datos directamente cuando sea conveniente utilizar DTO.
- No exponer contraseñas, tokens ni información sensible.
- Mantener respuestas HTTP y códigos de estado apropiados.
- Documentar endpoints y sus parámetros.

### Comandos

Ejecutar desde `backend/`:

| Acción | Comando |
|---|---|
| Iniciar backend | `mvnw.cmd spring-boot:run` |
| Compilar | `mvnw.cmd compile` |
| Ejecutar pruebas | `mvnw.cmd test` |
| Empaquetar | `mvnw.cmd package` |

En Windows, utilizar `mvnw.cmd`.

## 5. Base de datos — MySQL

### Configuración actual

MySQL se ejecuta mediante Docker Compose.

- Contenedor: `cafeitsh-mysql`.
- Imagen: `mysql:8.0`.
- Puerto del host: `3307`.
- Puerto interno del contenedor: `3306`.
- Base de datos: `cafeitsh_db`.
- Usuario de aplicación: `cafe_admin`.

### Reglas

- No utilizar el usuario root desde la aplicación.
- Utilizar el usuario `cafe_admin`.
- No guardar contraseñas en archivos versionados.
- No eliminar volúmenes de Docker.
- No ejecutar operaciones destructivas sobre la base de datos sin autorización.
- Revisar la estructura existente antes de crear o modificar tablas.
- Mantener consistencia en nombres de tablas, columnas y relaciones.
- Utilizar claves primarias y foráneas cuando corresponda.
- Evitar duplicidad de información.

### Migraciones

Flyway está integrado desde el Sprint 0 (aprobado por el equipo).

- Las migraciones viven en `backend/src/main/resources/db/migraciones/`.
- Cada cambio de esquema debe tener una migración nueva (`V2`, `V3`, ...).
- No modificar migraciones que ya hayan sido aplicadas.
- No crear tablas manualmente sin documentar el cambio.
- Mantener las migraciones versionadas en Git y registrar el cambio en `docs/bitacora-desarrollo.md`.

## 6. Frontend — Flutter

### Reglas generales

- Utilizar Dart y Flutter.
- Mantener una arquitectura modular.
- Separar pantallas, componentes, modelos, servicios y configuración.
- Evitar concentrar toda la lógica en un solo archivo.
- Reutilizar componentes cuando sea conveniente.
- Mantener una interfaz consistente.
- Diseñar pensando en dispositivos móviles.
- Mostrar mensajes claros cuando ocurra un error.
- Validar los formularios antes de enviar información al backend.

### Comunicación con el backend

La aplicación debe consumir la API REST desarrollada con Spring Boot.

En desarrollo local:

- Backend: `http://localhost:8081`.
- Android Emulator: normalmente utilizar `http://10.0.2.2:8081`.
- Dispositivo físico: utilizar la dirección IP local del equipo servidor, siempre que la red y el servidor lo permitan.

No asumir que `localhost` representa el mismo equipo desde todos los dispositivos.

Centralizar la configuración de la URL de la API para facilitar cambios entre desarrollo y producción.

### Comandos

Ejecutar desde `frontend/`:

| Acción | Comando |
|---|---|
| Descargar dependencias | `flutter pub get` |
| Ejecutar aplicación | `flutter run` |
| Analizar código | `flutter analyze` |
| Ejecutar pruebas | `flutter test` |
| Formatear código | `dart format .` |

## 7. Seguridad

La seguridad es obligatoria en todos los módulos.

- Nunca almacenar contraseñas en texto plano.
- Utilizar algoritmos seguros de hash de contraseñas, como BCrypt.
- Proteger los endpoints que requieran autenticación.
- Implementar autorización de acuerdo con los roles.
- No confiar únicamente en las validaciones del frontend.
- Validar también en el backend.
- No exponer credenciales ni secretos en Git.
- Utilizar variables de entorno para configuraciones sensibles.
- No incluir información de tarjetas bancarias.
- No exponer errores internos, consultas SQL ni stack traces al usuario.
- Configurar HTTPS para producción.

## 8. Roles y funcionalidades

La aplicación debe contemplar los roles que se definan en los requerimientos oficiales.

Como mínimo, la arquitectura debe permitir separar las operaciones de:

- Administración.
- Caja.
- Cocina.
- Cliente.

No asumir permisos específicos de cada rol sin consultar la documentación funcional.

La autorización debe verificarse en el backend, no solamente ocultando botones o pantallas en Flutter.

## 9. Pedidos anticipados

Las reglas de negocio de pedidos deben respetarse en frontend y backend.

Regla vigente (ajustada y aprobada, detallada en `specs/pedidos/spec.md`):

- Los pedidos son anticipados del **mismo día**, con zona horaria `America/Mexico_City`, y solo pueden realizarse dentro del **horario de servicio**.
- La hora de entrega debe cumplir: `entrega >= ahora + tiempoPreparacionMinutos` (lead time), `entrega <= horaCierre - margenCierreMinutos` (margen antes del cierre) y estar dentro del horario de servicio. Si `ahora + tiempoPreparacionMinutos` es anterior a la apertura del mismo día, el mínimo efectivo es la apertura.
- La hora de entrega **no** exige alineación a una cuadrícula/franja exacta (p. ej. múltiplos de 10 minutos); se acepta cualquier hora válida dentro del rango.
- Parámetros configurables en `application.yaml` (no secretos): `cafeitsh.horario-apertura` (inicial `08:00`), `cafeitsh.horario-cierre` (`20:00`), `cafeitsh.tiempo-preparacion-minutos` (`15`), `cafeitsh.margen-cierre-minutos` (`15`).
- El pedido se crea en **PENDIENTE** y es inmutable tras su creación (Sprint 2: sin endpoints de edición ni de transición de estado).

Estados contemplados para los pedidos:

1. Pendiente.
2. Pagado.
3. En preparación.
4. Listo.
5. Entregado.
6. Cancelado.

No modificar los estados ni sus transiciones sin revisar los requerimientos y aprobar el cambio.

Las operaciones de pago, cancelación y entrega deben respetar las reglas de negocio que se documenten en sus respectivas especificaciones.

## 10. Documentación y especificaciones Markdown

Los archivos Markdown son la referencia para el desarrollo.

Antes de implementar una funcionalidad:

1. Revisar la especificación correspondiente.
2. Identificar los requisitos.
3. Revisar los criterios de aceptación.
4. Analizar los archivos existentes.
5. Proponer un plan de implementación.
6. Implementar únicamente lo aprobado.
7. Ejecutar las pruebas.
8. Actualizar la documentación y el estado de la tarea.

Cada módulo debe tener una especificación que describa:

- Objetivo.
- Descripción funcional.
- Requisitos.
- Reglas de negocio.
- Datos de entrada y salida.
- Validaciones.
- Criterios de aceptación.
- Consideraciones de seguridad.
- Pruebas necesarias.

No considerar terminada una funcionalidad únicamente porque compila.

## 11. Pruebas y calidad

Toda funcionalidad nueva debe incluir pruebas apropiadas.

### Backend

- Pruebas unitarias de servicios.
- Pruebas de controladores.
- Pruebas de validaciones.
- Pruebas de persistencia cuando corresponda.
- Pruebas de seguridad y autorización.

### Frontend

- Pruebas de widgets.
- Pruebas de lógica.
- Pruebas de validaciones.
- Pruebas de navegación.
- Pruebas de interacción cuando corresponda.

### Verificación obligatoria

Antes de considerar terminada una tarea:

Backend:

```cmd
cd backend
mvnw.cmd test
mvnw.cmd package
```

Frontend:

```cmd
cd frontend
flutter analyze
flutter test
```

Si alguna verificación falla:

- Identificar el error.
- Explicar su causa.
- Corregirlo si está dentro del alcance autorizado.
- Volver a ejecutar las pruebas.

No afirmar que las pruebas fueron exitosas si no se ejecutaron.

## 12. Flujo de trabajo con OpenCode

El desarrollo se realizará por módulos y tareas pequeñas.

Ciclo de trabajo:

1. Analizar la solicitud.
2. Leer los documentos relacionados.
3. Revisar el código existente.
4. Proponer los archivos que se modificarán.
5. Presentar el plan.
6. Esperar aprobación para cambios importantes.
7. Implementar.
8. Ejecutar pruebas.
9. Informar los archivos modificados.
10. Actualizar la documentación.

No implementar múltiples módulos independientes en una sola tarea sin autorización.

### Al terminar cada tarea, informar:

- Qué se implementó.
- Qué archivos se crearon.
- Qué archivos se modificaron.
- Qué pruebas se ejecutaron.
- Resultado de las pruebas.
- Qué queda pendiente.

## 13. Reglas no negociables

1. No cambiar la arquitectura tecnológica sin autorización.
2. No eliminar datos ni volúmenes de Docker.
3. No almacenar secretos en Git.
4. No introducir dependencias innecesarias.
5. No modificar funcionalidades fuera del alcance aprobado.
6. No duplicar lógica de negocio.
7. No omitir validaciones del backend.
8. No confiar únicamente en la seguridad del frontend.
9. No declarar terminada una tarea sin verificarla.
10. Mantener sincronizados el código y la documentación.

---

**Prioridad de consulta para el agente:**

1. `AGENTS.md` (raíz del repositorio; resumen operativo y estado actual del proyecto)
2. `docs/AGENTS.md` (este documento, reglas detalladas), `docs/reglas.md` y documentación general del proyecto, cuando existan.
3. Especificación de la funcionalidad dentro de `specs/`.
4. Código fuente existente.
5. Decisiones técnicas previamente documentadas.

Si existen contradicciones, detenerse, informar la diferencia y solicitar una decisión antes de realizar cambios importantes.