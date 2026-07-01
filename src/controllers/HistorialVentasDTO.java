package controllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HistorialVentasDTO {
    private LocalDateTime fecha;
    private Integer idVenta;
    private Integer anteriorValor;
    private Integer nuevoValor;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal precioProducto;
    private String clienteNombre;

    // Getters y setters
    public LocalDateTime getFecha() { 
    	return fecha; 
    }
    
    public void setFecha(LocalDateTime fecha) { 
    	this.fecha = fecha; 
    }
    

    public Integer getIdVenta() { 
    	return idVenta; 
    }
    
    public void setIdVenta(Integer idVenta) { 
    	this.idVenta = idVenta; 
    }
    

    public Integer getAnteriorValor() { 
    	return anteriorValor; 
    }
    
    public void setAnteriorValor(Integer anteriorValor) { 
    	this.anteriorValor = anteriorValor; 
    }
    

    public Integer getNuevoValor() { 
    	return nuevoValor; 
    }
    
    public void setNuevoValor(Integer nuevoValor) { 
    	this.nuevoValor = nuevoValor; 
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
    

    public BigDecimal getPrecioProducto() { 
    	return precioProducto; 
    }
    
    public void setPrecioProducto(BigDecimal precioProducto) { 
    	this.precioProducto = precioProducto; 
    }
    

    public String getClienteNombre() { 
    	return clienteNombre; 
    }
    
    public void setClienteNombre(String clienteNombre) { 
    	this.clienteNombre = clienteNombre; 
    }
    
}
