package models;

import java.util.Objects;

/**
 * Modelo que representa un proveedor del sistema.
 *
 * Un proveedor es la empresa o persona que suministra los productos
 * del inventario. Cada producto tiene exactamente un proveedor asociado.
 * Sus atributos se corresponden con las columnas de la tabla Proveedor en
 * MySQL.
 */
public class Proveedor {

    // Identificador único generado automáticamente por la base de datos
    private Integer idProveedor;
    private String nombre;
    // Persona de contacto dentro de la empresa proveedora
    private String contacto;
    private String telefono;
    private String direccion;
    private String email;

    // Constructor vacío necesario para que ProveedorController pueda
    // crear instancias al mapear los resultados de la base de datos
    public Proveedor() {
    }

    /**
     * Constructor con todos los campos excepto el ID.
     * El ID lo asigna la base de datos automáticamente al insertar.
     *
     * @param nombre    nombre de la empresa proveedora
     * @param contacto  persona de contacto en la empresa
     * @param telefono  teléfono de contacto
     * @param direccion dirección postal
     * @param email     correo electrónico
     */
    public Proveedor(String nombre, String contacto, String telefono, String direccion, String email) {
        this.nombre = nombre;
        this.contacto = contacto;
        this.telefono = telefono;
        this.direccion = direccion;
        this.email = email;
    }

    // --- Getters y Setters ---
    // Siguen la convención JavaBeans: necesarios para que los controladores
    // puedan leer y escribir los atributos del modelo.

    public Integer getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(Integer idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Dos proveedores son iguales si tienen el mismo ID.
     * Necesario para comparar proveedores correctamente en
     * colecciones y componentes Swing (JComboBox, JTable).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Proveedor proveedor = (Proveedor) o;
        return Objects.equals(idProveedor, proveedor.idProveedor);
    }

    /**
     * El hashCode se basa en el ID para mantener consistencia con equals.
     * Si dos objetos son iguales según equals, deben tener el mismo hashCode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(idProveedor);
    }

    /**
     * Representación textual del proveedor.
     * Se usa en los JComboBox de la interfaz para mostrar el proveedor
     * de forma legible: "[ID] Nombre".
     */
    @Override
    public String toString() {
        return "[" + idProveedor + "] " + nombre;
    }
}