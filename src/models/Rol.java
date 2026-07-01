package models;

import java.util.Objects;

public class Rol {
    private Integer idRol;
    private String nombre;
    private String descripcion;

    // Constructores
    public Rol() {}

    public Rol(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    // Getters y Setters
 
    public Integer getIdRol() { 
    	return idRol; 
    }
    
    public void setIdRol(Integer idRol) { 
    	this.idRol = idRol; 
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rol rol = (Rol) o;
        return Objects.equals(idRol, rol.idRol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idRol);
    }
}
