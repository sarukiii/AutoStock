# AutoStock — Sistema de Gestión de Inventarios

Aplicación de escritorio desarrollada en Java como Trabajo de Fin de Grado del ciclo formativo **DAM (Desarrollo de Aplicaciones Multiplataforma)**. AutoStock permite a pequeños y medianos negocios gestionar su inventario, ventas, clientes y proveedores desde una interfaz de escritorio intuitiva.

---

## Características principales

- **Autenticación y roles**: sistema de login con dos roles diferenciados (Administrador y Empleado). El menú de administración solo es visible para usuarios con rol Administrador.
- **Gestión de productos**: alta, baja y modificación de productos con control de stock en tiempo real.
- **Gestión de ventas**: registro de nuevas ventas con selección de productos y cliente, con actualización automática del stock.
- **Historial de ventas**: consulta del historial completo de transacciones.
- **Gestión de clientes y proveedores**: CRUD completo, accesible solo para administradores.
- **Gestión de usuarios**: el administrador puede crear, editar y eliminar usuarios del sistema.
- **Informes**: panel de informes con resumen de ventas por producto, accesible solo para administradores.

---

## Tecnologías utilizadas

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 23 | Lenguaje principal |
| Java Swing | — | Interfaz gráfica de escritorio |
| MySQL | 8.0 | Base de datos relacional |
| MySQL Connector/J | 9.1.0 | Driver JDBC para conexión a MySQL |

---

## Arquitectura

El proyecto sigue el patrón **MVC (Modelo-Vista-Controlador)**:

```
src/
├── config/         # Conexión a la base de datos y excepciones personalizadas
├── controllers/    # Lógica de negocio (Cliente, Producto, Proveedor, Usuario, Venta)
├── models/         # Entidades del dominio (Cliente, Producto, Proveedor, Usuario, Venta, etc.)
├── state/          # Estado global de la sesión (usuario logueado, rol activo)
├── views/          # Vistas principales (Login, Registro, Ventana principal)
│   └── panels/     # Paneles de cada módulo (Productos, Ventas, Clientes, etc.)
└── Main.java       # Punto de entrada de la aplicación
database/
└── init.sql        # Script de inicialización de la base de datos
lib/
└── mysql-connector-j-9.1.0.jar
```

Aspectos técnicos destacables:
- Uso de **PreparedStatement** en todas las consultas SQL (prevención de SQL injection).
- **Hashing de contraseñas** con SHA-256 antes de almacenarlas.
- Manejo de excepciones con excepciones personalizadas (`DatabaseException`) y logging con `java.util.logging`.
- Cierre correcto de recursos JDBC en bloques `finally`.
- Control de acceso por rol implementado tanto en la capa de vista como en la lógica de negocio.

---

## Requisitos previos

- **JDK 17 o superior** (probado con JDK 23)
- **MySQL Server 8.0**
- **VS Code** con la extensión [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) (o Eclipse IDE)

---

## Instalación y configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/autostock.git
cd autostock
```

### 2. Crear la base de datos

Conéctate a tu instancia de MySQL y ejecuta el script de inicialización:

```bash
mysql -u root -p < database/init.sql
```

O desde MySQL Workbench: abre el archivo `database/init.sql` y ejecútalo.

### 3. Configurar la conexión a la base de datos

Abre el archivo `src/config/DatabaseConnection.java` y ajusta las credenciales a tu entorno local:

```java
private static final String URL = "jdbc:mysql://localhost:3306/autostock_inventory";
private static final String USER = "root";
private static final String PASSWORD = "tu_contraseña";
```

### 4. Añadir el driver de MySQL al classpath

En VS Code, en el panel **Java Projects → Referenced Libraries**, añade el archivo:

```
lib/mysql-connector-j-9.1.0.jar
```

### 5. Ejecutar la aplicación

Abre `src/Main.java` y haz clic en **Run** (▶), o desde la terminal:

```bash
javac -cp lib/mysql-connector-j-9.1.0.jar -d bin src/**/*.java src/Main.java
java -cp bin:lib/mysql-connector-j-9.1.0.jar Main
```

### 6. Crear el primer usuario administrador

Al arrancar la aplicación por primera vez, usa la opción **Registro** para crear tu usuario. Luego, desde MySQL, asígnale el rol de administrador:

```sql
UPDATE autostock_inventory.Usuario SET id_rol = 1 WHERE email = 'tu@email.com';
```

Los roles disponibles son:
- `1` → Administrador (acceso completo)
- `2` → Empleado (acceso limitado a productos y ventas)

---

## Capturas de pantalla

> *Próximamente*

---

## Posibles mejoras futuras

- Migrar las credenciales de base de datos a un archivo `.properties` externo.
- Sustituir SHA-256 sin salt por BCrypt para el hashing de contraseñas.
- Exportación de informes a PDF o Excel.
- Empaquetado como `.jar` ejecutable o instalador nativo con jpackage.

---

## Autor

**Sara** — DAM, promoción 2024  
[LinkedIn](#) · [GitHub](#)
