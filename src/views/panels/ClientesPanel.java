package views.panels;

import controllers.ClienteController;
import controllers.VentaController;
import models.Cliente;
import models.Venta;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel de gestión de clientes.
 *
 * Muestra todos los clientes en una tabla con búsqueda por nombre o empresa.
 * Permite añadir, editar y eliminar clientes mediante un botón y un menú
 * contextual (clic derecho). Este panel solo es accesible para administradores.
 *
 * No se puede eliminar un cliente que tenga ventas asociadas, para mantener
 * la integridad del historial de ventas.
 *
 * Extiende JPanel para poder incrustarse en el área de contenido de MainView.
 */
public class ClientesPanel extends JPanel {

    // Controladores para acceder a la lógica de negocio
    private final ClienteController clienteController;
    // VentaController se usa para verificar si el cliente tiene ventas antes de eliminarlo
    private final VentaController ventaController;
    // Tabla que muestra el listado de clientes
    private final JTable clientesTable;
    // Modelo de datos de la tabla (gestiona filas y columnas)
    private final DefaultTableModel tableModel;
    // Campo de texto para filtrar clientes por nombre o empresa
    private final JTextField buscarField;
    // Diálogo reutilizable para crear y editar clientes
    private JDialog clienteDialog;
    // Cliente seleccionado en la tabla para editar o eliminar
    private Cliente clienteSeleccionado;
    // Indica si el diálogo está en modo edición (true) o creación (false)
    private boolean isEditing = false;

    public ClientesPanel() {
        this.clienteController = new ClienteController();
        this.ventaController = new VentaController();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel superior: búsqueda y botón de añadir ---
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));

        JPanel buscarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buscarField = new JTextField(20);
        // Permitimos buscar pulsando Intro en el campo de búsqueda
        buscarField.addActionListener(e -> refreshTable(buscarField.getText()));

        JButton buscarButton = new JButton("Buscar");
        buscarButton.addActionListener(e -> refreshTable(buscarField.getText()));

        buscarPanel.add(new JLabel("Buscar:"));
        buscarPanel.add(buscarField);
        buscarPanel.add(buscarButton);

        JButton añadirButton = new JButton("Añadir nuevo cliente");
        añadirButton.addActionListener(e -> showClienteDialog(null));

        topPanel.add(buscarPanel, BorderLayout.WEST);
        topPanel.add(añadirButton, BorderLayout.EAST);

        // --- Tabla de clientes ---
        // isCellEditable devuelve false para que el usuario no pueda editar
        // las celdas directamente; la edición se hace a través del diálogo
        String[] columns = {"ID", "Nombre", "Teléfono", "Email", "Empresa", "Dirección"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        clientesTable = new JTable(tableModel);
        // Solo permitimos seleccionar una fila a la vez
        clientesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Ajustamos el ancho preferido de cada columna
        clientesTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        clientesTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        clientesTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        clientesTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        clientesTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        clientesTable.getColumnModel().getColumn(5).setPreferredWidth(200);

        // --- Menú contextual con opciones sobre el cliente seleccionado ---
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
        // Separador visual entre opciones de gestión y opciones de ventas
        popupMenu.addSeparator();
        popupMenu.add(newVentaItem);
        popupMenu.add(viewVentasItem);

        // Listener del ratón para mostrar el menú contextual al hacer clic derecho.
        // Se comprueban tanto mousePressed como mouseReleased porque el evento
        // de menú contextual varía según el sistema operativo (Windows vs Mac/Linux).
        clientesTable.addMouseListener(new MouseAdapter() {
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
                int row = clientesTable.rowAtPoint(e.getPoint());
                if (row >= 0 && row < clientesTable.getRowCount()) {
                    // Seleccionamos la fila sobre la que se hizo clic derecho
                    clientesTable.setRowSelectionInterval(row, row);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        // --- Ensamblaje del panel principal ---
        add(topPanel, BorderLayout.NORTH);
        // Envolvemos la tabla en un JScrollPane para que tenga barra de desplazamiento
        add(new JScrollPane(clientesTable), BorderLayout.CENTER);

        // Cargamos todos los clientes al inicializar el panel
        refreshTable("");
    }

    /**
     * Actualiza el contenido de la tabla filtrando por el término de búsqueda.
     *
     * Carga todos los clientes de la base de datos y filtra en memoria
     * los que contienen el término en el nombre o la empresa.
     * Si el término está vacío, muestra todos los clientes.
     *
     * @param buscarTerm texto a buscar (puede estar vacío para mostrar todos)
     */
    private void refreshTable(String buscarTerm) {
        // Vaciamos la tabla antes de rellenarla con los nuevos datos
        tableModel.setRowCount(0);
        List<Cliente> clientes = clienteController.findAll();

        for (Cliente cliente : clientes) {
            // Filtramos por nombre o empresa si hay término de búsqueda.
            // Comprobamos null en empresa porque es un campo opcional.
            if (buscarTerm.isEmpty() ||
                    cliente.getNombre().toLowerCase().contains(buscarTerm.toLowerCase()) ||
                    (cliente.getEmpresa() != null &&
                            cliente.getEmpresa().toLowerCase().contains(buscarTerm.toLowerCase()))) {

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

    /**
     * Muestra el diálogo de creación o edición de un cliente.
     *
     * Si se pasa un cliente existente, el diálogo se abre en modo edición
     * con los campos pre-rellenados. Si se pasa null, se abre en modo creación.
     * Los campos marcados con * son obligatorios (nombre y teléfono).
     *
     * @param cliente cliente a editar, o null para crear uno nuevo
     */
    private void showClienteDialog(Cliente cliente) {
        isEditing = (cliente != null);
        clienteSeleccionado = cliente;

        // Creamos un diálogo modal para que el usuario no pueda interactuar
        // con la tabla mientras está editando un cliente
        clienteDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEditing ? "Editar cliente" : "Nuevo Cliente",
                true);
        clienteDialog.setLayout(new BorderLayout(10, 10));

        // --- Formulario del cliente ---
        JPanel formularioPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Campos del formulario
        JTextField nombreField = new JTextField(20);
        JTextField telefonoField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField empresaField = new JTextField(20);
        JTextArea direccionArea = new JTextArea(3, 20);
        JScrollPane direccionScroll = new JScrollPane(direccionArea);

        // Si estamos editando, pre-rellenamos los campos con los datos actuales
        if (isEditing) {
            nombreField.setText(cliente.getNombre());
            telefonoField.setText(cliente.getTelefono());
            emailField.setText(cliente.getEmail());
            empresaField.setText(cliente.getEmpresa());
            direccionArea.setText(cliente.getDireccion());
        }

        // Añadimos los campos al formulario con su etiqueta correspondiente
        // El asterisco (*) indica que el campo es obligatorio
        gbc.gridx = 0; gbc.gridy = 0;
        formularioPanel.add(new JLabel("Nombre:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(nombreField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formularioPanel.add(new JLabel("Teléfono:*"), gbc);
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
        formularioPanel.add(new JLabel("Dirección:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(direccionScroll, gbc);

        // Nota informativa sobre los campos obligatorios
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        formularioPanel.add(new JLabel("* Campos obligatorios"), gbc);

        // --- Botones del diálogo ---
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton guardarButton = new JButton("Guardar");
        JButton cancelarButton = new JButton("Cancelar");

        guardarButton.addActionListener(e -> {
            try {
                // Recogemos los valores del formulario
                String nombre = nombreField.getText().trim();
                String telefono = telefonoField.getText().trim();
                String email = emailField.getText().trim();
                String empresa = empresaField.getText().trim();
                String direccion = direccionArea.getText().trim();

                // Validamos que los campos obligatorios estén rellenos
                if (nombre.isEmpty() || telefono.isEmpty()) {
                    JOptionPane.showMessageDialog(clienteDialog,
                            "Por favor, complete todos los campos obligatorios",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Validamos el formato del email si se ha introducido uno
                if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    JOptionPane.showMessageDialog(clienteDialog,
                            "Por favor, introduce una dirección de correo electrónico válida",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Creamos o actualizamos el cliente según el modo del diálogo
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

                // Cerramos el diálogo y refrescamos la tabla
                clienteDialog.dispose();
                refreshTable(buscarField.getText());

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(clienteDialog,
                        "Error guardando el cliente: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // El botón cancelar simplemente cierra el diálogo sin guardar cambios
        cancelarButton.addActionListener(e -> clienteDialog.dispose());

        buttonsPanel.add(guardarButton);
        buttonsPanel.add(cancelarButton);

        // Ensamblamos el diálogo
        clienteDialog.add(formularioPanel, BorderLayout.CENTER);
        clienteDialog.add(buttonsPanel, BorderLayout.SOUTH);

        // pack() ajusta el tamaño del diálogo al contenido
        clienteDialog.pack();
        clienteDialog.setLocationRelativeTo(this);
        clienteDialog.setVisible(true);
    }

    /**
     * Abre el diálogo de edición con el cliente seleccionado en la tabla.
     * Si no hay ninguna fila seleccionada, no hace nada.
     */
    private void editClienteSeleccionado() {
        int filaSeleccionada = clientesTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            // Obtenemos el ID del cliente de la primera columna de la fila seleccionada
            Integer clienteId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            Cliente cliente = clienteController.findById(clienteId);
            if (cliente != null) {
                showClienteDialog(cliente);
            }
        }
    }

    /**
     * Elimina el cliente seleccionado en la tabla tras verificar que no tiene
     * ventas asociadas y pedir confirmación al usuario.
     *
     * No se permite eliminar un cliente con historial de ventas para preservar
     * la integridad de los datos históricos.
     */
    private void deleteClienteSeleccionado() {
        int filaSeleccionada = clientesTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer clienteId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            String clienteNombre = (String) tableModel.getValueAt(filaSeleccionada, 1);

            // Verificamos si el cliente tiene ventas antes de intentar eliminarlo.
            // Si tiene ventas, mostramos un error y no permitimos la eliminación.
            List<Venta> ventasCliente = ventaController.findByClienteId(clienteId);
            if (!ventasCliente.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No se puede eliminar un cliente con un historial de ventas existente",
                        "Error de borrado",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Pedimos confirmación antes de eliminar para evitar borrados accidentales
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "¿Está seguro de que desea eliminar el cliente " + clienteNombre + "?",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (respuesta == JOptionPane.YES_OPTION) {
                try {
                    clienteController.delete(clienteId);
                    // Refrescamos la tabla para reflejar el cambio
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

    /**
     * Muestra el historial de ventas del cliente seleccionado.
     *
     * Funcionalidad pendiente de implementar: actualmente muestra un mensaje
     * informativo. En una versión futura abrirá el panel de historial de ventas
     * filtrado por este cliente.
     */
    private void viewVentasCliente() {
        int filaSeleccionada = clientesTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            JOptionPane.showMessageDialog(this,
                    "La función de historial de ventas estará disponible pronto",
                    "Próximamente",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Abre el formulario de nueva venta con el cliente seleccionado pre-cargado.
     *
     * Funcionalidad pendiente de implementar: actualmente muestra un mensaje
     * informativo. En una versión futura abrirá el panel de nueva venta
     * con este cliente ya seleccionado.
     */
    private void createNuevaVenta() {
        int filaSeleccionada = clientesTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            JOptionPane.showMessageDialog(this,
                    "La nueva función de venta estará disponible pronto.",
                    "Próximamente",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}