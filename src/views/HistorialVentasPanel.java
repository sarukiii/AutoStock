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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HistorialVentasPanel extends JPanel {
    private final VentaController ventaController;
    private final JTable ventasTable;
    private final DefaultTableModel tableModel;
    private final JTextField busquedaField;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public HistorialVentasPanel() {
        this.ventaController = new VentaController();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Crea el panel superior con búsqueda y filtros
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        busquedaField = new JTextField(20);
        busquedaField.addActionListener(e -> actualizarTable(busquedaField.getText()));

        JButton buscarButton = new JButton("Buscar");
        buscarButton.addActionListener(e -> actualizarTable(busquedaField.getText()));

        JPanel buscarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buscarPanel.add(new JLabel("Buscar:"));
        buscarPanel.add(busquedaField);
        buscarPanel.add(buscarButton);

        // Filtros de rango de fechas
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        // En este punto se podrían agregar selectores de fechas

        topPanel.add(buscarPanel, BorderLayout.WEST);
        topPanel.add(filterPanel, BorderLayout.EAST);

        // Tabla
        String[] columnas = {"ID", "Fecha", "Cliente", "Usuario", "Total", "Items"};
        tableModel = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        ventasTable = new JTable(tableModel);
        ventasTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ventasTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        ventasTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        ventasTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        ventasTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        ventasTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        ventasTable.getColumnModel().getColumn(5).setPreferredWidth(80);

        // Listener de doble clic para obtener más detalles
        ventasTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showDetalleVenta();
                }
            }
        });

        // Menú contextual
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem viewDetallesItem = new JMenuItem("Ver detalles");
        JMenuItem deleteItem = new JMenuItem("Borrar");

        viewDetallesItem.addActionListener(e -> showDetalleVenta());
        deleteItem.addActionListener(e -> deleteVentaSeleccionada());

        popupMenu.add(viewDetallesItem);
        popupMenu.add(deleteItem);

        ventasTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = ventasTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < ventasTable.getRowCount()) {
                        ventasTable.setRowSelectionInterval(row, row);
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        // Componentes del panel principal
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(ventasTable), BorderLayout.CENTER);

        // Carga inicial de datos
        actualizarTable("");
    }

    private void actualizarTable(String busquedaTerm) {
        tableModel.setRowCount(0);
        List<Venta> ventas = ventaController.findAll();

        for (Venta venta : ventas) {
            String clienteNombre = venta.getCliente().getNombre().toLowerCase();
            String usuarioNombre = venta.getUsuario().getNombre().toLowerCase();
            busquedaTerm = busquedaTerm.toLowerCase();

            if (busquedaTerm.isEmpty() ||
                    clienteNombre.contains(busquedaTerm) ||
                    usuarioNombre.contains(busquedaTerm)) {

                tableModel.addRow(new Object[]{
                        venta.getIdVenta(),
                        formatter.format(venta.getFecha()),
                        venta.getCliente().getNombre(),
                        venta.getUsuario().getNombre(),
                        String.format("€%.2f", venta.getTotal()),
                        venta.getDetalles().size()
                });
            }
        }
    }

    private void showDetalleVenta() {
        int filaSeleccionada = ventasTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer ventaId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            Venta venta = ventaController.findById(ventaId);

            if (venta != null) {
                JDialog detallesDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                        "Detalles venta", true);
                detallesDialog.setLayout(new BorderLayout(10, 10));

                // Panel de detalles
                JPanel detallesPanel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(5, 5, 5, 5);

                // Información de la venta
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

                // Items tabla
                String[] columns = {"Producto", "Cantidad", "Precio/unidad", "Subtotal"};
                DefaultTableModel itemsModel = new DefaultTableModel(columns, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };

                for (DetalleVenta detalle : venta.getDetalles()) {
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

                // Panel importe total
                JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                totalPanel.add(new JLabel("Total: "));
                JLabel totalLabel = new JLabel(String.format("€%.2f", venta.getTotal()));
                totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD));
                totalPanel.add(totalLabel);

                // Componetes cuadro de diálogo
                detallesDialog.add(detallesPanel, BorderLayout.NORTH);
                detallesDialog.add(scrollPane, BorderLayout.CENTER);
                detallesDialog.add(totalPanel, BorderLayout.SOUTH);

                detallesDialog.pack();
                detallesDialog.setLocationRelativeTo(this);
                detallesDialog.setVisible(true);
            }
        }
    }

    private void deleteVentaSeleccionada() {
        int selectedRow = ventasTable.getSelectedRow();
        if (selectedRow >= 0) {
            Integer ventaId = (Integer) tableModel.getValueAt(selectedRow, 0);
            String ventaDate = (String) tableModel.getValueAt(selectedRow, 1);

            int response = JOptionPane.showConfirmDialog(this,
                    "¿Está seguro de que desea eliminar la venta de " + ventaDate + "?",
                    "Confirmar borrado",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (response == JOptionPane.YES_OPTION) {
                try {
                    ventaController.delete(ventaId);
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