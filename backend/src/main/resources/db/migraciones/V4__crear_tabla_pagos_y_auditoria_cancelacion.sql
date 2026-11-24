-- V4: tabla de pagos y auditoría de cancelación/reembolso manual en pedidos
-- Sprint 3 - Caja. No modifica V1-V3.

CREATE TABLE pagos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    metodo_pago VARCHAR(30) NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    fecha_pago DATETIME NOT NULL,
    registrado_por_usuario_id BIGINT NOT NULL,
    referencia VARCHAR(100) NULL,
    CONSTRAINT fk_pagos_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    CONSTRAINT fk_pagos_registrado_por FOREIGN KEY (registrado_por_usuario_id) REFERENCES usuarios(id),
    CONSTRAINT chk_pagos_monto_positivo CHECK (monto > 0),
    INDEX idx_pagos_pedido (pedido_id),
    INDEX idx_pagos_fecha_pago (fecha_pago)
);

ALTER TABLE pedidos
    ADD COLUMN cancelado_en DATETIME NULL,
    ADD COLUMN cancelado_por_usuario_id BIGINT NULL,
    ADD COLUMN motivo_cancelacion VARCHAR(255) NULL,
    ADD COLUMN requiere_reembolso BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN reembolso_manual_registrado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN observacion_reembolso VARCHAR(255) NULL,
    ADD CONSTRAINT fk_pedidos_cancelado_por FOREIGN KEY (cancelado_por_usuario_id) REFERENCES usuarios(id);
