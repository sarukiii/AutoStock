package views.panels;

import controllers.ProveedorController;
import models.Proveedor;
import state.AppState;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class ProveedoresPanel extends JPanel {
    private final ProveedorController proveedorController;
    private final JTable proveedoresTable;
    private final DefaultTableModel tableModel;
    private final JTextField busquedaField;
    private JDialog proveedorDialog;
    private Proveedor provedorSeleccionado;
    private boolean isEditing = false;

    public ProveedoresPanel() {
        this.proveedorController = new ProveedorController();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel superior con boton y barra de búsqueda
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));

        // Panel de búsqueda
        JPanel busquedaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        busquedaField = new JTextField(20);
        busquedaField.addActionListener(e -> refreshTable(busquedaField.getText()));

        JButton buscarButton = new JButton("Buscar");
        buscarButton.addActionListener(e -> refreshTable(busquedaField.getText()));

        busquedaPanel.add(new JLabel("Buscar:"));
        busquedaPanel.add(busquedaField);
        busquedaPanel.add(buscarButton);

        // Añadir button
        JButton addButton = new JButton("Añadir nuevo proveedor");
        addButton.addActionListener(e -> showProveedorDialog(null));

        topPanel.add(busquedaPanel, BorderLayout.WEST);
        topPanel.add(addButton, BorderLayout.EAST);

        // Tabla
        String[] columnas = {"ID", "Nombre", "Contacto", "Teléfono", "Email", "Dirección"};
        tableModel = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        proveedoresTable = new JTable(tableModel);
        proveedoresTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        proveedoresTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        proveedoresTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        proveedoresTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        proveedoresTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        proveedoresTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        proveedoresTable.getColumnModel().getColumn(5).setPreferredWidth(200);

        // Menú contextual
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem editarItem = new JMenuItem("Editar");
        JMenuItem borrarItem = new JMenuItem("Borrar");
        JMenuItem viewProductosItem = new JMenuItem("Ver productos");

        editarItem.addActionListener(e -> editProveedorSeleccionado());
        borrarItem.addActionListener(e -> deleteProveedorSeleccionado());
        viewProductosItem.addActionListener(e -> viewProductosProvedor());

        popupMenu.add(editarItem);
        popupMenu.add(borrarItem);
        popupMenu.addSeparator();
        popupMenu.add(viewProductosItem);

        proveedoresTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = proveedoresTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < proveedoresTable.getRowCount()) {
                        proveedoresTable.setRowSelectionInterval(row, row);
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        // Componentes al panel principal
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(proveedoresTable), BorderLayout.CENTER);

        // Carga inicial
        refreshTable("");
    }

    private void refreshTable(String busquedaTerm) {
        tableModel.setRowCount(0);
        List<Proveedor> proveedores = proveedorController.findAll();

        for (Proveedor proveedor : proveedores) {
            if (busquedaTerm.isEmpty() ||
                    proveedor.getNombre().toLowerCase().contains(busquedaTerm.toLowerCase()) ||
                    proveedor.getContacto().toLowerCase().contains(busquedaTerm.toLowerCase())) {

                tableModel.addRow(new Object[]{
                        proveedor.getIdProveedor(),
                        proveedor.getNombre(),
                        proveedor.getContacto(),
                        proveedor.getTelefono(),
                        proveedor.getEmail(),
                        proveedor.getDireccion()
                });
            }
        }
    }

    private void showProveedorDialog(Proveedor proveedor) {
        isEditing = (proveedor != null);
        provedorSeleccionado = proveedor;

        proveedorDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEditing ? "Editar proveedor" : "Nuevo proveedor",
                true);
        proveedorDialog.setLayout(new BorderLayout(10, 10));

        // Formulario
        JPanel formularioPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Campos del formulario
        JTextField nombreField = new JTextField(20);
        JTextField contactoField = new JTextField(20);
        JTextField telefononField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextArea direccionArea = new JTextArea(3, 20);
        JScrollPane direccionScroll = new JScrollPane(direccionArea);

        // Edición
        if (isEditing) {
            nombreField.setText(proveedor.getNombre());
            contactoField.setText(proveedor.getContacto());
            telefononField.setText(proveedor.getTelefono());
            emailField.setText(proveedor.getEmail());
            direccionArea.setText(proveedor.getDireccion());
        }

        // Componentes del formulario
        gbc.gridx = 0; gbc.gridy = 0;
        formularioPanel.add(new JLabel("Nombre:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(nombreField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formularioPanel.add(new JLabel("Contacto:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(contactoField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formularioPanel.add(new JLabel("Telefono:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(telefononField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formularioPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formularioPanel.add(new JLabel("Dirección:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(direccionScroll, gbc);

        // Nota de campos obligatorios
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        formularioPanel.add(new JLabel("* Campos obligatorios"), gbc);

        // Botones panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton guardarButton = new JButton("Guardar");
        JButton cancelarButton = new JButton("Cancelar");

        guardarButton.addActionListener(e -> {
            try {
                // Validación datos de entrada
                String nombre = nombreField.getText().trim();
                String contacto = contactoField.getText().trim();
                String telefono = telefononField.getText().trim();
                String email = emailField.getText().trim();
                String direccion = direccionArea.getText().trim();

                if (nombre.isEmpty() || contacto.isEmpty() || telefono.isEmpty()) {
                    JOptionPane.showMessageDialog(proveedorDialog,
                    		"Por favor, rellena los campos requeridos",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Validación deel formato de email
                if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    JOptionPane.showMessageDialog(proveedorDialog,
                            "Por favor, introduce una dirección de email válida",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Crear o actualizar proveedor 
                Proveedor proveedorToSave = isEditing ? provedorSeleccionado : new Proveedor();
                proveedorToSave.setNombre(nombre);
                proveedorToSave.setContacto(contacto);
                proveedorToSave.setTelefono(telefono);
                proveedorToSave.setEmail(email);
                proveedorToSave.setDireccion(direccion);

                if (isEditing) {
                    proveedorController.update(proveedorToSave);
                } else {
                    proveedorController.create(proveedorToSave);
                }

                proveedorDialog.dispose();
                refreshTable(busquedaField.getText());

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(proveedorDialog,
                		"Error guardando proveedor: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelarButton.addActionListener(e -> proveedorDialog.dispose());

        buttonsPanel.add(guardarButton);
        buttonsPanel.add(cancelarButton);

        // Panel de diálogo
        proveedorDialog.add(formularioPanel, BorderLayout.CENTER);
        proveedorDialog.add(buttonsPanel, BorderLayout.SOUTH);

        // Mostrar diálogo
        proveedorDialog.pack();
        proveedorDialog.setLocationRelativeTo(this);
        proveedorDialog.setVisible(true);
    }

    private void editProveedorSeleccionado() {
        int filaSeleccionada = proveedoresTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer proveedorId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            Proveedor proveedor = proveedorController.findById(proveedorId);
            if (proveedor != null) {
                showProveedorDialog(proveedor);
            }
        }
    }

    private void deleteProveedorSeleccionado() {
        int filaSeleccionada = proveedoresTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer proveedorId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            String proveedorNombre = (String) tableModel.getValueAt(filaSeleccionada, 1);

            int respuesta = JOptionPane.showConfirmDialog(this,
            		"¿Estás seguro de que deseas eliminar el proveedor: " + proveedorNombre + "?\n" +
                            "Esto afectará a todos los productos asociados con este proveedor",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (respuesta == JOptionPane.YES_OPTION) {
                try {
                    proveedorController.delete(proveedorId);
                    refreshTable(busquedaField.getText());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                    		"Error eliminando proveedor: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void viewProductosProvedor() {
        int filaSeleccionada = proveedoresTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer proveedorId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            String proveedorNombre = (String) tableModel.getValueAt(filaSeleccionada, 1);

            // Crear y mostrar productos del proveedor.
            // Esto podría implementarse como una vista separada que muestre todos los productos
            // de este proveedor
        }	
    }
}