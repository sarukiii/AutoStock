package views;

import controllers.VentaController;
import models.DetalleVenta;
import models.Venta;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel que muestra el historial completo de ventas realizadas.
 *
 * Muestra todas las ventas en una tabla con búsqueda por nombre de cliente
 * o usuario. Al hacer doble clic sobre una venta (o desde el menú contextual)
 * se abre un diálogo con el detalle completo de la venta: productos, cantidades,
 * precios unitarios, subtotales y total.
 *
 * También permite eliminar ventas, lo que restaura automáticamente el stock
 * de los productos vendidos (gestionado por VentaController).
 *
 * Extiende JPanel para poder incrustarse en el área de contenido de MainView.
 */
public class HistorialVentasPanel extends JPanel {

    // Controlador para acceder a la lógica de negocio de ventas
    private final VentaController ventaController;
    // Tabla que muestra el listado de ventas
    private final JTable ventasTable;
    // Modelo de datos de la tabla (gestiona filas y columnas)
    private final DefaultTableModel tableModel;
    // Campo de texto para filtrar ventas por cliente o usuario
    private final JTextField busquedaField;
    // Formato de fecha y hora para mostrar en la tabla
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public HistorialVentasPanel() {
        this.ventaController = new VentaController();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel superior: búsqueda ---
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        busquedaField = new JTextField(20);
        // Permitimos buscar pulsando Intro en el campo de búsqueda
        busquedaField.addActionListener(e -> actualizarTable(busquedaField.getText()));

        JButton buscarButton = new JButton("Buscar");
        buscarButton.addActionListener(e -> actualizarTable(busquedaField.getText()));

        JPanel buscarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buscarPanel.add(new JLabel("Buscar:"));
        buscarPanel.add(busquedaField);
        buscarPanel.add(buscarButton);

        // Panel reservado para futuros filtros de rango de fechas
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        topPanel.add(buscarPanel, BorderLayout.WEST);
        topPanel.add(filterPanel, BorderLayout.EAST);

        // --- Tabla de ventas ---
        // isCellEditable devuelve false para que el usuario no pueda editar
        // las celdas directamente
        String[] columnas = {"ID", "Fecha", "Cliente", "Usuario", "Total", "Items"};
        tableModel = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ventasTable = new JTable(tableModel);
        // Solo permitimos seleccionar una fila a la vez
        ventasTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Ajustamos el ancho preferido de cada columna
        ventasTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        ventasTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        ventasTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        ventasTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        ventasTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        ventasTable.getColumnModel().getColumn(5).setPreferredWidth(80);

        // Doble clic sobre una fila abre el diálogo de detalles de la venta
        ventasTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showDetalleVenta();
                }
            }
        });

        // --- Menú contextual con opciones sobre la venta seleccionada ---
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem viewDetallesItem = new JMenuItem("Ver detalles");
        JMenuItem deleteItem = new JMenuItem("Borrar");

        viewDetallesItem.addActionListener(e -> showDetalleVenta());
        deleteItem.addActionListener(e -> deleteVentaSeleccionada());

        popupMenu.add(viewDetallesItem);
        popupMenu.add(deleteItem);

        // Listener del ratón para mostrar el menú contextual al hacer clic derecho.
        // Se comprueban tanto mousePressed como mouseReleased porque el evento
        // de menú contextual varía según el sistema operativo (Windows vs Mac/Linux).
        ventasTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    mostrarMenuContextual(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    mostrarMenuContextual(e);
                }
            }

            private void mostrarMenuContextual(MouseEvent e) {
                int row = ventasTable.rowAtPoint(e.getPoint());
                if (row >= 0 && row < ventasTable.getRowCount()) {
                    // Seleccionamos la fila sobre la que se hizo clic derecho
                    ventasTable.setRowSelectionInterval(row, row);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        // --- Ensamblaje del panel principal ---
        add(topPanel, BorderLayout.NORTH);
        // Envolvemos la tabla en un JScrollPane para que tenga barra de desplazamiento
        add(new JScrollPane(ventasTable), BorderLayout.CENTER);

        // Cargamos todas las ventas al inicializar el panel
        actualizarTable("");
    }

    /**
     * Actualiza el contenido de la tabla filtrando por el término de búsqueda.
     *
     * Carga todas las ventas de la base de datos y filtra en memoria
     * las que contienen el término en el nombre del cliente o del usuario.
     * Si el término está vacío, muestra todas las ventas.
     *
     * @param busquedaTerm texto a buscar (puede estar vacío para mostrar todas)
     */
    private void actualizarTable(String busquedaTerm) {
        // Vaciamos la tabla antes de rellenarla con los nuevos datos
        tableModel.setRowCount(0);
        List<Venta> ventas = ventaController.findAll();

        for (Venta venta : ventas) {
            String clienteNombre = venta.getCliente().getNombre().toLowerCase();
            String usuarioNombre = venta.getUsuario().getNombre().toLowerCase();
            String termino = busquedaTerm.toLowerCase();

            // Filtramos por nombre de cliente o de usuario
            if (termino.isEmpty() ||
                    clienteNombre.contains(termino) ||
                    usuarioNombre.contains(termino)) {

                tableModel.addRow(new Object[]{
                        venta.getIdVenta(),
                        // Formateamos la fecha para que sea legible en la tabla
                        formatter.format(venta.getFecha()),
                        venta.getCliente().getNombre(),
                        venta.getUsuario().getNombre(),
                        String.format("€%.2f", venta.getTotal()),
                        // Mostramos el número de líneas de detalle de la venta
                        venta.getDetalles().size()
                });
            }
        }
    }

    /**
     * Muestra el diálogo de detalles de la venta seleccionada en la tabla.
     *
     * El diálogo muestra la información general de la venta (ID, fecha, cliente,
     * usuario) y una tabla con todas las líneas de detalle (producto, cantidad,
     * precio unitario, subtotal) y el total de la venta.
     */
    private void showDetalleVenta() {
        int filaSeleccionada = ventasTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer ventaId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            Venta venta = ventaController.findById(ventaId);

            if (venta != null) {
                // Creamos un diálogo modal para mostrar los detalles
                JDialog detallesDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                        "Detalles de la venta", true);
                detallesDialog.setLayout(new BorderLayout(10, 10));

                // --- Panel con información general de la venta ---
                JPanel detallesPanel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, 5, 5, 5);

                gbc.gridx = 0; gbc.gridy = 0;
                detallesPanel.add(new JLabel("ID venta:"), gbc);
                gbc.gridx = 1;
                detallesPanel.add(new JLabel(venta.getIdVenta().toString()), gbc);

                gbc.gridx = 0; gbc.gridy = 1;
                detallesPanel.add(new JLabel("Fecha:"), gbc);
                gbc.gridx = 1;
                detallesPanel.add(new JLabel(formatter.format(venta.getFecha())), gbc);

                gbc.gridx = 0; gbc.gridy = 2;
                detallesPanel.add(new JLabel("Cliente:"), gbc);
                gbc.gridx = 1;
                detallesPanel.add(new JLabel(venta.getCliente().getNombre()), gbc);

                gbc.gridx = 0; gbc.gridy = 3;
                detallesPanel.add(new JLabel("Usuario:"), gbc);
                gbc.gridx = 1;
                detallesPanel.add(new JLabel(venta.getUsuario().getNombre()), gbc);

                // --- Tabla con las líneas de detalle de la venta ---
                String[] columns = {"Producto", "Cantidad", "Precio/unidad", "Subtotal"};
                DefaultTableModel itemsModel = new DefaultTableModel(columns, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };

                for (DetalleVenta detalle : venta.getDetalles()) {
                    // Calculamos el subtotal de cada línea para mostrarlo en la tabla
                    BigDecimal subtotal = detalle.getPrecioUnitario()
                            .multiply(BigDecimal.valueOf(detalle.getCantidad()));

                    itemsModel.addRow(new Object[]{
                            detalle.getProducto().getNombre(),
                            detalle.getCantidad(),
                            String.format("€%.2f", detalle.getPrecioUnitario()),
                            String.format("€%.2f", subtotal)
                    });
                }

                JTable itemsTable = new JTable(itemsModel);
                JScrollPane scrollPane = new JScrollPane(itemsTable);
                scrollPane.setPreferredSize(new Dimension(500, 200));

                // --- Panel con el total de la venta ---
                JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                totalPanel.add(new JLabel("Total: "));
                JLabel totalLabel = new JLabel(String.format("€%.2f", venta.getTotal()));
                // Mostramos el total en negrita para destacarlo
                totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD));
                totalPanel.add(totalLabel);

                // Ensamblamos el diálogo
                detallesDialog.add(detallesPanel, BorderLayout.NORTH);
                detallesDialog.add(scrollPane, BorderLayout.CENTER);
                detallesDialog.add(totalPanel, BorderLayout.SOUTH);

                detallesDialog.pack();
                detallesDialog.setLocationRelativeTo(this);
                detallesDialog.setVisible(true);
            }
        }
    }

    /**
     * Elimina la venta seleccionada en la tabla tras pedir confirmación.
     *
     * Al eliminar una venta, VentaController restaura automáticamente el stock
     * de todos los productos incluidos en ella, manteniendo la integridad
     * del inventario.
     */
    private void deleteVentaSeleccionada() {
        int filaSeleccionada = ventasTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer ventaId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            String ventaFecha = (String) tableModel.getValueAt(filaSeleccionada, 1);

            // Pedimos confirmación antes de eliminar para evitar borrados accidentales
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "¿Está seguro de que desea eliminar la venta de " + ventaFecha + "?",
                    "Confirmar borrado",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (respuesta == JOptionPane.YES_OPTION) {
                try {
                    ventaController.delete(ventaId);
                    // Refrescamos la tabla para reflejar el cambio
                    actualizarTable(busquedaField.getText());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                            "Error al eliminar la venta: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}