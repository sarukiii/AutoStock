package models;

import java.util.Objects;

/**
 * Modelo que representa un cliente del sistema.
 *
 * Un cliente es la persona o empresa a la que se le realizan ventas.
 * Esta clase actúa como DTO (Data Transfer Object) entre la base de
 * datos y la interfaz gráfica: sus atributos se corresponden
 * directamente con las columnas de la tabla Cliente en MySQL.
 */
public class Cliente {

    // Identificador único generado automáticamente por la base de datos
    private Integer idCliente;
    private String nombre;
    private String telefono;
    private String direccion;
    private String email;
    // Empresa a la que pertenece el cliente (puede ser null si es particular)
    private String empresa;

    // Constructor vacío necesario para que ClienteController pueda
    // crear instancias al mapear los resultados de la base de datos
    public Cliente() {
    }

    /**
     * Constructor con todos los campos excepto el ID.
     * El ID lo asigna la base de datos automáticamente al insertar.
     *
     * @param nombre    nombre completo del cliente
     * @param telefono  teléfono de contacto
     * @param direccion dirección postal
     * @param email     correo electrónico
     * @param empresa   empresa del cliente (puede ser null)
     */
    public Cliente(String nombre, String telefono, String direccion, String email, String empresa) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.direccion = direccion;
        this.email = email;
        this.empresa = empresa;
    }

    // --- Getters y Setters ---
    // Siguen la convención JavaBeans: necesarios para que los controladores
    // puedan leer y escribir los atributos del modelo.

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    /**
     * Dos clientes son iguales si tienen el mismo ID.
     * Esto permite usar clientes en colecciones (List, Set) y
     * compararlos correctamente en los componentes Swing (JComboBox, JTable).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Cliente cliente = (Cliente) o;
        return Objects.equals(idCliente, cliente.idCliente);
    }

    /**
     * El hashCode se basa en el ID para mantener consistencia con equals.
     * Si dos objetos son iguales según equals, deben tener el mismo hashCode.
     */
    @Override
    public int hashCode() {
        return Objects.hash(idCliente);
    }

    /**
     * Representación textual del cliente.
     * Se usa en los JComboBox de la interfaz para mostrar el cliente
     * de forma legible: "[ID] Nombre".
     */
    @Override
    public String toString() {
        return "[" + idCliente + "] " + nombre;
    }
}