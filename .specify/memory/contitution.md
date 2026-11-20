# Constitución del proyecto cafeITSH

**Versión:** 1.0 (borrador)  
**Estado:** Pendiente de aprobación  
**Proyecto:** Sistema de pedidos anticipados — Cafetería ITSH  
**Fecha:** Noviembre de 2026

## I. Propósito y alcance del proyecto

cafeITSH es un sistema de pedidos anticipados para la cafetería del Instituto Tecnológico Superior de Huauchinango.

Su propósito es facilitar la consulta del menú, la realización de pedidos y la administración de las operaciones de la cafetería.

El desarrollo debe priorizar un producto mínimo viable (MVP) funcional, estable y verificable.

Las funcionalidades iniciales contempladas son:

- Autenticación y administración de usuarios.
- Control de acceso por roles.
- Consulta y gestión del menú.
- Carrito de compras.
- Realización y seguimiento de pedidos anticipados.
- Operaciones de caja.
- Gestión de pedidos en cocina.

Las funcionalidades de inteligencia artificial y notificaciones se consideran secundarias y no deben desplazar las prioridades del MVP.

Ninguna funcionalidad adicional debe incorporarse sin revisar su alcance y obtener aprobación.

## II. Desarrollo guiado por especificaciones

El proyecto utilizará una metodología basada en Spec-Driven Development.

Todo desarrollo deberá seguir este ciclo:

1. Constitución: consultar las reglas generales del proyecto.
2. Especificación: definir qué debe hacer la funcionalidad.
3. Planificación: establecer cómo se desarrollará técnicamente.
4. Tareas: dividir el trabajo en actividades concretas.
5. Implementación: desarrollar las tareas autorizadas.
6. Verificación: ejecutar pruebas y comprobar los criterios de aceptación.

### Reglas obligatorias para OpenCode

- Leer `AGENTS.md` antes de trabajar.
- Consultar esta constitución y las especificaciones relacionadas con la tarea.
- Analizar el código existente antes de proponer modificaciones.
- No implementar funcionalidades que todavía no tengan especificación.
- No comenzar la implementación sin presentar previamente un plan.
- Esperar autorización explícita antes de modificar código para una nueva funcionalidad.
- No marcar tareas como terminadas si no se han verificado.
- Actualizar la documentación cuando una decisión técnica o funcional cambie.

Si existe una contradicción entre los documentos, debe señalarla y esperar una decisión. No debe resolverla mediante suposiciones.

## III. Arquitectura y tecnologías

La arquitectura inicial del proyecto estará basada en:

| Componente | Tecnología |
|---|---|
| Backend | Java 17 |
| Framework backend | Spring Boot 4.1.2 |
| Gestor de dependencias | Maven |
| Frontend | Flutter |
| Lenguaje frontend | Dart |
| Base de datos | MySQL 8 |
| Contenedores | Docker y Docker Compose |

### Principios arquitectónicos

- Flutter será responsable de la interfaz de usuario.
- Spring Boot será responsable de la lógica de negocio y de proporcionar la API.
- MySQL será responsable de la persistencia de información.
- La comunicación entre frontend y backend se realizará mediante una API REST.
- La lógica de negocio debe permanecer en el backend.
- Las responsabilidades deben estar separadas y organizadas por módulos.
- No duplicar lógica de negocio entre frontend y backend.

No sustituir tecnologías ni modificar la arquitectura general sin autorización.

## IV. Seguridad y protección de información

La seguridad debe considerarse desde el diseño de cada funcionalidad.

Reglas obligatorias:

- Las contraseñas nunca deben almacenarse en texto plano.
- Utilizar mecanismos seguros de hash de contraseñas, como BCrypt.
- Las operaciones protegidas deben requerir autenticación.
- Los permisos deben verificarse en el backend.
- Los datos recibidos deben validarse en el servidor.
- Los secretos y credenciales deben almacenarse mediante variables de entorno.
- No subir archivos `.env` con credenciales a Git.
- No exponer información sensible en respuestas de la API.
- No devolver stack traces ni detalles internos al usuario.
- No almacenar información de tarjetas bancarias.

El mecanismo de autenticación y autorización deberá especificarse antes de implementarse.

## V. Reglas de negocio y consistencia

Las reglas de negocio deben estar documentadas antes de programarse.

Se deberá prestar especial atención a:

- Disponibilidad de productos.
- Precios y cantidades.
- Integridad de los pedidos.
- Estados y transiciones de pedidos.
- Operaciones de caja.
- Procesamiento de pedidos en cocina.
- Cancelaciones y modificaciones.
- Consistencia de la información entre los módulos.

No inventar reglas de negocio ni permitir transiciones de estados que no estén autorizadas por las especificaciones.

## VI. Base de datos y persistencia

La base de datos oficial del entorno local es MySQL 8.

Reglas:

- Revisar el esquema existente antes de crear tablas.
- Mantener relaciones y restricciones de integridad.
- Evitar información duplicada innecesariamente.
- No ejecutar operaciones destructivas sin autorización.
- No eliminar volúmenes de Docker.
- Documentar los cambios del esquema.
- Utilizar un mecanismo de migraciones versionadas para los cambios de estructura.

Flyway podrá utilizarse como herramienta de migraciones, pero su incorporación deberá planificarse y aprobarse antes de modificar la configuración actual.

No asumir que una herramienta o funcionalidad ya está instalada si no se ha comprobado.

## VII. Calidad del código

El código deberá cumplir los siguientes principios:

- Claridad y legibilidad.
- Modularidad.
- Responsabilidad única.
- Reutilización cuando sea conveniente.
- Bajo acoplamiento.
- Validaciones apropiadas.
- Manejo controlado de errores.
- Nombres descriptivos.
- Evitar código duplicado.
- Evitar complejidad innecesaria.

No agregar dependencias sin justificar su necesidad y explicar sus implicaciones.

## VIII. Pruebas y verificación

Toda funcionalidad deberá incluir pruebas acordes con su complejidad y alcance.

### Backend

- Pruebas unitarias.
- Pruebas de servicios.
- Pruebas de controladores.
- Pruebas de validaciones.
- Pruebas de persistencia cuando corresponda.
- Pruebas de autenticación y autorización.

### Frontend

- Pruebas de widgets.
- Pruebas de lógica.
- Pruebas de formularios.
- Pruebas de navegación.
- Pruebas de integración cuando corresponda.

Antes de considerar una tarea terminada, OpenCode deberá informar:

- Qué pruebas ejecutó.
- Qué resultados obtuvo.
- Qué errores encontró.
- Qué problemas quedaron pendientes.

Nunca afirmar que una prueba fue exitosa si no se ejecutó.

## IX. Documentación y comunicación

El idioma principal del proyecto será español.

Se deberá mantener en español:

- Documentación.
- Comentarios.
- Mensajes de validación.
- Mensajes de error destinados al usuario.
- Descripciones de funcionalidades.
- Especificaciones.
- Reportes de implementación.

Se conservarán los nombres técnicos propios de las tecnologías cuando sea necesario.

Los documentos Markdown serán la referencia para comprender los requisitos y las decisiones del proyecto.

El código y la documentación deben mantenerse sincronizados.

## X. Trabajo en equipo y control de cambios

El proyecto se desarrollará en equipo.

Por lo tanto:

- Evitar cambios innecesarios en archivos compartidos.
- No sobrescribir trabajo existente sin revisarlo.
- Mantener los cambios relacionados con la tarea solicitada.
- Informar los archivos creados, modificados o eliminados.
- No modificar configuraciones globales sin justificación.
- No introducir cambios que puedan afectar otros módulos sin advertirlo.

Las decisiones que afecten a varios integrantes deberán documentarse.

## XI. Restricciones para el agente

OpenCode no tiene autorización automática para:

- Cambiar la arquitectura.
- Eliminar información.
- Modificar credenciales.
- Incorporar funcionalidades fuera del MVP.
- Instalar dependencias sin justificación.
- Alterar reglas de negocio.
- Reemplazar implementaciones existentes sin analizarlas.
- Modificar múltiples módulos independientes sin autorización.

Cuando una tarea requiera alguno de estos cambios, deberá explicar el motivo y solicitar aprobación.

## XII. Gobernanza de la constitución

Esta constitución es el documento principal de principios y restricciones del proyecto.

Toda modificación deberá:

1. Identificar el principio que se desea modificar.
2. Explicar por qué es necesario.
3. Evaluar las consecuencias para el proyecto.
4. Obtener aprobación del equipo.
5. Actualizar la versión y fecha del documento.

Ningún agente puede modificar esta constitución unilateralmente.

---

**Fin de la constitución de cafeITSH.**