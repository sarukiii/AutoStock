package models;

import java.util.Objects;

public class Cliente {
    private Integer idCliente;
    private String nombre;
    private String telefono;
    private String direccion;
    private String email;
    private String empresa;

    // Constructores
    public Cliente() {}

    public Cliente(String nombre, String telefono, String direccion, String email, String empresa) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.direccion = direccion;
        this.email = email;
        this.empresa = empresa;
    }

    // Getters y Setters
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cliente cliente = (Cliente) o;
        return Objects.equals(idCliente, cliente.idCliente);
    }

    @Override
    public String toString() {
        return "[" + idCliente + "] " + nombre;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCliente);
    }
}
