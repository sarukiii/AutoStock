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

public class InformesPanel extends JPanel {
    private final VentaController ventaController;
    private final ProductoController productoController;
    private final ClienteController clienteController;
    private final ProveedorController proveedorController;
    private final JPanel contentPanel;
    private final JComboBox<String> tipoInformeCombo;
    private final JPanel filtroPanel;
    private final JTable informeTable;
    private final DefaultTableModel tableModel;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final JButton exportarButton;

    public InformesPanel() {
        this.ventaController = new VentaController();
        this.productoController = new ProductoController();
        this.clienteController = new ClienteController();
        this.proveedorController = new ProveedorController();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel superior
        JPanel topPanel = new JPanel(new BorderLayout());

        // Selección del tipo de informe
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectorPanel.add(new JLabel("Tipo de informe:"));

        tipoInformeCombo = new JComboBox<>(new String[]{
                "Resumen de ventas",
                "Ventas por producto",
                "Ventas por cliente",
                "Productos con bajo nivel de stock",
                "Historial de producto",
                "Histórico de proveedor"
        });

        tipoInformeCombo.addActionListener(e -> actualizarInformeView());
        selectorPanel.add(tipoInformeCombo);

        // Exportar button
        exportarButton = new JButton("Exportar a CSV");
        exportarButton.addActionListener(e -> exportarACSV());

        topPanel.add(selectorPanel, BorderLayout.WEST);
        topPanel.add(exportarButton, BorderLayout.EAST);

        // Filtro panel
        filtroPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Tabla
        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        informeTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(informeTable);

        // Panel
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(filtroPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Componentes
        add(topPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        // Carga inicial de datos
        actualizarInformeView();
    }

    private void actualizarInformeView() {
        filtroPanel.removeAll();
        String informeSeleccionado = (String) tipoInformeCombo.getSelectedItem();

        // Filtros de fecha
        if ("Resumen de ventas".equals(informeSeleccionado) ||
                "Ventas por producto".equals(informeSeleccionado) ||
                "Ventas por cliente".equals(informeSeleccionado)) {

            addDateFilters();
        }

        // Filtros específicos para cada tipo de informe
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

        // Generar button
        JButton generatButton = new JButton("Generar ");
        generatButton.addActionListener(e -> generarInforme());
        filtroPanel.add(generatButton);

        filtroPanel.revalidate();
        filtroPanel.repaint();
    }

    private void addDateFilters() {
        JTextField startDateField = new JTextField(10);
        JTextField endDateField = new JTextField(10);

        startDateField.setName("startDate");
        endDateField.setName("endDate");
        
        LocalDate today = LocalDate.now();
        startDateField.setText(today.minusMonths(1).format(dateFormatter));
        endDateField.setText(today.format(dateFormatter));

        filtroPanel.add(new JLabel("Fecha inicio:"));
        filtroPanel.add(startDateField);
        filtroPanel.add(new JLabel("Fecha fin:"));
        filtroPanel.add(endDateField);
    }

    private void addProductoFilter() {
        JComboBox<Producto> productoCombo = new JComboBox<>();
        List<Producto> productos = productoController.findAll();
        for (Producto producto : productos) {
            productoCombo.addItem(producto);
        }
        filtroPanel.add(new JLabel("Producto:"));
        filtroPanel.add(productoCombo);
    }

    private void addClienteFilter() {
        JComboBox<Cliente> clienteCombo = new JComboBox<>();
        List<Cliente> clientes = clienteController.findAll();
        for (Cliente cliente : clientes) {
            clienteCombo.addItem(cliente);
        }
        filtroPanel.add(new JLabel("Cliente:"));
        filtroPanel.add(clienteCombo);
    }

    private void addProveedorFilter() {
        JComboBox<Proveedor> proveedorCombo = new JComboBox<>();
        List<Proveedor> proveedores = proveedorController.findAll();
        for (Proveedor proveedor : proveedores) {
            proveedorCombo.addItem(proveedor);
        }
        filtroPanel.add(new JLabel("Proveedor:"));
        filtroPanel.add(proveedorCombo);
    }

    private String getStockStatus(int currentStock, int threshold) {
        if (currentStock == 0) {
            return "FUERA DE STOCK";
        } else if (currentStock <= threshold * 0.25) {
            return "NIVEL CRITICO DE STOCK";
        } else if (currentStock <= threshold * 0.5) {
            return "BAJO";
        } else {
            return "REVISAR";
        }
    }

    private void addStockTLimiteFilter() {
        JSpinner limiteSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 1000, 1));
        limiteSpinner.setName("Límite de stock");
        filtroPanel.add(new JLabel("Límite de stock:"));
        filtroPanel.add(limiteSpinner);
    }

    private void generarInforme() {
        String informeSeleccionado = (String) tipoInformeCombo.getSelectedItem();
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        try {
            
            if ("Resumen de ventas".equals(informeSeleccionado) ||
                    "Ventas por producto".equals(informeSeleccionado) ||
                    "Ventas por cliente".equals(informeSeleccionado)) {
                validarDates();
            }
            
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
            		"Error generating report: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generarInformeResumenVentas() {
        // Rango de fechas
        LocalDateTime startDate = getStartDate();
        LocalDateTime endDate = getEndDate();

        // Columnas
        String[] columnas = {
        		"Fecha", "Ventas totales", "Número de pedidos",
                "Importe medio", "Productos totales vendidos"
        };
        for (String columna : columnas) {
            tableModel.addColumn(columna);
        }

        // Datos de ventas
        List<Venta> ventas = ventaController.findAllBetweenDates(startDate, endDate);

        // Calcular totales
        BigDecimal ventasTotales = BigDecimal.ZERO;
        int pedidosTotales = ventas.size();
        int productosTotales = 0;

        for (Venta venta : ventas) {
            ventasTotales = ventasTotales.add(venta.getTotal());
            productosTotales += venta.getDetalles().stream()
                    .mapToInt(DetalleVenta::getCantidad)
                    .sum();
        }

        BigDecimal valorPromedioPedido = pedidosTotales > 0
                ? ventasTotales.divide(BigDecimal.valueOf(pedidosTotales), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        // Fila de resumen
        Object[] fila = {
                startDate.format(dateFormatter) + " to " + endDate.format(dateFormatter),
                ventasTotales,
                pedidosTotales,
                valorPromedioPedido,
                productosTotales
        };
        tableModel.addRow(fila);
    }

    private void generarInformeVentasPorProducto() {
        // Producto seleccionado del filtro
        JComboBox<?> productoCombo = findProductoComboBox();
        if (productoCombo == null || productoCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un producto");
            return;
        }

        Producto productoSeleccionado = (Producto) productoCombo.getSelectedItem();

        // Rango de fechas
        LocalDateTime startDate = getStartDate();
        LocalDateTime endDate = getEndDate();

        // Columnas
        String[] columnas = {
        		"ID producto", "Nombre producto", "Total unidades vendidas",
                "Ingresos totales", "Precio medio", "Número de ventas"
        };
        for (String columna : columnas) {
            tableModel.addColumn(columna);
        }

        // Columnas
        List<Venta> ventas = ventaController.findAllBetweenDates(startDate, endDate);

        // Crear resumen solo para el producto seleccionado
        ResumenVentasProducto resumen = new ResumenVentasProducto(productoSeleccionado);

        // Filtrar y procesar ventas solo para el producto seleccionado
        for (Venta venta : ventas) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                if (detalle.getProducto().getIdProducto().equals(productoSeleccionado.getIdProducto())) {
                    resumen.addVenta(detalle);
                }
            }
        }

        // Fila de datos para el producto seleccionado
        if (resumen.numeroDeVentas > 0) {
            Object[] row = {
                    resumen.producto.getIdProducto(),
                    resumen.producto.getNombre(),
                    resumen.totalUnidadesVendidas,
                    String.format("€%.2f", resumen.ingresosTotales),
                    String.format("€%.2f", resumen.getPrecioMedio()),
                    resumen.numeroDeVentas
            };
            tableModel.addRow(row);

            // Resumenes
            Object[] resumenRow = {
                    "Resumen",
                    "",
                    "Total Units: " + resumen.totalUnidadesVendidas,
                    "Total Revenue: €" + String.format("%.2f", resumen.ingresosTotales),
                    "Avg Price: €" + String.format("%.2f", resumen.getPrecioMedio()),
                    "Total Sales: " + resumen.numeroDeVentas
            };
            tableModel.addRow(resumenRow);
        } else {
            // No se encontraron ventas para el producto seleccionado.
            Object[] noVentasRow = {
                    productoSeleccionado.getIdProducto(),
                    productoSeleccionado.getNombre(),
                    0,
                    "€0.00",
                    "€0.00",
                    0
            };
            tableModel.addRow(noVentasRow);
        }

        // Añadir detalle venta
        tableModel.addRow(new Object[]{"", "", "", "", "", ""}); // fila vacía para espacio

        // Detalle ventas
        Object[] detailHeader = {
   
        		"Fecha venta", "ID venta", "Unidades", "Precio/unidad", "Total", "Cliente"
        };
        tableModel.addRow(detailHeader);

        // Agregar encabezados de detalle
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

    private void generateVentasPorClienteReport() {
        // Cliente seleccionado del filtro
        JComboBox<?> clienteCombo = findClienteComboBox();
        if (clienteCombo == null || clienteCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un cliente");
            return;
        }

        Cliente clienteSeleccionado = (Cliente) clienteCombo.getSelectedItem();
        LocalDateTime startDate = getStartDate();
        LocalDateTime endDate = getEndDate();

        // Columnas
        String[] columnas = {
        		"Fecha", "ID venta", "Importe total", "Artículos comprados", "Productos"
        };
        for (String columna : columnas) {
            tableModel.addColumn(columna);
        }

        // Datos de ventas para el cliente seleccionado
        List<Venta> ventas = ventaController.findAllBetweenDates(startDate, endDate)
                .stream()
                .filter(venta -> venta.getCliente().getIdCliente().equals(clienteSeleccionado.getIdCliente()))
                .toList();

        // Información resumida
        BigDecimal importeTotal = BigDecimal.ZERO;
        int comprasTotales = ventas.size();
        int totalItems = 0;

        for (Venta venta : ventas) {
            importeTotal = importeTotal.add(venta.getTotal());
            totalItems += venta.getDetalles().stream()
                    .mapToInt(DetalleVenta::getCantidad)
                    .sum();
        }

        // Resumen filas
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
        tableModel.addRow(new Object[]{"", "", "", "", ""}); // Fila vacía para espaciar

        // Detalle filas
        for (Venta venta : ventas) {
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

    private JComboBox<?> findClienteComboBox() {
        return findComponentByType(filtroPanel, JComboBox.class)
                .map(combo -> (JComboBox<?>) combo)
                .filter(combo -> combo.getSelectedItem() instanceof Cliente)
                .findFirst()
                .orElse(null);
    }

    private void generarInformeHistorialProducto() {
        // Obtener producto seleccionado del filtro
        JComboBox<?> productoCombo = findProductoComboBox();
        if (productoCombo == null || productoCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un producto");
            return;
        }

        Producto productoSeleccionado = (Producto) productoCombo.getSelectedItem();

        // Borrar columnas existentes
        tableModel.setColumnCount(0);
        tableModel.setRowCount(0);

        // Columnas
        String[] columnas = {
        		"Fecha", "Tipo", "Valor anterior", "Valor nuevo",
                "Cambiar", "ID de venta", "Cliente"
        };
        for (String columna : columnas) {
            tableModel.addColumn(columna);
        }

        // Historial de este producto
        List<HistorialVentasDTO> historial = ventaController.findVentaHistoryByProductId(productoSeleccionado.getIdProducto());

        for (HistorialVentasDTO entry : historial) {
            // Agregar entrada de cambio de stock
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

            //  Agregar entrada de precio si el precio cambia
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

    private void generarInformeRendimientoProveedor() {
        // Obtener proveedor seleccionado del filtro
        JComboBox<?> clienteCombo = findProveedorComboBox();
        if (clienteCombo == null || clienteCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un proveedor");
            return;
        }

        Proveedor proveedorSeleccionado = (Proveedor) clienteCombo.getSelectedItem();

        // Columnas
        String[] columnas = {
        		"ID producto", "Nombre producto", "Stock actual",
                "Ventas totales", "Rotación de existencias", "Ingresos generados"
        };
        for (String columna : columnas) {
            tableModel.addColumn(columna);
        }

        // Obtener todos los productos de este proveedor
        List<Producto> productosProveedor = productoController.findAll().stream()
                .filter(p -> p.getProveedor().getIdProveedor().equals(proveedorSeleccionado.getIdProveedor()))
                .toList();

        // Obtener todas las ventas
        List<Venta> allVentas = ventaController.findAll();

        // Rendimiento por producto
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

            // Calcular la rotación de stock (ventas totales / stock actual)
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

    // Clases auxiliares para resumir datos
    private class ResumenVentasProducto {
        Producto producto;
        int totalUnidadesVendidas = 0;
        BigDecimal ingresosTotales = BigDecimal.ZERO;
        int numeroDeVentas = 0;

        ResumenVentasProducto(Producto product) {
            this.producto = product;
        }

        void addVenta(DetalleVenta detalle) {
            totalUnidadesVendidas += detalle.getCantidad();
            ingresosTotales = ingresosTotales.add(
                    detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()))
            );
            numeroDeVentas++;
        }

        BigDecimal getPrecioMedio() {
            return numeroDeVentas > 0
                    ? ingresosTotales.divide(BigDecimal.valueOf(totalUnidadesVendidas), 2, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;
        }
    }

    // Métodos auxiliares para encontrar componentes
    private JComboBox<?> findProductoComboBox() {
        return findComponentByType(filtroPanel, JComboBox.class)
                .map(combo -> (JComboBox<?>) combo)
                .filter(combo -> combo.getSelectedItem() instanceof Producto)
                .findFirst()
                .orElse(null);
    }

    private JComboBox<?> findProveedorComboBox() {
        return findComponentByType(filtroPanel, JComboBox.class)
                .map(combo -> (JComboBox<?>) combo)
                .filter(combo -> combo.getSelectedItem() instanceof Proveedor)
                .findFirst()
                .orElse(null);
    }

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

    private void generarInformeBajoStock() {
        // Límite con spinner
        JSpinner limiteSpinner = (JSpinner) findComponentByType(filtroPanel, JSpinner.class)
                .findFirst()
                .orElse(null);

        if (limiteSpinner == null) {
            JOptionPane.showMessageDialog(this, "No se pudo encontrar el control de umbral");
            return;
        }

        int limite = (Integer) limiteSpinner.getValue();

        // Columnas
        String[] columnas = {
        		"ID producto", "Nombre", "Stock actual", "Umbral minimo",
                "Proveedor", "Precio", "Estado"
        };
        for (String columna : columnas) {
            tableModel.addColumn(columna);
        }

        // Obtener todos los productos
        List<Producto> productos = productoController.findAll();

        // Filtrar y clasificar productos por nivel de existencias
        List<Producto> productosBajasExistencias = productos.stream()
                .filter(p -> p.getCantidadDisponible() <= limite)
                .sorted((p1, p2) -> Integer.compare(p1.getCantidadDisponible(), p2.getCantidadDisponible()))
                .toList();

        // Resumen fila
        Object[] resumenRow = {
                "Resumen",
                String.format("Productos por debajo de %d unidades:", limite),
                productosBajasExistencias.size(),
                "",
                "",
                "",
                ""
        };
        tableModel.addRow(resumenRow);
        tableModel.addRow(new Object[]{"", "", "", "", "", "", ""}); // Fila vacía

        // Filas de productos
        for (Producto producto : productosBajasExistencias) {
            String status = getStockStatus(producto.getCantidadDisponible(), limite);

            Object[] row = {
                    producto.getIdProducto(),
                    producto.getNombre(),
                    producto.getCantidadDisponible(),
                    limite,
                    producto.getProveedor().getNombre(),
                    String.format("€%.2f", producto.getPrecio()),
                    status
            };
            tableModel.addRow(row);
        }
    }

    private void exportarACSV() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
            		"No hay datos para exportar",
                    "Error de exportación",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser selectorArchivos = new JFileChooser();
        selectorArchivos.setDialogTitle("Informe de exportación");
        selectorArchivos.setFileFilter(new FileNameExtensionFilter("CSV Archivos", "csv"));

        if (selectorArchivos.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = selectorArchivos.getSelectedFile();
            if (!archivo.getName().toLowerCase().endsWith(".csv")) {
                archivo = new File(archivo.getAbsolutePath() + ".csv");
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
                // Encabezado
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.write(tableModel.getColumnName(i));
                    if (i < tableModel.getColumnCount() - 1) {
                        writer.write(",");
                    }
                }
                writer.newLine();

                // Datos
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
                		"Informe exportado exitosamente",
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
            		"Formato de fecha de inicio no válido. Utilice aaaa-MM-dd",
                    "Error de fecha",
                    JOptionPane.ERROR_MESSAGE);
            return LocalDateTime.now().minusMonths(1);
        } catch (Exception e) {
            return LocalDateTime.now().minusMonths(1);
        }
    }

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

    private LocalDateTime getEndDate() {
        try {
            JTextField endDateField = (JTextField) findComponentByName(filtroPanel, "endDate");
            return LocalDate.parse(endDateField.getText(), dateFormatter).atTime(23, 59, 59);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

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