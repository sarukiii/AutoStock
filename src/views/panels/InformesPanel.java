package views.panels;

import controllers.*;
import models.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Panel de generación de informes del sistema.
 *
 * Permite al administrador generar seis tipos de informes diferentes:
 * - Resumen de ventas: totales, número de pedidos y media por período.
 * - Ventas por producto: unidades vendidas e ingresos de un producto concreto.
 * - Ventas por cliente: historial de compras de un cliente concreto.
 * - Productos con bajo nivel de stock: productos por debajo de un umbral.
 * - Historial de producto: evolución del stock de un producto a lo largo del tiempo.
 * - Histórico de proveedor: rendimiento de los productos de un proveedor.
 *
 * Cada informe tiene sus propios filtros (fechas, producto, cliente, proveedor
 * o umbral de stock) que se cargan dinámicamente al cambiar el tipo de informe.
 * Los resultados se muestran en una tabla y pueden exportarse a CSV.
 *
 * Solo es accesible para administradores.
 * Extiende JPanel para poder incrustarse en el área de contenido de MainView.
 */
public class InformesPanel extends JPanel {

    // Controladores para acceder a los datos de cada entidad
    private final VentaController ventaController;
    private final ProductoController productoController;
    private final ClienteController clienteController;
    private final ProveedorController proveedorController;

    // Panel central que contiene los filtros y la tabla de resultados
    private final JPanel contentPanel;
    // Selector del tipo de informe a generar
    private final JComboBox<String> tipoInformeCombo;
    // Panel de filtros: se actualiza dinámicamente según el tipo de informe
    private final JPanel filtroPanel;
    // Tabla donde se muestran los resultados del informe generado
    private final JTable informeTable;
    // Modelo de datos de la tabla (columnas y filas variables según el informe)
    private final DefaultTableModel tableModel;
    // Formato de fecha para los campos de filtro y para mostrar en la tabla
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // Botón para exportar el informe generado a un archivo CSV
    private final JButton exportarButton;

    public InformesPanel() {
        this.ventaController = new VentaController();
        this.productoController = new ProductoController();
        this.clienteController = new ClienteController();
        this.proveedorController = new ProveedorController();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel superior: selector de tipo de informe y botón de exportar ---
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectorPanel.add(new JLabel("Tipo de informe:"));

        // Los tipos de informe disponibles
        tipoInformeCombo = new JComboBox<>(new String[]{
                "Resumen de ventas",
                "Ventas por producto",
                "Ventas por cliente",
                "Productos con bajo nivel de stock",
                "Historial de producto",
                "Histórico de proveedor"
        });

        // Al cambiar el tipo de informe se actualizan los filtros dinámicamente
        tipoInformeCombo.addActionListener(e -> actualizarInformeView());
        selectorPanel.add(tipoInformeCombo);

        exportarButton = new JButton("Exportar a CSV");
        exportarButton.addActionListener(e -> exportarACSV());

        topPanel.add(selectorPanel, BorderLayout.WEST);
        topPanel.add(exportarButton, BorderLayout.EAST);

        // --- Panel de filtros (dinámico) ---
        // Se limpia y rellena cada vez que se cambia el tipo de informe
        filtroPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // --- Tabla de resultados ---
        // Las columnas cambian según el tipo de informe generado
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        informeTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(informeTable);

        // Panel central: filtros arriba, tabla de resultados abajo
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(filtroPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Ensamblaje del panel principal
        add(topPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        // Cargamos los filtros del primer tipo de informe al inicializar
        actualizarInformeView();
    }

    /**
     * Actualiza el panel de filtros según el tipo de informe seleccionado.
     *
     * Limpia los filtros anteriores y añade los controles específicos
     * para el tipo de informe elegido: campos de fecha, combo de producto,
     * combo de cliente, combo de proveedor o spinner de umbral de stock.
     * Al final añade siempre el botón "Generar" para lanzar el informe.
     */
    private void actualizarInformeView() {
        filtroPanel.removeAll();
        String informeSeleccionado = (String) tipoInformeCombo.getSelectedItem();

        // Los informes de ventas siempre necesitan filtro de rango de fechas
        if ("Resumen de ventas".equals(informeSeleccionado) ||
                "Ventas por producto".equals(informeSeleccionado) ||
                "Ventas por cliente".equals(informeSeleccionado)) {
            addDateFilters();
        }

        // Cada tipo de informe añade sus filtros específicos
        switch (informeSeleccionado) {
            case "Ventas por producto":
                addProductoFilter();
                break;
            case "Ventas por cliente":
                addClienteFilter();
                break;
            case "Productos con bajo nivel de stock":
                addStockTLimiteFilter();
                break;
            case "Historial de producto":
                addProductoFilter();
                break;
            case "Histórico de proveedor":
                addProveedorFilter();
                break;
        }

        // El botón "Generar" siempre va al final del panel de filtros
        JButton generarButton = new JButton("Generar");
        generarButton.addActionListener(e -> generarInforme());
        filtroPanel.add(generarButton);

        filtroPanel.revalidate();
        filtroPanel.repaint();
    }

    /**
     * Añade los campos de fecha de inicio y fin al panel de filtros.
     *
     * Los campos se pre-rellenan con el último mes por defecto.
     * Se identifican por nombre ("startDate" y "endDate") para poder
     * recuperarlos después al generar el informe.
     */
    private void addDateFilters() {
        JTextField startDateField = new JTextField(10);
        JTextField endDateField = new JTextField(10);

        // Usamos el nombre del componente para localizarlo más tarde
        startDateField.setName("startDate");
        endDateField.setName("endDate");

        // Por defecto mostramos el último mes
        LocalDate today = LocalDate.now();
        startDateField.setText(today.minusMonths(1).format(dateFormatter));
        endDateField.setText(today.format(dateFormatter));

        filtroPanel.add(new JLabel("Fecha inicio:"));
        filtroPanel.add(startDateField);
        filtroPanel.add(new JLabel("Fecha fin:"));
        filtroPanel.add(endDateField);
    }

    /**
     * Añade un combo de selección de producto al panel de filtros.
     * Carga todos los productos disponibles en la base de datos.
     */
    private void addProductoFilter() {
        JComboBox<Producto> productoCombo = new JComboBox<>();
        List<Producto> productos = productoController.findAll();
        for (Producto producto : productos) {
            productoCombo.addItem(producto);
        }
        filtroPanel.add(new JLabel("Producto:"));
        filtroPanel.add(productoCombo);
    }

    /**
     * Añade un combo de selección de cliente al panel de filtros.
     * Carga todos los clientes disponibles en la base de datos.
     */
    private void addClienteFilter() {
        JComboBox<Cliente> clienteCombo = new JComboBox<>();
        List<Cliente> clientes = clienteController.findAll();
        for (Cliente cliente : clientes) {
            clienteCombo.addItem(cliente);
        }
        filtroPanel.add(new JLabel("Cliente:"));
        filtroPanel.add(clienteCombo);
    }

    /**
     * Añade un combo de selección de proveedor al panel de filtros.
     * Carga todos los proveedores disponibles en la base de datos.
     */
    private void addProveedorFilter() {
        JComboBox<Proveedor> proveedorCombo = new JComboBox<>();
        List<Proveedor> proveedores = proveedorController.findAll();
        for (Proveedor proveedor : proveedores) {
            proveedorCombo.addItem(proveedor);
        }
        filtroPanel.add(new JLabel("Proveedor:"));
        filtroPanel.add(proveedorCombo);
    }

    /**
     * Añade un spinner de umbral de stock al panel de filtros.
     * El valor determina por debajo de qué cantidad se considera "bajo stock".
     */
    private void addStockTLimiteFilter() {
        JSpinner limiteSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 1000, 1));
        limiteSpinner.setName("Límite de stock");
        filtroPanel.add(new JLabel("Límite de stock:"));
        filtroPanel.add(limiteSpinner);
    }

    /**
     * Determina el estado del stock de un producto según su cantidad actual
     * y el umbral definido por el usuario.
     *
     * @param currentStock stock actual del producto
     * @param threshold    umbral mínimo definido por el usuario
     * @return texto descriptivo del estado del stock
     */
    private String getStockStatus(int currentStock, int threshold) {
        if (currentStock == 0) {
            return "FUERA DE STOCK";
        } else if (currentStock <= threshold * 0.25) {
            return "NIVEL CRÍTICO DE STOCK";
        } else if (currentStock <= threshold * 0.5) {
            return "BAJO";
        } else {
            return "REVISAR";
        }
    }

    /**
     * Lanza la generación del informe seleccionado.
     *
     * Limpia la tabla anterior, valida las fechas si el informe las requiere
     * y delega en el método específico de cada tipo de informe.
     */
    private void generarInforme() {
        String informeSeleccionado = (String) tipoInformeCombo.getSelectedItem();
        // Limpiamos la tabla antes de generar el nuevo informe
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        try {
            // Validamos el rango de fechas solo para los informes que lo usan
            if ("Resumen de ventas".equals(informeSeleccionado) ||
                    "Ventas por producto".equals(informeSeleccionado) ||
                    "Ventas por cliente".equals(informeSeleccionado)) {
                validarDates();
            }

            // Delegamos en el método correspondiente al tipo de informe
            switch (informeSeleccionado) {
                case "Resumen de ventas":
                    generarInformeResumenVentas();
                    break;
                case "Ventas por producto":
                    generarInformeVentasPorProducto();
                    break;
                case "Ventas por cliente":
                    generateVentasPorClienteReport();
                    break;
                case "Productos con bajo nivel de stock":
                    generarInformeBajoStock();
                    break;
                case "Historial de producto":
                    generarInformeHistorialProducto();
                    break;
                case "Histórico de proveedor":
                    generarInformeRendimientoProveedor();
                    break;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al generar el informe: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Genera el informe de resumen de ventas para el período seleccionado.
     *
     * Muestra en una sola fila: el rango de fechas, el total facturado,
     * el número de pedidos, el importe medio por pedido y el total de
     * unidades vendidas.
     */
    private void generarInformeResumenVentas() {
        LocalDateTime startDate = getStartDate();
        LocalDateTime endDate = getEndDate();

        String[] columnas = {
                "Fecha", "Ventas totales", "Número de pedidos",
                "Importe medio", "Productos totales vendidos"
        };
        for (String columna : columnas) {
            tableModel.addColumn(columna);
        }

        List<Venta> ventas = ventaController.findAllBetweenDates(startDate, endDate);

        // Calculamos los totales recorriendo todas las ventas del período
        BigDecimal ventasTotales = BigDecimal.ZERO;
        int pedidosTotales = ventas.size();
        int productosTotales = 0;

        for (Venta venta : ventas) {
            ventasTotales = ventasTotales.add(venta.getTotal());
            // Sumamos las unidades de todos los detalles de cada venta
            productosTotales += venta.getDetalles().stream()
                    .mapToInt(DetalleVenta::getCantidad)
                    .sum();
        }

        // Calculamos el importe medio por pedido evitando división por cero
        BigDecimal valorPromedioPedido = pedidosTotales > 0
                ? ventasTotales.divide(BigDecimal.valueOf(pedidosTotales), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        Object[] fila = {
                startDate.format(dateFormatter) + " a " + endDate.format(dateFormatter),
                ventasTotales,
                pedidosTotales,
                valorPromedioPedido,
                productosTotales
        };
        tableModel.addRow(fila);
    }

    /**
     * Genera el informe de ventas para un producto concreto en el período seleccionado.
     *
     * Muestra un resumen con totales y luego el detalle de cada venta en la que
     * aparece el producto seleccionado.
     */
    private void generarInformeVentasPorProducto() {
        JComboBox<?> productoCombo = findProductoComboBox();
        if (productoCombo == null || productoCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un producto");
            return;
        }

        Producto productoSeleccionado = (Producto) productoCombo.getSelectedItem();
        LocalDateTime startDate = getStartDate();
        LocalDateTime endDate = getEndDate();

        String[] columnas = {
                "ID producto", "Nombre producto", "Total unidades vendidas",
                "Ingresos totales", "Precio medio", "Número de ventas"
        };
        for (String columna : columnas) {
            tableModel.addColumn(columna);
        }

        List<Venta> ventas = ventaController.findAllBetweenDates(startDate, endDate);

        // Usamos la clase auxiliar ResumenVentasProducto para acumular los datos
        ResumenVentasProducto resumen = new ResumenVentasProducto(productoSeleccionado);

        for (Venta venta : ventas) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                if (detalle.getProducto().getIdProducto().equals(productoSeleccionado.getIdProducto())) {
                    resumen.addVenta(detalle);
                }
            }
        }

        if (resumen.numeroDeVentas > 0) {
            // Fila con los datos del producto
            Object[] row = {
                    resumen.producto.getIdProducto(),
                    resumen.producto.getNombre(),
                    resumen.totalUnidadesVendidas,
                    String.format("€%.2f", resumen.ingresosTotales),
                    String.format("€%.2f", resumen.getPrecioMedio()),
                    resumen.numeroDeVentas
            };
            tableModel.addRow(row);

            // Fila de resumen con totales
            Object[] resumenRow = {
                    "Resumen",
                    "",
                    "Total unidades: " + resumen.totalUnidadesVendidas,
                    "Total ingresos: €" + String.format("%.2f", resumen.ingresosTotales),
                    "Precio medio: €" + String.format("%.2f", resumen.getPrecioMedio()),
                    "Total ventas: " + resumen.numeroDeVentas
            };
            tableModel.addRow(resumenRow);
        } else {
            // Si no hay ventas para este producto en el período, mostramos una fila con ceros
            Object[] noVentasRow = {
                    productoSeleccionado.getIdProducto(),
                    productoSeleccionado.getNombre(),
                    0, "€0.00", "€0.00", 0
            };
            tableModel.addRow(noVentasRow);
        }

        // Fila vacía como separador visual
        tableModel.addRow(new Object[]{"", "", "", "", "", ""});

        // Cabecera de la sección de detalle de ventas
        Object[] detailHeader = {
                "Fecha venta", "ID venta", "Unidades", "Precio/unidad", "Total", "Cliente"
        };
        tableModel.addRow(detailHeader);

        // Detalle de cada venta en la que aparece el producto
        for (Venta venta : ventas) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                if (detalle.getProducto().getIdProducto().equals(productoSeleccionado.getIdProducto())) {
                    Object[] detalleRow = {
                            venta.getFecha().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                            venta.getIdVenta(),
                            detalle.getCantidad(),
                            String.format("€%.2f", detalle.getPrecioUnitario()),
                            String.format("€%.2f",
                                    detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()))),
                            venta.getCliente().getNombre()
                    };
                    tableModel.addRow(detalleRow);
                }
            }
        }
    }

    /**
     * Genera el informe de ventas para un cliente concreto en el período seleccionado.
     *
     * Muestra un resumen con totales del cliente y luego el detalle de
     * cada compra realizada en el período.
     */
    private void generateVentasPorClienteReport() {
        JComboBox<?> clienteCombo = findClienteComboBox();
        if (clienteCombo == null || clienteCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un cliente");
            return;
        }

        Cliente clienteSeleccionado = (Cliente) clienteCombo.getSelectedItem();
        LocalDateTime startDate = getStartDate();
        LocalDateTime endDate = getEndDate();

        String[] columnas = {
                "Fecha", "ID venta", "Importe total", "Artículos comprados", "Productos"
        };
        for (String columna : columnas) {
            tableModel.addColumn(columna);
        }

        // Filtramos las ventas del período para quedarnos solo con las de este cliente
        List<Venta> ventas = ventaController.findAllBetweenDates(startDate, endDate)
                .stream()
                .filter(venta -> venta.getCliente().getIdCliente().equals(clienteSeleccionado.getIdCliente()))
                .toList();

        // Calculamos los totales del cliente en el período
        BigDecimal importeTotal = BigDecimal.ZERO;
        int comprasTotales = ventas.size();
        int totalItems = 0;

        for (Venta venta : ventas) {
            importeTotal = importeTotal.add(venta.getTotal());
            totalItems += venta.getDetalles().stream()
                    .mapToInt(DetalleVenta::getCantidad)
                    .sum();
        }

        // Filas de resumen del cliente
        Object[] resumenRow1 = {
                "Resumen cliente", clienteSeleccionado.getNombre(), "", "", ""
        };
        Object[] resumenRow2 = {
                "Compras totales", comprasTotales,
                "Importe total", String.format("€%.2f", importeTotal), ""
        };
        Object[] resumenRow3 = {
                "Productos totales", totalItems,
                "Compra promedio",
                comprasTotales > 0 ? String.format("€%.2f", importeTotal.divide(
                        BigDecimal.valueOf(comprasTotales), 2, BigDecimal.ROUND_HALF_UP)) : "€0.00",
                ""
        };

        tableModel.addRow(resumenRow1);
        tableModel.addRow(resumenRow2);
        tableModel.addRow(resumenRow3);
        // Fila vacía como separador visual entre resumen y detalle
        tableModel.addRow(new Object[]{"", "", "", "", ""});

        // Detalle de cada venta del cliente en el período
        for (Venta venta : ventas) {
            // Construimos una cadena con todos los productos de la venta
            String productosStr = venta.getDetalles().stream()
                    .map(detalle -> String.format("%dx %s",
                            detalle.getCantidad(),
                            detalle.getProducto().getNombre()))
                    .collect(Collectors.joining(", "));

            Object[] row = {
                    venta.getFecha().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    venta.getIdVenta(),
                    String.format("€%.2f", venta.getTotal()),
                    venta.getDetalles().stream().mapToInt(DetalleVenta::getCantidad).sum(),
                    productosStr
            };
            tableModel.addRow(row);
        }
    }

    /**
     * Genera el informe de productos con stock por debajo del umbral indicado.
     *
     * Muestra una fila de resumen con el número de productos afectados y
     * luego el detalle de cada producto ordenado de menor a mayor stock,
     * con su estado (FUERA DE STOCK, NIVEL CRÍTICO, BAJO, REVISAR).
     */
    private void generarInformeBajoStock() {
        JSpinner limiteSpinner = (JSpinner) findComponentByType(filtroPanel, JSpinner.class)
                .findFirst()
                .orElse(null);

        if (limiteSpinner == null) {
            JOptionPane.showMessageDialog(this, "No se pudo encontrar el control de umbral");
            return;
        }

        int limite = (Integer) limiteSpinner.getValue();

        String[] columnas = {
                "ID producto", "Nombre", "Stock actual", "Umbral mínimo",
                "Proveedor", "Precio", "Estado"
        };
        for (String columna : columnas) {
            tableModel.addColumn(columna);
        }

        // Filtramos y ordenamos los productos por debajo del umbral
        List<Producto> productos = productoController.findAll();
        List<Producto> productosBajasExistencias = productos.stream()
                .filter(p -> p.getCantidadDisponible() <= limite)
                // Ordenamos de menor a mayor stock para ver primero los más críticos
                .sorted((p1, p2) -> Integer.compare(p1.getCantidadDisponible(), p2.getCantidadDisponible()))
                .toList();

        // Fila de resumen con el total de productos afectados
        Object[] resumenRow = {
                "Resumen",
                String.format("Productos por debajo de %d unidades:", limite),
                productosBajasExistencias.size(),
                "", "", "", ""
        };
        tableModel.addRow(resumenRow);
        // Fila vacía como separador visual
        tableModel.addRow(new Object[]{"", "", "", "", "", "", ""});

        // Detalle de cada producto con bajo stock
        for (Producto producto : productosBajasExistencias) {
            String estado = getStockStatus(producto.getCantidadDisponible(), limite);

            Object[] row = {
                    producto.getIdProducto(),
                    producto.getNombre(),
                    producto.getCantidadDisponible(),
                    limite,
                    producto.getProveedor().getNombre(),
                    String.format("€%.2f", producto.getPrecio()),
                    estado
            };
            tableModel.addRow(row);
        }
    }

    /**
     * Genera el informe de historial de un producto concreto.
     *
     * Muestra la evolución del stock a lo largo del tiempo: para cada venta
     * en la que apareció el producto, muestra el stock antes y después.
     * Si el precio de venta difiere del precio actual, también registra el
     * cambio de precio como una entrada adicional.
     */
    private void generarInformeHistorialProducto() {
        JComboBox<?> productoCombo = findProductoComboBox();
        if (productoCombo == null || productoCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un producto");
            return;
        }

        Producto productoSeleccionado = (Producto) productoCombo.getSelectedItem();

        tableModel.setColumnCount(0);
        tableModel.setRowCount(0);

        String[] columnas = {
                "Fecha", "Tipo", "Valor anterior", "Valor nuevo",
                "Cambio", "ID de venta", "Cliente"
        };
        for (String columna : columnas) {
            tableModel.addColumn(columna);
        }

        List<HistorialVentasDTO> historial = ventaController
                .findVentaHistoryByProductId(productoSeleccionado.getIdProducto());

        for (HistorialVentasDTO entry : historial) {
            // Fila con el cambio de stock producido por esta venta
            Object[] stockRow = {
                    entry.getFecha().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    "Cambio de stock",
                    entry.getAnteriorValor(),
                    entry.getNuevoValor(),
                    "-" + entry.getCantidad(),
                    entry.getIdVenta(),
                    entry.getClienteNombre()
            };
            tableModel.addRow(stockRow);

            // Si el precio de venta difiere del precio actual, registramos también el cambio de precio.
            // Esto permite detectar cuándo se vendió a un precio diferente al precio actual del producto.
            if (!entry.getPrecioUnitario().equals(entry.getPrecioProducto())) {
                Object[] precioRow = {
                        entry.getFecha().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        "Cambio de precio",
                        String.format("€%.2f", entry.getPrecioProducto()),
                        String.format("€%.2f", entry.getPrecioUnitario()),
                        String.format("€%.2f",
                                entry.getPrecioUnitario().subtract(entry.getPrecioProducto())),
                        entry.getIdVenta(),
                        entry.getClienteNombre()
                };
                tableModel.addRow(precioRow);
            }
        }
    }

    /**
     * Genera el informe de rendimiento de los productos de un proveedor concreto.
     *
     * Para cada producto del proveedor muestra el stock actual, el total de
     * unidades vendidas, la rotación de stock y los ingresos generados.
     * La rotación de stock es el cociente entre ventas totales y stock actual:
     * un valor alto indica que el producto se vende bien respecto a su stock.
     */
    private void generarInformeRendimientoProveedor() {
        JComboBox<?> proveedorCombo = findProveedorComboBox();
        if (proveedorCombo == null || proveedorCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un proveedor");
            return;
        }

        Proveedor proveedorSeleccionado = (Proveedor) proveedorCombo.getSelectedItem();

        String[] columnas = {
                "ID producto", "Nombre producto", "Stock actual",
                "Ventas totales", "Rotación de existencias", "Ingresos generados"
        };
        for (String columna : columnas) {
            tableModel.addColumn(columna);
        }

        // Filtramos los productos que pertenecen al proveedor seleccionado
        List<Producto> productosProveedor = productoController.findAll().stream()
                .filter(p -> p.getProveedor().getIdProveedor().equals(proveedorSeleccionado.getIdProveedor()))
                .toList();

        List<Venta> allVentas = ventaController.findAll();

        // Para cada producto del proveedor calculamos sus métricas de ventas
        for (Producto producto : productosProveedor) {
            int ventasTotales = 0;
            BigDecimal ingresosTotales = BigDecimal.ZERO;

            for (Venta venta : allVentas) {
                for (DetalleVenta detalle : venta.getDetalles()) {
                    if (detalle.getProducto().getIdProducto().equals(producto.getIdProducto())) {
                        ventasTotales += detalle.getCantidad();
                        ingresosTotales = ingresosTotales.add(
                                detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()))
                        );
                    }
                }
            }

            // Rotación = unidades vendidas / stock actual
            // Evitamos división por cero si el stock es 0
            double rotacionStock = producto.getCantidadDisponible() > 0
                    ? (double) ventasTotales / producto.getCantidadDisponible()
                    : 0.0;

            Object[] row = {
                    producto.getIdProducto(),
                    producto.getNombre(),
                    producto.getCantidadDisponible(),
                    ventasTotales,
                    String.format("%.2f", rotacionStock),
                    String.format("€%.2f", ingresosTotales)
            };
            tableModel.addRow(row);
        }
    }

    /**
     * Clase auxiliar para acumular datos de ventas de un producto concreto.
     *
     * Se usa en el informe de ventas por producto para calcular totales
     * sin necesidad de recorrer la lista varias veces.
     */
    private class ResumenVentasProducto {
        Producto producto;
        int totalUnidadesVendidas = 0;
        BigDecimal ingresosTotales = BigDecimal.ZERO;
        int numeroDeVentas = 0;

        ResumenVentasProducto(Producto producto) {
            this.producto = producto;
        }

        /**
         * Acumula los datos de una línea de detalle de venta.
         *
         * @param detalle línea de detalle a acumular
         */
        void addVenta(DetalleVenta detalle) {
            totalUnidadesVendidas += detalle.getCantidad();
            ingresosTotales = ingresosTotales.add(
                    detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()))
            );
            numeroDeVentas++;
        }

        /**
         * Calcula el precio medio de venta por unidad.
         *
         * @return precio medio, o cero si no hay ventas
         */
        BigDecimal getPrecioMedio() {
            return numeroDeVentas > 0
                    ? ingresosTotales.divide(BigDecimal.valueOf(totalUnidadesVendidas), 2, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;
        }
    }

    /**
     * Busca el JComboBox de productos en el panel de filtros.
     * Lo identifica por el tipo del ítem seleccionado (instancia de Producto).
     *
     * @return el combo de productos, o null si no se encuentra
     */
    private JComboBox<?> findProductoComboBox() {
        return findComponentByType(filtroPanel, JComboBox.class)
                .map(combo -> (JComboBox<?>) combo)
                .filter(combo -> combo.getSelectedItem() instanceof Producto)
                .findFirst()
                .orElse(null);
    }

    /**
     * Busca el JComboBox de clientes en el panel de filtros.
     * Lo identifica por el tipo del ítem seleccionado (instancia de Cliente).
     *
     * @return el combo de clientes, o null si no se encuentra
     */
    private JComboBox<?> findClienteComboBox() {
        return findComponentByType(filtroPanel, JComboBox.class)
                .map(combo -> (JComboBox<?>) combo)
                .filter(combo -> combo.getSelectedItem() instanceof Cliente)
                .findFirst()
                .orElse(null);
    }

    /**
     * Busca el JComboBox de proveedores en el panel de filtros.
     * Lo identifica por el tipo del ítem seleccionado (instancia de Proveedor).
     *
     * @return el combo de proveedores, o null si no se encuentra
     */
    private JComboBox<?> findProveedorComboBox() {
        return findComponentByType(filtroPanel, JComboBox.class)
                .map(combo -> (JComboBox<?>) combo)
                .filter(combo -> combo.getSelectedItem() instanceof Proveedor)
                .findFirst()
                .orElse(null);
    }

    /**
     * Busca recursivamente componentes de un tipo concreto dentro de un contenedor.
     *
     * Recorre el árbol de componentes del contenedor en profundidad y devuelve
     * un Stream con todos los componentes que son instancias del tipo indicado.
     *
     * @param container contenedor donde buscar
     * @param type      clase del tipo de componente a buscar
     * @return Stream con los componentes encontrados
     */
    private Stream<Component> findComponentByType(Container container, Class<?> type) {
        return Arrays.stream(container.getComponents())
                .flatMap(component -> {
                    if (component instanceof Container) {
                        return Stream.concat(
                                Stream.of(component),
                                findComponentByType((Container) component, type)
                        );
                    }
                    return Stream.of(component);
                })
                .filter(component -> type.isAssignableFrom(component.getClass()));
    }

    /**
     * Exporta el contenido actual de la tabla a un archivo CSV.
     *
     * Abre un selector de archivos para que el usuario elija la ubicación
     * y el nombre del archivo. Si el nombre no termina en ".csv", lo añade
     * automáticamente. Escribe la cabecera y todas las filas de datos.
     */
    private void exportarACSV() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "No hay datos para exportar",
                    "Error de exportación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser selectorArchivos = new JFileChooser();
        selectorArchivos.setDialogTitle("Exportar informe");
        selectorArchivos.setFileFilter(new FileNameExtensionFilter("Archivos CSV", "csv"));

        if (selectorArchivos.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = selectorArchivos.getSelectedFile();
            // Añadimos la extensión .csv si el usuario no la escribió
            if (!archivo.getName().toLowerCase().endsWith(".csv")) {
                archivo = new File(archivo.getAbsolutePath() + ".csv");
            }

            // try-with-resources garantiza que el writer se cierra aunque ocurra un error
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
                // Escribimos la fila de cabecera con los nombres de las columnas
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.write(tableModel.getColumnName(i));
                    if (i < tableModel.getColumnCount() - 1) {
                        writer.write(",");
                    }
                }
                writer.newLine();

                // Escribimos cada fila de datos
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
                        Object value = tableModel.getValueAt(row, col);
                        writer.write(value != null ? value.toString() : "");
                        if (col < tableModel.getColumnCount() - 1) {
                            writer.write(",");
                        }
                    }
                    writer.newLine();
                }

                JOptionPane.showMessageDialog(this,
                        "Informe exportado correctamente",
                        "Exportación exitosa",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error al exportar el informe: " + e.getMessage(),
                        "Error de exportación",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Obtiene la fecha de inicio del filtro como LocalDateTime.
     *
     * Lee el campo de texto "startDate" del panel de filtros y lo convierte
     * a LocalDateTime al inicio del día (00:00:00). Si el formato es incorrecto,
     * muestra un error y devuelve el mes anterior como valor por defecto.
     *
     * @return fecha de inicio del período a consultar
     */
    private LocalDateTime getStartDate() {
        try {
            JTextField startDateField = (JTextField) findComponentByName(filtroPanel, "startDate");
            if (startDateField == null) {
                throw new IllegalStateException("Fecha de inicio no encontrada");
            }
            String dateText = startDateField.getText().trim();
            if (dateText.isEmpty()) {
                throw new DateTimeParseException("Fecha vacía", dateText, 0);
            }
            return LocalDate.parse(dateText, dateFormatter).atStartOfDay();
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this,
                    "Formato de fecha de inicio no válido. Utilice yyyy-MM-dd",
                    "Error de fecha",
                    JOptionPane.ERROR_MESSAGE);
            return LocalDateTime.now().minusMonths(1);
        } catch (Exception e) {
            return LocalDateTime.now().minusMonths(1);
        }
    }

    /**
     * Obtiene la fecha de fin del filtro como LocalDateTime.
     *
     * Lee el campo de texto "endDate" del panel de filtros y lo convierte
     * a LocalDateTime al final del día (23:59:59) para incluir todas las
     * ventas de ese día. Si hay error, devuelve la fecha actual.
     *
     * @return fecha de fin del período a consultar
     */
    private LocalDateTime getEndDate() {
        try {
            JTextField endDateField = (JTextField) findComponentByName(filtroPanel, "endDate");
            // atTime(23, 59, 59) garantiza que se incluyen todas las ventas del último día
            return LocalDate.parse(endDateField.getText(), dateFormatter).atTime(23, 59, 59);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    /**
     * Valida que el rango de fechas sea coherente (inicio anterior al fin).
     *
     * @throws IllegalArgumentException si la fecha de fin es anterior a la de inicio
     */
    private void validarDates() {
        LocalDateTime startDate = getStartDate();
        LocalDateTime endDate = getEndDate();

        if (endDate.isBefore(startDate)) {
            JOptionPane.showMessageDialog(this,
                    "La fecha de finalización no puede ser anterior a la fecha de inicio",
                    "Error de fecha",
                    JOptionPane.ERROR_MESSAGE);
            throw new IllegalArgumentException("Intervalo de fechas no válido");
        }
    }

    /**
     * Busca un componente por su nombre dentro de un contenedor de forma recursiva.
     *
     * Se usa para localizar los campos de fecha ("startDate" y "endDate")
     * en el panel de filtros.
     *
     * @param container contenedor donde buscar
     * @param nombre    nombre del componente a buscar
     * @return el componente encontrado, o null si no existe
     */
    private Component findComponentByName(Container container, String nombre) {
        for (Component component : container.getComponents()) {
            if (nombre.equals(component.getName())) {
                return component;
            }
            if (component instanceof Container) {
                Component result = findComponentByName((Container) component, nombre);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}