-- Sprint 1: menú — categorías y productos
-- Reglas: borrado lógico (nunca físico), precio >= 0,
-- producto con categoría obligatoria. Ver specs/menu/spec.md.

CREATE TABLE categorias (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(100) NOT NULL,
    activa      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME  NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_categorias_nombre (nombre)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE productos (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    categoria_id BIGINT       NOT NULL,
    nombre      VARCHAR(100)  NOT NULL,
    descripcion VARCHAR(500)  NULL,
    precio      DECIMAL(10,2) NOT NULL,
    disponible  BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME   NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_productos_categoria
        FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT ck_productos_precio CHECK (precio >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
