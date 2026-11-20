-- Sprint 0: esquema inicial — autenticación y usuarios
-- Cada cambio de esquema futuro debe ser una migración nueva (V2, V3, ...).

CREATE TABLE usuarios (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(100) NOT NULL,
    correo      VARCHAR(255) NOT NULL,
    contrasena  VARCHAR(100) NOT NULL,
    rol         VARCHAR(20)  NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME  NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_usuarios_correo (correo),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ADMIN', 'CAJA', 'COCINA', 'CLIENTE'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
