-- Create the database
CREATE
DATABASE IF NOT EXISTS autostock_inventory;
USE
autostock_inventory;

-- Create Rol table
CREATE TABLE Rol
(
    id_rol      INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255)
);

-- Create Usuario table
CREATE TABLE Usuario
(
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    email      VARCHAR(100) NOT NULL UNIQUE,
    contrasena VARCHAR(255) NOT NULL,
    id_rol     INT,
    FOREIGN KEY (id_rol) REFERENCES Rol (id_rol)
);

-- Create Cliente table
CREATE TABLE Cliente
(
    id_cliente INT AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    telefono   VARCHAR(20),
    direccion  VARCHAR(255),
    email      VARCHAR(100),
    empresa    VARCHAR(100)
);

-- Create Proveedor table
CREATE TABLE Proveedor
(
    id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    contacto     VARCHAR(100),
    telefono     VARCHAR(20),
    direccion    VARCHAR(255),
    email        VARCHAR(100)
);

-- Create Producto table
CREATE TABLE Producto
(
    id_producto         INT AUTO_INCREMENT PRIMARY KEY,
    nombre              VARCHAR(100)   NOT NULL,
    descripcion         TEXT,
    precio              DECIMAL(10, 2) NOT NULL,
    cantidad_disponible INT            NOT NULL DEFAULT 0,
    id_proveedor        INT,
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor (id_proveedor)
);

-- Create Venta table
CREATE TABLE Venta
(
    id_venta   INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT,
    id_usuario INT,
    fecha      DATETIME DEFAULT CURRENT_TIMESTAMP,
    total      DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (id_cliente) REFERENCES Cliente (id_cliente),
    FOREIGN KEY (id_usuario) REFERENCES Usuario (id_usuario)
);

-- Create Detalle_Venta table
CREATE TABLE Detalle_Venta
(
    id_detalle      INT AUTO_INCREMENT PRIMARY KEY,
    id_venta        INT,
    id_producto     INT,
    cantidad        INT            NOT NULL,
    precio_unitario DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (id_venta) REFERENCES Venta (id_venta),
    FOREIGN KEY (id_producto) REFERENCES Producto (id_producto)
);

-- Insert default roles
INSERT INTO Rol (nombre, descripcion)
VALUES ('Administrador', 'Acceso completo al sistema'),
       ('Empleado', 'Acceso limitado a operaciones básicas');

-- Create indexes for better performance
CREATE INDEX idx_producto_nombre ON Producto (nombre);
CREATE INDEX idx_cliente_nombre ON Cliente (nombre);
CREATE INDEX idx_proveedor_nombre ON Proveedor (nombre);
CREATE INDEX idx_venta_fecha ON Venta (fecha);

-- Usuario add last_login
ALTER TABLE Usuario ADD COLUMN last_login DATETIME;