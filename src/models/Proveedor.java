package models;

import java.util.Objects;

public class Proveedor {
    private Integer idProveedor;
    private String nombre;
    private String contacto;
    private String telefono;
    private String direccion;
    private String email;

    // Constructores
    public Proveedor() {}

    public Proveedor(String nombre, String contacto, String telefono, String direccion, String email) {
        this.nombre = nombre;
        this.contacto = contacto;
        this.telefono = telefono;
        this.direccion = direccion;
        this.email = email;
    }

    // Getters y Setters
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Proveedor proveedor = (Proveedor) o;
        return Objects.equals(idProveedor, proveedor.idProveedor);
    }
    
    @Override
    public String toString() {
        return "[" + idProveedor + "] " + nombre;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProveedor);
    }
}
