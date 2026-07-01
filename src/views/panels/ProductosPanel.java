package views.panels;

import controllers.ProductoController;
import controllers.ProveedorController;
import models.Producto;
import models.Proveedor;
import state.AppState;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.List;

public class ProductosPanel extends JPanel {
    private final ProductoController productoController;
    private final ProveedorController proveedorController;
    private final JTable productosTable;
    private final DefaultTableModel tableModel;
    private final JTextField buscarField;
    private JDialog productoDialog;
    private Producto productoSeleccionado;
    JPopupMenu popupMenu = new JPopupMenu();
    private boolean isEditing = false;

    public ProductosPanel() {
        this.productoController = new ProductoController();
        this.proveedorController = new ProveedorController();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel superior con el botón buscar y añadir
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        buscarField = new JTextField(20);
        buscarField.addActionListener(e -> actualizarTabla(buscarField.getText()));

        JButton buscarButton = new JButton("Buscar");
        buscarButton.addActionListener(e -> actualizarTabla(buscarField.getText()));

        JPanel buscarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buscarPanel.add(new JLabel("Buscar:"));
        buscarPanel.add(buscarField);
        buscarPanel.add(buscarButton);

        JButton añadirButton = new JButton("Añadir nuevo producto");
        añadirButton.addActionListener(e -> showProductoDialog(null));

        topPanel.add(buscarPanel, BorderLayout.WEST);
        if (AppState.getInstance().getLoggedInUser().getRol().getIdRol() == 1) {
            topPanel.add(añadirButton, BorderLayout.EAST);
        }

        // Tabla
        String[] columns = {"ID", "Nombre", "Descripción", "Precio", "Stock", "Proveedor"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        productosTable = new JTable(tableModel);
        productosTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productosTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        productosTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        productosTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        productosTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        productosTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        productosTable.getColumnModel().getColumn(5).setPreferredWidth(150);

        if (AppState.getInstance().getLoggedInUser().getRol().getIdRol() == 1) {
            JMenuItem editarItem = new JMenuItem("Editar");
            JMenuItem borrarItem = new JMenuItem("Borrar");

            editarItem.addActionListener(e -> editProductoSeleccionado());
            borrarItem.addActionListener(e -> deleteProductoSeleccionado());

            popupMenu.add(editarItem);
            popupMenu.add(borrarItem);
        }

        productosTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {  // presionar click raton
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            private void showPopup(MouseEvent e) {
                int row = productosTable.rowAtPoint(e.getPoint());
                if (row >= 0 && row < productosTable.getRowCount()) {
                    productosTable.setRowSelectionInterval(row, row);
                    // Mostrar solo el menu contectual si tiene elementos de menú
                    if (popupMenu.getComponentCount() > 0) {
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        // Menú contextual
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem editarItem = new JMenuItem("Editar");
        JMenuItem borrarItem = new JMenuItem("Borrar");

        editarItem.addActionListener(e -> editProductoSeleccionado());
        borrarItem.addActionListener(e -> deleteProductoSeleccionado());

        popupMenu.add(editarItem);
        popupMenu.add(borrarItem);

        productosTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = productosTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < productosTable.getRowCount()) {
                        productosTable.setRowSelectionInterval(row, row);
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        // Componentes panel principal
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(productosTable), BorderLayout.CENTER);

        // Carga inicial de datos
        actualizarTabla("");
    }

    private void actualizarTabla(String buscarTerm) {
        tableModel.setRowCount(0);
        List<Producto> productos = productoController.findAll(); 

        for (Producto producto : productos) {
            if (buscarTerm.isEmpty() ||
                    producto.getNombre().toLowerCase().contains(buscarTerm.toLowerCase()) ||
                    producto.getDescripcion().toLowerCase().contains(buscarTerm.toLowerCase())) {

                tableModel.addRow(new Object[]{
                        producto.getIdProducto(),
                        producto.getNombre(),
                        producto.getDescripcion(),
                        producto.getPrecio(),
                        producto.getCantidadDisponible(),
                        producto.getProveedor().getNombre()
                });
            }
        }
    }

    private void showProductoDialog(Producto producto) {
        isEditing = (producto != null);
        productoSeleccionado = producto;

        productoDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEditing ? "Editar Producto" : "Nuevo producto",
                true);
        productoDialog.setLayout(new BorderLayout(10, 10));

        // Formulario
        JPanel formularioPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Campos del formulario
        JTextField nombreField = new JTextField(20);
        JTextArea descripcionArea = new JTextArea(3, 20);
        JScrollPane descScrollPane = new JScrollPane(descripcionArea);
        JTextField precioField = new JTextField(10);
        JSpinner stockSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        JComboBox<Proveedor> proveedorCombo = new JComboBox<>();

        // Listado proveedores
        List<Proveedor> proveedores = proveedorController.findAll();
        for (Proveedor proveedor : proveedores) {
            proveedorCombo.addItem(proveedor);
        }

        // Campos para rellenar
        if (isEditing) {
            nombreField.setText(producto.getNombre());
            descripcionArea.setText(producto.getDescripcion());
            precioField.setText(producto.getPrecio().toString());
            stockSpinner.setValue(producto.getCantidadDisponible());
            for (int i = 0; i < proveedorCombo.getItemCount(); i++) {
                if (proveedorCombo.getItemAt(i).getIdProveedor().equals(producto.getProveedor().getIdProveedor())) {
                    proveedorCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Componentes formulario
        gbc.gridx = 0; gbc.gridy = 0;
        formularioPanel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(nombreField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formularioPanel.add(new JLabel("Descripción:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(descScrollPane, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formularioPanel.add(new JLabel("Precio:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(precioField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formularioPanel.add(new JLabel("Stock:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(stockSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formularioPanel.add(new JLabel("Proveedor:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(proveedorCombo, gbc);

        // Botones panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton guardarButton = new JButton("Guardar");
        JButton cancelarButton = new JButton("Cancelar");

        guardarButton.addActionListener(e -> {
            try {
                // Validación de datos
                String nombre = nombreField.getText().trim();
                String descripcion = descripcionArea.getText().trim();
                String precioText = precioField.getText().trim();
                int stock = (Integer) stockSpinner.getValue();
                Proveedor proveedor = (Proveedor) proveedorCombo.getSelectedItem();

                if (nombre.isEmpty() || descripcion.isEmpty() || precioText.isEmpty() || proveedor == null) {
                    JOptionPane.showMessageDialog(productoDialog,
                    		"Por favor, complete todos los campos",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                BigDecimal precio;
                try {
                    precio = new BigDecimal(precioText);
                    if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(productoDialog,
                    		"Por favor, introduce un precio válido",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Crear o actualizar productos
                Producto productoToSave = isEditing ? productoSeleccionado : new Producto();
                productoToSave.setNombre(nombre);
                productoToSave.setDescripcion(descripcion);
                productoToSave.setPrecio(precio);
                productoToSave.setCantidadDisponible(stock);
                productoToSave.setProveedor(proveedor);

                if (isEditing) {
                    productoController.update(productoToSave);
                } else {
                    productoController.create(productoToSave);
                }

                productoDialog.dispose();
                actualizarTabla(buscarField.getText());

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(productoDialog,
                		"Error guardando el producto: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelarButton.addActionListener(e -> productoDialog.dispose());

        buttonsPanel.add(guardarButton);
        buttonsPanel.add(cancelarButton);

        // Panel diálogo
        productoDialog.add(formularioPanel, BorderLayout.CENTER);
        productoDialog.add(buttonsPanel, BorderLayout.SOUTH);

        // Mostrar diálogo
        productoDialog.pack();
        productoDialog.setLocationRelativeTo(this);
        productoDialog.setVisible(true);
    }

    private void editProductoSeleccionado() {
        int filaSeleccionada = productosTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer productoId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            Producto producto = productoController.findById(productoId);
            if (producto != null) {
                showProductoDialog(producto);
            }
        }
    }

    private void deleteProductoSeleccionado() {
        int filaSeleccionada = productosTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer productoId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            String productoNombre = (String) tableModel.getValueAt(filaSeleccionada, 1);

            int respuesta = JOptionPane.showConfirmDialog(this,
                    "¿Estás seguro de que deseas eliminar el producto: " + productoNombre + "?",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (respuesta == JOptionPane.YES_OPTION) {
                try {
                    productoController.delete(productoId);
                    actualizarTabla(buscarField.getText());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                    		"Error borrando el producto: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}