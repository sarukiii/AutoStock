package models;

import java.math.BigDecimal;
import java.util.Objects;

public class DetalleVenta {
    private Integer idDetalle;
    private Venta venta;
    private Producto producto;
    private Integer cantidad;
    private BigDecimal precioUnitario;

    // Constructores
    public DetalleVenta() {}

    public DetalleVenta(Venta venta, Producto producto, Integer cantidad, BigDecimal precioUnitario) {
        this.venta = venta;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    // Getters y Setters
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
    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(new BigDecimal(cantidad));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetalleVenta detalle = (DetalleVenta) o;
        return Objects.equals(idDetalle, detalle.idDetalle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDetalle);
    }
}
