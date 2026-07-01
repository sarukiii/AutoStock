package views.panels;

import controllers.ClienteController;
import controllers.VentaController;
import models.Cliente;
import models.Venta;
import state.AppState;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class ClientesPanel extends JPanel {
    private final ClienteController clienteController;
    private final VentaController ventaController;
    private final JTable clientesTable;
    private final DefaultTableModel tableModel;
    private final JTextField buscarField;
    private JDialog clienteDialog;
    private Cliente clienteSeleccionado;
    private boolean isEditing = false;

    public ClientesPanel() {
        this.clienteController = new ClienteController();
        this.ventaController = new VentaController();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel superior con botón de buscar y agrega
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));

        // Panel de buscar
        JPanel buscarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buscarField = new JTextField(20);
        buscarField.addActionListener(e -> refreshTable(buscarField.getText()));

        JButton buscarButton = new JButton("Buscar");
        buscarButton.addActionListener(e -> refreshTable(buscarField.getText()));

        buscarPanel.add(new JLabel("Buscar:"));
        buscarPanel.add(buscarField);
        buscarPanel.add(buscarButton);

        // Añadir button
        JButton añadirButton = new JButton("Añadir nuevo cliente");
        añadirButton.addActionListener(e -> showClienteDialog(null));

        topPanel.add(buscarPanel, BorderLayout.WEST);
        topPanel.add(añadirButton, BorderLayout.EAST);

        // Tabla
        String[] columns = {"ID", "Nombre", "Teléfono", "Email", "Empresa", "Dirección"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        clientesTable = new JTable(tableModel);
        clientesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clientesTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        clientesTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        clientesTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        clientesTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        clientesTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        clientesTable.getColumnModel().getColumn(5).setPreferredWidth(200);

        // Menú contextual
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem editarItem = new JMenuItem("Editar");
        JMenuItem borrarItem = new JMenuItem("Borrar");
        JMenuItem viewVentasItem = new JMenuItem("Ver historial de ventas");
        JMenuItem newVentaItem = new JMenuItem("Nueva venta");

        editarItem.addActionListener(e -> editClienteSeleccionado());
        borrarItem.addActionListener(e -> deleteClienteSeleccionado());
        viewVentasItem.addActionListener(e -> viewVentasCliente());
        newVentaItem.addActionListener(e -> createNuevaVenta());

        popupMenu.add(editarItem);
        popupMenu.add(borrarItem);
        popupMenu.addSeparator();
        popupMenu.add(newVentaItem);
        popupMenu.add(viewVentasItem);

        clientesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = clientesTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < clientesTable.getRowCount()) {
                        clientesTable.setRowSelectionInterval(row, row);
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        // Componentes panel principal
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(clientesTable), BorderLayout.CENTER);

        // Carga inicial
        refreshTable("");
    }

    private void refreshTable(String buscarTerm) {
        tableModel.setRowCount(0);
        List<Cliente> clientes = clienteController.findAll();

        for (Cliente cliente : clientes) {
            if (buscarTerm.isEmpty() ||
                    cliente.getNombre().toLowerCase().contains(buscarTerm.toLowerCase()) ||
                    (cliente.getEmpresa() != null && cliente.getEmpresa().toLowerCase().contains(buscarTerm.toLowerCase()))) {

                tableModel.addRow(new Object[]{
                        cliente.getIdCliente(),
                        cliente.getNombre(),
                        cliente.getTelefono(),
                        cliente.getEmail(),
                        cliente.getEmpresa(),
                        cliente.getDireccion()
                });
            }
        }
    }

    private void showClienteDialog(Cliente cliente) {
        isEditing = (cliente != null);
        clienteSeleccionado = cliente;

        clienteDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEditing ? "Editar cliente" : "Nuevo Cliente",
                true);
        clienteDialog.setLayout(new BorderLayout(10, 10));

        // Panel formulario
        JPanel formularioPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Campos formulario
        JTextField nombreField = new JTextField(20);
        JTextField telefonoField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField empresaField = new JTextField(20);
        JTextArea direccionArea = new JTextArea(3, 20);
        JScrollPane direccionScroll = new JScrollPane(direccionArea);

        // Editar
        if (isEditing) {
            nombreField.setText(cliente.getNombre());
            telefonoField.setText(cliente.getTelefono());
            emailField.setText(cliente.getEmail());
            empresaField.setText(cliente.getEmpresa());
            direccionArea.setText(cliente.getDireccion());
        }

        // Componentes formulario
        gbc.gridx = 0; gbc.gridy = 0;
        formularioPanel.add(new JLabel("Nombre:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(nombreField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formularioPanel.add(new JLabel("Telefono:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(telefonoField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formularioPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formularioPanel.add(new JLabel("Empresa:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(empresaField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formularioPanel.add(new JLabel("Direccion:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(direccionScroll, gbc);

        // NOta campos obligatorios
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        formularioPanel.add(new JLabel("* Campos obligatorios"), gbc);

        // Buttons panel guardar
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton guardarButton = new JButton("Guardar");
        JButton cancelarButton = new JButton("Cancelar");

        guardarButton.addActionListener(e -> {
            try {
                // Validación entrada datos
                String nombre = nombreField.getText().trim();
                String telefono = telefonoField.getText().trim();
                String email = emailField.getText().trim();
                String empresa = empresaField.getText().trim();
                String direccion = direccionArea.getText().trim();

                if (nombre.isEmpty() || telefono.isEmpty()) {
                    JOptionPane.showMessageDialog(clienteDialog,
                    		"Por favor, complete todos los campos obligatorios",
                            "Error de valicación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Validación datos email
                if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    JOptionPane.showMessageDialog(clienteDialog,
                            "Por favor, introduce una dirección de correo electrónico válida",
                            "Validation Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Validación del formato del email
                Cliente clienteToSave = isEditing ? clienteSeleccionado : new Cliente();
                clienteToSave.setNombre(nombre);
                clienteToSave.setTelefono(telefono);
                clienteToSave.setEmail(email);
                clienteToSave.setEmpresa(empresa);
                clienteToSave.setDireccion(direccion);

                if (isEditing) {
                    clienteController.update(clienteToSave);
                } else {
                    clienteController.create(clienteToSave);
                }

                clienteDialog.dispose();
                refreshTable(buscarField.getText());

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(clienteDialog,
                		"Error guardando el cliente: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelarButton.addActionListener(e -> clienteDialog.dispose());

        buttonsPanel.add(guardarButton);
        buttonsPanel.add(cancelarButton);

        // Agregar paneles al dialog
        clienteDialog.add(formularioPanel, BorderLayout.CENTER);
        clienteDialog.add(buttonsPanel, BorderLayout.SOUTH);

        // Mostrar dialog
        clienteDialog.pack();
        clienteDialog.setLocationRelativeTo(this);
        clienteDialog.setVisible(true);
    }

    private void editClienteSeleccionado() {
        int filaSeleccionada = clientesTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer clienteId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            Cliente cliente = clienteController.findById(clienteId);
            if (cliente != null) {
                showClienteDialog(cliente);
            }
        }
    }

    private void deleteClienteSeleccionado() {
        int filaSeleccionada = clientesTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer clienteId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            String clienteNombre = (String) tableModel.getValueAt(filaSeleccionada, 1);

            List<Venta> ventasCliente = ventaController.findByClienteId(clienteId);
            if (!ventasCliente.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                		"No se puede eliminar un cliente con un historial de ventas existente",
                        "Error de borrado",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int respuesta = JOptionPane.showConfirmDialog(this,
                    "¿Está seguro de que desea eliminar el cliente " + clienteNombre + "?",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (respuesta == JOptionPane.YES_OPTION) {
                try {
                    clienteController.delete(clienteId);
                    refreshTable(buscarField.getText());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                    		"Error al eliminar cliente: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void viewVentasCliente() {
        int filasSeleccionadas = clientesTable.getSelectedRow();
        if (filasSeleccionadas >= 0) {
            Integer clienteId = (Integer) tableModel.getValueAt(filasSeleccionadas, 0);
            String clienteNombre = (String) tableModel.getValueAt(filasSeleccionadas, 1);

            // Mostrar el historial de ventas del cliente
            // Esto se implementará cuando se cree el panel del historial de ventas en un futuro
            JOptionPane.showMessageDialog(this,
            		"La función de historial de ventas estará disponible pronto",
                    "Llegará pronto",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void createNuevaVenta() {
        int filasSeleccionadas = clientesTable.getSelectedRow();
        if (filasSeleccionadas >= 0) {
            Integer clienteId = (Integer) tableModel.getValueAt(filasSeleccionadas, 0);

            // Abrir nuevo panel/diálogo de venta para este cliente
            // Esto se implementará cuando creemos el panel de nueva venta
            JOptionPane.showMessageDialog(this,
            		"La nueva función de venta estará disponible pronto.",
                    "Llegará pronto",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}