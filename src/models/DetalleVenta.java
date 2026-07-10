package models;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Modelo que representa una línea de detalle dentro de una venta.
 *
 * Cada DetalleVenta corresponde a un producto concreto incluido en una venta,
 * con su cantidad y precio unitario en el momento de la venta. Sigue el patrón
 * cabecera-líneas: Venta es la cabecera y DetalleVenta son las líneas.
 *
 * El precio unitario se guarda en el detalle (no se toma del producto)
 * para preservar el precio histórico de la venta, aunque el precio del
 * producto cambie posteriormente.
 *
 * Sus atributos se corresponden con las columnas de la tabla Detalle_Venta en
 * MySQL.
 */
public class DetalleVenta {

    // Identificador único generado automáticamente por la base de datos
    private Integer idDetalle;
    // Referencia a la venta a la que pertenece este detalle
    private Venta venta;
    // Producto vendido en esta línea
    private Producto producto;
    private Integer cantidad;
    // Precio unitario en el momento de la venta (puede diferir del precio actual
    // del producto)
    private BigDecimal precioUnitario;

    // Constructor vacío necesario para que VentaController pueda
    // crear instancias al mapear los resultados de la base de datos
    public DetalleVenta() {
    }

    /**
     * Constructor con todos los campos necesarios para crear un detalle de venta.
     *
     * @param venta          venta a la que pertenece este detalle
     * @param producto       producto vendido
     * @param cantidad       unidades vendidas
     * @param precioUnitario precio por unidad en el momento de la venta
     */
    public DetalleVenta(Venta venta, Producto producto, Integer cantidad, BigDecimal precioUnitario) {
        this.venta = venta;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    // --- Getters y Setters ---
    // Siguen la convención JavaBeans: necesarios para que los controladores
    // puedan leer y escribir los atributos del modelo.

    public Integer getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(Integer idDetalle) {
        this.idDetalle = idDetalle;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
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

    /**
     * Calcula el subtotal de esta línea de detalle.
     *
     * El subtotal es el resultado de multiplicar el precio unitario por
     * la cantidad vendida. Se calcula en tiempo real en lugar de almacenarse
     * en la base de datos, ya que siempre puede derivarse de los otros campos.
     *
     * @return subtotal de esta línea (precioUnitario × cantidad)
     */
    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(new BigDecimal(cantidad));
    }

    /**
     * Dos detalles son iguales si tienen el mismo ID.
     * Necesario para comparar detalles correctamente en colecciones.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DetalleVenta detalle = (DetalleVenta) o;
        return Objects.equals(idDetalle, detalle.idDetalle);
    }

    /**
     * El hashCode se basa en el ID para mantener consistencia con equals.
     * Si dos objetos son iguales según equals, deben tener el mismo hashCode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(idDetalle);
    }
}