package models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Modelo que representa una venta realizada en el sistema.
 *
 * Una venta es la transacción principal que registra qué cliente compró,
 * qué usuario procesó la venta, cuándo se realizó y el importe total.
 * Los productos concretos vendidos se almacenan en la lista de detalles
 * (DetalleVenta), siguiendo el patrón cabecera-líneas típico en sistemas
 * de gestión comercial.
 *
 * Sus atributos se corresponden con las columnas de la tabla Venta en MySQL.
 * Los detalles se cargan por separado desde la tabla Detalle_Venta.
 */
public class Venta {

    // Identificador único generado automáticamente por la base de datos
    private Integer idVenta;
    // Cliente al que se le realiza la venta
    private Cliente cliente;
    // Usuario del sistema que procesa la venta
    private Usuario usuario;
    // Fecha y hora exacta en que se registra la venta
    private LocalDateTime fecha;
    // Importe total de la venta. BigDecimal garantiza precisión monetaria exacta
    private BigDecimal total;
    // Lista de líneas de detalle: cada una representa un producto vendido
    // con su cantidad y precio unitario
    private List<DetalleVenta> detalles;

    // Constructor vacío necesario para que VentaController pueda
    // crear instancias al mapear los resultados de la base de datos
    public Venta() {
    }

    /**
     * Constructor con los campos principales de la venta.
     * El ID lo asigna la base de datos y los detalles se añaden por separado.
     *
     * @param cliente cliente al que se realiza la venta
     * @param usuario usuario que procesa la venta
     * @param fecha   fecha y hora de la venta
     * @param total   importe total de la venta
     */
    public Venta(Cliente cliente, Usuario usuario, LocalDateTime fecha, BigDecimal total) {
        this.cliente = cliente;
        this.usuario = usuario;
        this.fecha = fecha;
        this.total = total;
    }

    // --- Getters y Setters ---
    // Siguen la convención JavaBeans: necesarios para que los controladores
    // puedan leer y escribir los atributos del modelo.

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

    /**
     * Establece la lista de detalles (líneas de producto) de esta venta.
     * Los detalles se cargan desde la tabla Detalle_Venta después de
     * obtener la cabecera de la venta.
     *
     * @param detalles lista de detalles de la venta
     */
    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }

    /**
     * Devuelve la lista de detalles (líneas de producto) de esta venta.
     *
     * @return lista de detalles, o null si aún no se han cargado
     */
    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    /**
     * Dos ventas son iguales si tienen el mismo ID.
     * Necesario para comparar ventas correctamente en colecciones.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Venta venta = (Venta) o;
        return Objects.equals(idVenta, venta.idVenta);
    }

    /**
     * El hashCode se basa en el ID para mantener consistencia con equals.
     * Si dos objetos son iguales según equals, deben tener el mismo hashCode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(idVenta);
    }
}