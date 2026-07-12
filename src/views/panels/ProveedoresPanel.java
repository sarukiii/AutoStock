package views.panels;

import controllers.ProveedorController;
import models.Proveedor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel de gestión de proveedores.
 *
 * Muestra todos los proveedores en una tabla con búsqueda por nombre o contacto.
 * Permite añadir, editar y eliminar proveedores mediante un botón y un menú
 * contextual (clic derecho). Este panel solo es accesible para administradores.
 *
 * Extiende JPanel para poder incrustarse en el área de contenido de MainView.
 */
public class ProveedoresPanel extends JPanel {

    // Controlador para acceder a la lógica de negocio de proveedores
    private final ProveedorController proveedorController;
    // Tabla que muestra el listado de proveedores
    private final JTable proveedoresTable;
    // Modelo de datos de la tabla (gestiona filas y columnas)
    private final DefaultTableModel tableModel;
    // Campo de texto para filtrar proveedores por nombre o contacto
    private final JTextField busquedaField;
    // Diálogo reutilizable para crear y editar proveedores
    private JDialog proveedorDialog;
    // Proveedor seleccionado en la tabla para editar o eliminar
    private Proveedor proveedorSeleccionado;
    // Indica si el diálogo está en modo edición (true) o creación (false)
    private boolean isEditing = false;

    public ProveedoresPanel() {
        this.proveedorController = new ProveedorController();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel superior: búsqueda y botón de añadir ---
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));

        JPanel busquedaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        busquedaField = new JTextField(20);
        // Permitimos buscar pulsando Intro en el campo de búsqueda
        busquedaField.addActionListener(e -> refreshTable(busquedaField.getText()));

        JButton buscarButton = new JButton("Buscar");
        buscarButton.addActionListener(e -> refreshTable(busquedaField.getText()));

        busquedaPanel.add(new JLabel("Buscar:"));
        busquedaPanel.add(busquedaField);
        busquedaPanel.add(buscarButton);

        JButton addButton = new JButton("Añadir nuevo proveedor");
        addButton.addActionListener(e -> showProveedorDialog(null));

        topPanel.add(busquedaPanel, BorderLayout.WEST);
        topPanel.add(addButton, BorderLayout.EAST);

        // --- Tabla de proveedores ---
        // isCellEditable devuelve false para que el usuario no pueda editar
        // las celdas directamente; la edición se hace a través del diálogo
        String[] columnas = {"ID", "Nombre", "Contacto", "Teléfono", "Email", "Dirección"};
        tableModel = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };

        proveedoresTable = new JTable(tableModel);
        // Solo permitimos seleccionar una fila a la vez
        proveedoresTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Ajustamos el ancho preferido de cada columna
        proveedoresTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        proveedoresTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        proveedoresTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        proveedoresTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        proveedoresTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        proveedoresTable.getColumnModel().getColumn(5).setPreferredWidth(200);

        // --- Menú contextual con opciones sobre el proveedor seleccionado ---
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem editarItem = new JMenuItem("Editar");
        JMenuItem borrarItem = new JMenuItem("Borrar");
        JMenuItem viewProductosItem = new JMenuItem("Ver productos");

        editarItem.addActionListener(e -> editProveedorSeleccionado());
        borrarItem.addActionListener(e -> deleteProveedorSeleccionado());
        viewProductosItem.addActionListener(e -> viewProductosProveedor());

        popupMenu.add(editarItem);
        popupMenu.add(borrarItem);
        // Separador visual entre opciones de gestión y opciones de productos
        popupMenu.addSeparator();
        popupMenu.add(viewProductosItem);

        // Listener del ratón para mostrar el menú contextual al hacer clic derecho.
        // Se comprueban tanto mousePressed como mouseReleased porque el evento
        // de menú contextual varía según el sistema operativo (Windows vs Mac/Linux).
        proveedoresTable.addMouseListener(new MouseAdapter() {
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
                int row = proveedoresTable.rowAtPoint(e.getPoint());
                if (row >= 0 && row < proveedoresTable.getRowCount()) {
                    // Seleccionamos la fila sobre la que se hizo clic derecho
                    proveedoresTable.setRowSelectionInterval(row, row);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        // --- Ensamblaje del panel principal ---
        add(topPanel, BorderLayout.NORTH);
        // Envolvemos la tabla en un JScrollPane para que tenga barra de desplazamiento
        add(new JScrollPane(proveedoresTable), BorderLayout.CENTER);

        // Cargamos todos los proveedores al inicializar el panel
        refreshTable("");
    }

    /**
     * Actualiza el contenido de la tabla filtrando por el término de búsqueda.
     *
     * Carga todos los proveedores de la base de datos y filtra en memoria
     * los que contienen el término en el nombre o el contacto.
     * Si el término está vacío, muestra todos los proveedores.
     *
     * @param busquedaTerm texto a buscar (puede estar vacío para mostrar todos)
     */
    private void refreshTable(String busquedaTerm) {
        // Vaciamos la tabla antes de rellenarla con los nuevos datos
        tableModel.setRowCount(0);
        List<Proveedor> proveedores = proveedorController.findAll();

        for (Proveedor proveedor : proveedores) {
            // Filtramos por nombre o contacto si hay término de búsqueda
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

    /**
     * Muestra el diálogo de creación o edición de un proveedor.
     *
     * Si se pasa un proveedor existente, el diálogo se abre en modo edición
     * con los campos pre-rellenados. Si se pasa null, se abre en modo creación.
     * Los campos marcados con * son obligatorios (nombre, contacto y teléfono).
     *
     * @param proveedor proveedor a editar, o null para crear uno nuevo
     */
    private void showProveedorDialog(Proveedor proveedor) {
        isEditing = (proveedor != null);
        proveedorSeleccionado = proveedor;

        // Creamos un diálogo modal para que el usuario no pueda interactuar
        // con la tabla mientras está editando un proveedor
        proveedorDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEditing ? "Editar proveedor" : "Nuevo proveedor",
                true);
        proveedorDialog.setLayout(new BorderLayout(10, 10));

        // --- Formulario del proveedor ---
        JPanel formularioPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Campos del formulario
        JTextField nombreField = new JTextField(20);
        JTextField contactoField = new JTextField(20);
        JTextField telefonoField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextArea direccionArea = new JTextArea(3, 20);
        JScrollPane direccionScroll = new JScrollPane(direccionArea);

        // Si estamos editando, pre-rellenamos los campos con los datos actuales
        if (isEditing) {
            nombreField.setText(proveedor.getNombre());
            contactoField.setText(proveedor.getContacto());
            telefonoField.setText(proveedor.getTelefono());
            emailField.setText(proveedor.getEmail());
            direccionArea.setText(proveedor.getDireccion());
        }

        // Añadimos los campos al formulario con su etiqueta correspondiente
        // El asterisco (*) indica que el campo es obligatorio
        gbc.gridx = 0; gbc.gridy = 0;
        formularioPanel.add(new JLabel("Nombre:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(nombreField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formularioPanel.add(new JLabel("Contacto:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(contactoField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formularioPanel.add(new JLabel("Teléfono:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(telefonoField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formularioPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(emailField, gbc);

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
                String contacto = contactoField.getText().trim();
                String telefono = telefonoField.getText().trim();
                String email = emailField.getText().trim();
                String direccion = direccionArea.getText().trim();

                // Validamos que los campos obligatorios estén rellenos
                if (nombre.isEmpty() || contacto.isEmpty() || telefono.isEmpty()) {
                    JOptionPane.showMessageDialog(proveedorDialog,
                            "Por favor, rellena los campos requeridos",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Validamos el formato del email si se ha introducido uno
                if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    JOptionPane.showMessageDialog(proveedorDialog,
                            "Por favor, introduce una dirección de email válida",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Creamos o actualizamos el proveedor según el modo del diálogo
                Proveedor proveedorToSave = isEditing ? proveedorSeleccionado : new Proveedor();
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

                // Cerramos el diálogo y refrescamos la tabla
                proveedorDialog.dispose();
                refreshTable(busquedaField.getText());

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(proveedorDialog,
                        "Error guardando proveedor: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // El botón cancelar simplemente cierra el diálogo sin guardar cambios
        cancelarButton.addActionListener(e -> proveedorDialog.dispose());

        buttonsPanel.add(guardarButton);
        buttonsPanel.add(cancelarButton);

        // Ensamblamos el diálogo
        proveedorDialog.add(formularioPanel, BorderLayout.CENTER);
        proveedorDialog.add(buttonsPanel, BorderLayout.SOUTH);

        // pack() ajusta el tamaño del diálogo al contenido
        proveedorDialog.pack();
        proveedorDialog.setLocationRelativeTo(this);
        proveedorDialog.setVisible(true);
    }

    /**
     * Abre el diálogo de edición con el proveedor seleccionado en la tabla.
     * Si no hay ninguna fila seleccionada, no hace nada.
     */
    private void editProveedorSeleccionado() {
        int filaSeleccionada = proveedoresTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            // Obtenemos el ID del proveedor de la primera columna de la fila seleccionada
            Integer proveedorId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            Proveedor proveedor = proveedorController.findById(proveedorId);
            if (proveedor != null) {
                showProveedorDialog(proveedor);
            }
        }
    }

    /**
     * Elimina el proveedor seleccionado en la tabla tras pedir confirmación.
     *
     * Avisa al usuario de que la eliminación afectará a los productos
     * asociados a este proveedor antes de confirmar.
     */
    private void deleteProveedorSeleccionado() {
        int filaSeleccionada = proveedoresTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer proveedorId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            String proveedorNombre = (String) tableModel.getValueAt(filaSeleccionada, 1);

            // Advertimos que la eliminación puede afectar a los productos asociados
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "¿Estás seguro de que deseas eliminar el proveedor: " + proveedorNombre + "?\n" +
                    "Esto afectará a todos los productos asociados con este proveedor",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (respuesta == JOptionPane.YES_OPTION) {
                try {
                    proveedorController.delete(proveedorId);
                    // Refrescamos la tabla para reflejar el cambio
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

    /**
     * Muestra los productos asociados al proveedor seleccionado.
     *
     * Funcionalidad pendiente de implementar: actualmente muestra un mensaje
     * informativo. En una versión futura abrirá un panel o diálogo con
     * todos los productos de este proveedor.
     */
    private void viewProductosProveedor() {
        int filaSeleccionada = proveedoresTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            JOptionPane.showMessageDialog(this,
                    "La función de ver productos por proveedor estará disponible pronto.",
                    "Próximamente",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}