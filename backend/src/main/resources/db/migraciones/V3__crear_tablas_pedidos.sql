-- Sprint 2: pedidos anticipados
-- Reglas: pedido en estado PENDIENTE al crearse, precio unitario vigente
-- capturado por ?tem, sin borrado f?sico. Ver specs/pedidos/spec.md.

CREATE TABLE pedidos (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    usuario_id             BIGINT        NOT NULL,
    estado                 VARCHAR(30)   NOT NULL,
    fecha_hora_entrega     DATETIME      NOT NULL,
    metodo_pago_preferido  VARCHAR(30)   NULL,
    total                  DECIMAL(10,2) NOT NULL,
    creado_en              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en         DATETIME      NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_pedidos_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_pedidos_total CHECK (total >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE pedido_items (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    pedido_id       BIGINT        NOT NULL,
    producto_id     BIGINT        NOT NULL,
    cantidad        INT           NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pedido_items_pedido
        FOREIGN KEY (pedido_id) REFERENCES pedidos (id),
    CONSTRAINT fk_pedido_items_producto
        FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT ck_pedido_items_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_pedido_items_precio_unitario CHECK (precio_unitario >= 0),
    CONSTRAINT ck_pedido_items_subtotal CHECK (subtotal >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_pedidos_usuario ON pedidos (usuario_id);
CREATE INDEX idx_pedidos_estado ON pedidos (estado);
CREATE INDEX idx_pedido_items_pedido ON pedido_items (pedido_id);
