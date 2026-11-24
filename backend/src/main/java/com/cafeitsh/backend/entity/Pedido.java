package com.cafeitsh.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido anticipado de un cliente. Se crea en estado PENDIENTE y es
 * inmutable tras su creaci?n (sin edici?n de ?tems, hora ni total).
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPedido estado;

    @Column(name = "fecha_hora_entrega", nullable = false)
    private LocalDateTime fechaHoraEntrega;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago_preferido", length = 30)
    private MetodoPagoPreferido metodoPagoPreferido;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @Column(name = "cancelado_en")
    private LocalDateTime canceladoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelado_por_usuario_id")
    private Usuario canceladoPor;

    @Column(name = "motivo_cancelacion", length = 255)
    private String motivoCancelacion;

    @Column(name = "requiere_reembolso", nullable = false)
    private boolean requiereReembolso = false;

    @Column(name = "reembolso_manual_registrado", nullable = false)
    private boolean reembolsoManualRegistrado = false;

    @Column(name = "observacion_reembolso", length = 255)
    private String observacionReembolso;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", fetch = FetchType.LAZY)
    private List<Pago> pagos = new ArrayList<>();

    @PrePersist
    void alCrear() {
        this.creadoEn = LocalDateTime.now();
    }

    @PreUpdate
    void alActualizar() {
        this.actualizadoEn = LocalDateTime.now();
    }

    public void agregarItem(PedidoItem item) {
        items.add(item);
        item.setPedido(this);
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaHoraEntrega() {
        return fechaHoraEntrega;
    }

    public void setFechaHoraEntrega(LocalDateTime fechaHoraEntrega) {
        this.fechaHoraEntrega = fechaHoraEntrega;
    }

    public MetodoPagoPreferido getMetodoPagoPreferido() {
        return metodoPagoPreferido;
    }

    public void setMetodoPagoPreferido(MetodoPagoPreferido metodoPagoPreferido) {
        this.metodoPagoPreferido = metodoPagoPreferido;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public List<PedidoItem> getItems() {
        return items;
    }

    public LocalDateTime getCanceladoEn() {
        return canceladoEn;
    }

    public void setCanceladoEn(LocalDateTime canceladoEn) {
        this.canceladoEn = canceladoEn;
    }

    public Usuario getCanceladoPor() {
        return canceladoPor;
    }

    public void setCanceladoPor(Usuario canceladoPor) {
        this.canceladoPor = canceladoPor;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    public boolean isRequiereReembolso() {
        return requiereReembolso;
    }

    public void setRequiereReembolso(boolean requiereReembolso) {
        this.requiereReembolso = requiereReembolso;
    }

    public boolean isReembolsoManualRegistrado() {
        return reembolsoManualRegistrado;
    }

    public void setReembolsoManualRegistrado(boolean reembolsoManualRegistrado) {
        this.reembolsoManualRegistrado = reembolsoManualRegistrado;
    }

    public String getObservacionReembolso() {
        return observacionReembolso;
    }

    public void setObservacionReembolso(String observacionReembolso) {
        this.observacionReembolso = observacionReembolso;
    }

    public List<Pago> getPagos() {
        return pagos;
    }
}
