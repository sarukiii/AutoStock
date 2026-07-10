package controllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) para el historial de ventas de un producto.
 *
 * Un DTO es un objeto cuya única función es transportar datos entre capas
 * de la aplicación. En este caso, agrupa en un solo objeto toda la
 * información necesaria para mostrar una fila en el panel de historial
 * de ventas: datos de la venta, evolución del stock y datos del cliente.
 *
 * A diferencia de los modelos (Cliente, Producto, etc.), este DTO no
 * se corresponde con una tabla concreta de la base de datos, sino que
 * combina datos de varias tablas (Venta, Detalle_Venta, Cliente, Producto)
 * obtenidos mediante la consulta en
 * VentaController.findVentaHistoryByProductId().
 */
public class HistorialVentasDTO {

    // Fecha y hora en que se realizó la venta
    private LocalDateTime fecha;
    // ID de la venta a la que pertenece este registro
    private Integer idVenta;
    // Stock del producto antes de esta venta (valor histórico calculado)
    private Integer anteriorValor;
    // Stock del producto después de esta venta (valor histórico calculado)
    private Integer nuevoValor;
    // Unidades vendidas en esta línea de detalle
    private Integer cantidad;
    // Precio al que se vendió la unidad en el momento de la venta
    private BigDecimal precioUnitario;
    // Precio actual del producto (puede diferir del precio de venta histórico)
    private BigDecimal precioProducto;
    // Nombre del cliente que realizó la compra
    private String clienteNombre;

    // --- Getters y Setters ---
    // Siguen la convención JavaBeans: necesarios para que VentaController
    // pueda rellenar este DTO y el panel de historial pueda leerlo.

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Integer getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(Integer idVenta) {
        this.idVenta = idVenta;
    }

    public Integer getAnteriorValor() {
        return anteriorValor;
    }

    public void setAnteriorValor(Integer anteriorValor) {
        this.anteriorValor = anteriorValor;
    }

    public Integer getNuevoValor() {
        return nuevoValor;
    }

    public void setNuevoValor(Integer nuevoValor) {
        this.nuevoValor = nuevoValor;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getPrecioProducto() {
        return precioProducto;
    }

    public void setPrecioProducto(BigDecimal precioProducto) {
        this.precioProducto = precioProducto;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }
}