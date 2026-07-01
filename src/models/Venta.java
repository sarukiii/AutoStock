package models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Venta {
    private Integer idVenta;
    private Cliente cliente;
    private Usuario usuario;
    private LocalDateTime fecha;
    private BigDecimal total;
    
    private List<DetalleVenta> detalles;

    // Constructores
    public Venta() {
    }

    public Venta(Cliente cliente, Usuario usuario, LocalDateTime fecha, BigDecimal total) {
        this.cliente = cliente;
        this.usuario = usuario;
        this.fecha = fecha;
        this.total = total;
    }

    // Getters y Setters
    public Integer getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(Integer idVenta) {
        this.idVenta = idVenta;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Venta venta = (Venta) o;
        return Objects.equals(idVenta, venta.idVenta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idVenta);
    }

    public void setDetalles(List<DetalleVenta> detallesByVentaId) {
        this.detalles = detallesByVentaId;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }
}
