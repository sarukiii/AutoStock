package models;

import java.math.BigDecimal;
import java.util.Objects;

public class Producto {
    private Integer idProducto;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer cantidadDisponible;
    private Proveedor proveedor;

    // Constructores
    public Producto() {}

    public Producto(String nombre, String descripcion, BigDecimal precio,
                    Integer cantidadDisponible, Proveedor proveedor) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.cantidadDisponible = cantidadDisponible;
        this.proveedor = proveedor;
    }

    // Getters y Setters
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Producto producto = (Producto) o;
        return Objects.equals(idProducto, producto.idProducto);
    }
    
    @Override
    public String toString() {
        return "[" + idProducto + "] " + nombre;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProducto);
    }
}
