package models;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Modelo que representa un producto del inventario.
 *
 * Un producto es el artículo que se vende y del que se lleva el control
 * de stock. Cada producto tiene un proveedor asociado. Sus atributos se
 * corresponden con las columnas de la tabla Producto en MySQL.
 *
 * Usa BigDecimal para el precio en lugar de double o float para evitar
 * errores de precisión en operaciones monetarias (problema conocido de
 * los tipos de punto flotante en Java).
 */
public class Producto {

    // Identificador único generado automáticamente por la base de datos
    private Integer idProducto;
    private String nombre;
    private String descripcion;
    // BigDecimal garantiza precisión exacta en cálculos monetarios
    private BigDecimal precio;
    private Integer cantidadDisponible;
    // Relación con el proveedor: cada producto tiene exactamente un proveedor
    private Proveedor proveedor;

    // Constructor vacío necesario para que ProductoController pueda
    // crear instancias al mapear los resultados de la base de datos
    public Producto() {
    }

    /**
     * Constructor con todos los campos excepto el ID.
     * El ID lo asigna la base de datos automáticamente al insertar.
     *
     * @param nombre             nombre del producto
     * @param descripcion        descripción detallada
     * @param precio             precio de venta (usa BigDecimal para precisión
     *                           monetaria)
     * @param cantidadDisponible stock actual disponible
     * @param proveedor          proveedor que suministra el producto
     */
    public Producto(String nombre, String descripcion, BigDecimal precio,
            Integer cantidadDisponible, Proveedor proveedor) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.cantidadDisponible = cantidadDisponible;
        this.proveedor = proveedor;
    }

    // --- Getters y Setters ---
    // Siguen la convención JavaBeans: necesarios para que los controladores
    // puedan leer y escribir los atributos del modelo.

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Integer getCantidadDisponible() {
        return cantidadDisponible;
    }

    public void setCantidadDisponible(Integer cantidadDisponible) {
        this.cantidadDisponible = cantidadDisponible;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    /**
     * Dos productos son iguales si tienen el mismo ID.
     * Necesario para comparar productos correctamente en
     * colecciones y componentes Swing (JComboBox, JTable).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Producto producto = (Producto) o;
        return Objects.equals(idProducto, producto.idProducto);
    }

    /**
     * El hashCode se basa en el ID para mantener consistencia con equals.
     * Si dos objetos son iguales según equals, deben tener el mismo hashCode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(idProducto);
    }

    /**
     * Representación textual del producto.
     * Se usa en los JComboBox de la interfaz para mostrar el producto
     * de forma legible: "[ID] Nombre".
     */
    @Override
    public String toString() {
        return "[" + idProducto + "] " + nombre;
    }
}