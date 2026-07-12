package views.panels;

import controllers.UsuarioController;
import models.Usuario;
import models.Rol;
import state.AppState;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel de gestión de usuarios del sistema.
 *
 * Muestra todos los usuarios en una tabla con búsqueda por nombre o email.
 * Permite añadir, editar y eliminar usuarios, así como restablecer contraseñas,
 * mediante un botón y un menú contextual (clic derecho).
 * Este panel solo es accesible para administradores.
 *
 * Restricciones de seguridad implementadas:
 * - Un administrador no puede eliminarse a sí mismo.
 * - No se puede eliminar a otro administrador desde el menú contextual.
 *
 * Extiende JPanel para poder incrustarse en el área de contenido de MainView.
 */
public class UsuariosPanel extends JPanel {

    // Controlador para acceder a la lógica de negocio de usuarios
    private final UsuarioController usuarioController;
    // Tabla que muestra el listado de usuarios
    private final JTable usuariosTable;
    // Modelo de datos de la tabla (gestiona filas y columnas)
    private final DefaultTableModel tableModel;
    // Campo de texto para filtrar usuarios por nombre o email
    private final JTextField busquedaField;
    // Diálogo reutilizable para crear y editar usuarios
    private JDialog usuarioDialog;
    // Usuario seleccionado en la tabla para editar o eliminar
    private Usuario usuarioSeleccionado;
    // Indica si el diálogo está en modo edición (true) o creación (false)
    private boolean isEditing = false;

    public UsuariosPanel() {
        this.usuarioController = new UsuarioController();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel superior: búsqueda y botón de añadir ---
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));

        JPanel busquedaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        busquedaField = new JTextField(20);
        // Permitimos buscar pulsando Intro en el campo de búsqueda
        busquedaField.addActionListener(e -> actualizarTable(busquedaField.getText()));

        JButton busquedaButton = new JButton("Buscar");
        busquedaButton.addActionListener(e -> actualizarTable(busquedaField.getText()));

        busquedaPanel.add(new JLabel("Buscar:"));
        busquedaPanel.add(busquedaField);
        busquedaPanel.add(busquedaButton);

        JButton addButton = new JButton("Añadir nuevo usuario");
        addButton.addActionListener(e -> showUsuarioDialog(null));

        topPanel.add(busquedaPanel, BorderLayout.WEST);
        topPanel.add(addButton, BorderLayout.EAST);

        // --- Tabla de usuarios ---
        // isCellEditable devuelve false para que el usuario no pueda editar
        // las celdas directamente; la edición se hace a través del diálogo
        String[] columns = {"ID", "Nombre", "Email", "Rol", "Último Login"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        usuariosTable = new JTable(tableModel);
        // Solo permitimos seleccionar una fila a la vez
        usuariosTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Ajustamos el ancho preferido de cada columna
        usuariosTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        usuariosTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        usuariosTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        usuariosTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        usuariosTable.getColumnModel().getColumn(4).setPreferredWidth(150);

        // --- Menú contextual con opciones sobre el usuario seleccionado ---
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem editarItem = new JMenuItem("Editar");
        JMenuItem borrarItem = new JMenuItem("Borrar");
        JMenuItem resetPasswordItem = new JMenuItem("Restablecer contraseña");

        editarItem.addActionListener(e -> editUsuarioSeleccionado());
        borrarItem.addActionListener(e -> deleteUsuarioSeleccionado());
        resetPasswordItem.addActionListener(e -> resetearUserPassword());

        popupMenu.add(editarItem);
        popupMenu.add(borrarItem);
        // Separador visual entre opciones de gestión y opciones de contraseña
        popupMenu.addSeparator();
        popupMenu.add(resetPasswordItem);

        // Listener del ratón para mostrar el menú contextual al hacer clic derecho.
        // Se comprueban tanto mousePressed como mouseReleased porque el evento
        // de menú contextual varía según el sistema operativo (Windows vs Mac/Linux).
        usuariosTable.addMouseListener(new MouseAdapter() {
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
                int row = usuariosTable.rowAtPoint(e.getPoint());
                if (row >= 0 && row < usuariosTable.getRowCount()) {
                    usuariosTable.setRowSelectionInterval(row, row);

                    // Deshabilitamos el botón de borrar si el usuario seleccionado
                    // es el propio usuario logueado o es un administrador,
                    // para evitar autoeliminación y eliminación de admins
                    int usuarioId = (int) tableModel.getValueAt(row, 0);
                    boolean esPropioUsuarioOAdmin = usuarioId == AppState.getInstance().getLoggedInUser().getIdUsuario()
                            || "Administrador".equals(tableModel.getValueAt(row, 3));
                    borrarItem.setEnabled(!esPropioUsuarioOAdmin);

                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        // --- Ensamblaje del panel principal ---
        add(topPanel, BorderLayout.NORTH);
        // Envolvemos la tabla en un JScrollPane para que tenga barra de desplazamiento
        add(new JScrollPane(usuariosTable), BorderLayout.CENTER);

        // Cargamos todos los usuarios al inicializar el panel
        actualizarTable("");
    }

    /**
     * Actualiza el contenido de la tabla filtrando por el término de búsqueda.
     *
     * Carga todos los usuarios de la base de datos y filtra en memoria
     * los que contienen el término en el nombre o el email.
     * Si el término está vacío, muestra todos los usuarios.
     *
     * @param busquedaTerm texto a buscar (puede estar vacío para mostrar todos)
     */
    private void actualizarTable(String busquedaTerm) {
        // Vaciamos la tabla antes de rellenarla con los nuevos datos
        tableModel.setRowCount(0);
        List<Usuario> usuarios = usuarioController.findAll();

        for (Usuario usuario : usuarios) {
            // Filtramos por nombre o email si hay término de búsqueda
            if (busquedaTerm.isEmpty() ||
                    usuario.getNombre().toLowerCase().contains(busquedaTerm.toLowerCase()) ||
                    usuario.getEmail().toLowerCase().contains(busquedaTerm.toLowerCase())) {

                tableModel.addRow(new Object[]{
                        usuario.getIdUsuario(),
                        usuario.getNombre(),
                        usuario.getEmail(),
                        usuario.getRol().getNombre(),
                        // Mostramos "N/A" si el usuario nunca ha iniciado sesión
                        usuario.getLastLogin() != null ? usuario.getLastLogin() : "N/A"
                });
            }
        }
    }

    /**
     * Muestra el diálogo de creación o edición de un usuario.
     *
     * En modo creación muestra los campos de contraseña y confirmación.
     * En modo edición se ocultan los campos de contraseña (se usa el
     * diálogo de restablecer contraseña para cambiarla).
     *
     * @param usuario usuario a editar, o null para crear uno nuevo
     */
    private void showUsuarioDialog(Usuario usuario) {
        isEditing = (usuario != null);
        usuarioSeleccionado = usuario;

        // Creamos un diálogo modal para que el usuario no pueda interactuar
        // con la tabla mientras está editando un usuario
        usuarioDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEditing ? "Editar usuario" : "Nuevo usuario",
                true);
        usuarioDialog.setLayout(new BorderLayout(10, 10));

        // --- Formulario del usuario ---
        JPanel formularioPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Campos del formulario
        JTextField nombreField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JPasswordField confirmarPasswordField = new JPasswordField(20);
        JComboBox<String> rolCombo = new JComboBox<>(new String[]{"Empleado", "Administrador"});

        // Si estamos editando, pre-rellenamos los campos con los datos actuales.
        // Los campos de contraseña se ocultan en modo edición: para cambiar la
        // contraseña se usa el diálogo de restablecer contraseña.
        if (isEditing) {
            nombreField.setText(usuario.getNombre());
            emailField.setText(usuario.getEmail());
            rolCombo.setSelectedItem(usuario.getRol().getNombre());
        }

        // Añadimos los campos al formulario con su etiqueta correspondiente
        gbc.gridx = 0; gbc.gridy = 0;
        formularioPanel.add(new JLabel("Nombre:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(nombreField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formularioPanel.add(new JLabel("Email:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(emailField, gbc);

        // Los campos de contraseña solo se muestran en modo creación
        if (!isEditing) {
            gbc.gridx = 0; gbc.gridy = 2;
            formularioPanel.add(new JLabel("Contraseña:*"), gbc);
            gbc.gridx = 1;
            formularioPanel.add(passwordField, gbc);

            gbc.gridx = 0; gbc.gridy = 3;
            formularioPanel.add(new JLabel("Confirmar contraseña:*"), gbc);
            gbc.gridx = 1;
            formularioPanel.add(confirmarPasswordField, gbc);
        }

        // La fila del rol cambia según si estamos en modo edición o creación
        gbc.gridx = 0; gbc.gridy = isEditing ? 2 : 4;
        formularioPanel.add(new JLabel("Rol:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(rolCombo, gbc);

        // Nota informativa sobre los campos obligatorios
        gbc.gridx = 0; gbc.gridy = isEditing ? 3 : 5;
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
                String email = emailField.getText().trim();
                String rol = (String) rolCombo.getSelectedItem();

                // Validamos que los campos obligatorios estén rellenos
                if (nombre.isEmpty() || email.isEmpty()) {
                    JOptionPane.showMessageDialog(usuarioDialog,
                            "Por favor complete todos los campos requeridos",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Validamos el formato del email
                if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    JOptionPane.showMessageDialog(usuarioDialog,
                            "Por favor, introduce una dirección de correo electrónico válida",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // En modo creación validamos también la contraseña
                String password = null;
                if (!isEditing) {
                    password = new String(passwordField.getPassword());
                    String confirmarPassword = new String(confirmarPasswordField.getPassword());

                    if (password.length() < 6) {
                        JOptionPane.showMessageDialog(usuarioDialog,
                                "La contraseña debe tener al menos 6 caracteres",
                                "Error de validación",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Verificamos que la contraseña y su confirmación coincidan
                    if (!password.equals(confirmarPassword)) {
                        JOptionPane.showMessageDialog(usuarioDialog,
                                "Las contraseñas no coinciden",
                                "Error de validación",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // Creamos o actualizamos el usuario según el modo del diálogo
                Usuario usuarioToSave = isEditing ? usuarioSeleccionado : new Usuario();
                usuarioToSave.setNombre(nombre);
                usuarioToSave.setEmail(email);
                if (password != null) {
                    usuarioToSave.setPassword(password);
                }

                // Asignamos el rol seleccionado: Administrador (id=1) o Empleado (id=2)
                Rol usuarioRol = new Rol();
                usuarioRol.setIdRol("Administrador".equals(rol) ? 1 : 2);
                usuarioRol.setNombre(rol);
                usuarioToSave.setRol(usuarioRol);

                if (isEditing) {
                    usuarioController.update(usuarioToSave);
                } else {
                    usuarioController.create(usuarioToSave);
                }

                // Cerramos el diálogo y refrescamos la tabla
                usuarioDialog.dispose();
                actualizarTable(busquedaField.getText());

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(usuarioDialog,
                        "Error al guardar usuario: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // El botón cancelar simplemente cierra el diálogo sin guardar cambios
        cancelarButton.addActionListener(e -> usuarioDialog.dispose());

        buttonsPanel.add(guardarButton);
        buttonsPanel.add(cancelarButton);

        // Ensamblamos el diálogo
        usuarioDialog.add(formularioPanel, BorderLayout.CENTER);
        usuarioDialog.add(buttonsPanel, BorderLayout.SOUTH);

        // pack() ajusta el tamaño del diálogo al contenido
        usuarioDialog.pack();
        usuarioDialog.setLocationRelativeTo(this);
        usuarioDialog.setVisible(true);
    }

    /**
     * Abre el diálogo de edición con el usuario seleccionado en la tabla.
     * Si no hay ninguna fila seleccionada, no hace nada.
     */
    private void editUsuarioSeleccionado() {
        int filaSeleccionada = usuariosTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            // Obtenemos el ID del usuario de la primera columna de la fila seleccionada
            Integer usuarioId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            Usuario usuario = usuarioController.findById(usuarioId);
            if (usuario != null) {
                showUsuarioDialog(usuario);
            }
        }
    }

    /**
     * Elimina el usuario seleccionado en la tabla tras verificar restricciones
     * de seguridad y pedir confirmación al usuario.
     *
     * No se permite eliminar el propio usuario logueado para evitar
     * que un administrador se quede sin acceso al sistema.
     */
    private void deleteUsuarioSeleccionado() {
        int filaSeleccionada = usuariosTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer usuarioId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            String usuarioNombre = (String) tableModel.getValueAt(filaSeleccionada, 1);

            // Impedimos que el administrador se elimine a sí mismo
            if (usuarioId.equals(AppState.getInstance().getLoggedInUser().getIdUsuario())) {
                JOptionPane.showMessageDialog(this,
                        "No puedes eliminar tu propia cuenta",
                        "Operación no permitida",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Pedimos confirmación antes de eliminar para evitar borrados accidentales
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "¿Estás seguro de que deseas eliminar al usuario: " + usuarioNombre + "?",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (respuesta == JOptionPane.YES_OPTION) {
                try {
                    usuarioController.delete(usuarioId);
                    // Refrescamos la tabla para reflejar el cambio
                    actualizarTable(busquedaField.getText());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                            "Error al eliminar usuario: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Muestra el diálogo para restablecer la contraseña del usuario seleccionado.
     *
     * Permite al administrador establecer una nueva contraseña para cualquier
     * usuario sin necesidad de conocer la contraseña actual.
     * La nueva contraseña debe tener al menos 6 caracteres y coincidir
     * con el campo de confirmación.
     */
    private void resetearUserPassword() {
        int filaSeleccionada = usuariosTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer usuarioId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            String nombreUsuario = (String) tableModel.getValueAt(filaSeleccionada, 1);

            // Creamos un diálogo modal específico para el restablecimiento de contraseña
            JDialog resetearDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                    "Restablecer contraseña",
                    true);
            resetearDialog.setLayout(new BorderLayout(10, 10));

            JPanel formularioPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            JPasswordField newPasswordField = new JPasswordField(20);
            JPasswordField confirmarPasswordField = new JPasswordField(20);

            gbc.gridx = 0; gbc.gridy = 0;
            formularioPanel.add(new JLabel("Nueva contraseña:"), gbc);
            gbc.gridx = 1;
            formularioPanel.add(newPasswordField, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            formularioPanel.add(new JLabel("Confirmar contraseña:"), gbc);
            gbc.gridx = 1;
            formularioPanel.add(confirmarPasswordField, gbc);

            JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton resetearButton = new JButton("Restablecer");
            JButton cancelarButton = new JButton("Cancelar");

            resetearButton.addActionListener(e -> {
                try {
                    String nuevoPassword = new String(newPasswordField.getPassword());
                    String confirmarPassword = new String(confirmarPasswordField.getPassword());

                    // Validamos la longitud mínima de la contraseña
                    if (nuevoPassword.length() < 6) {
                        JOptionPane.showMessageDialog(resetearDialog,
                                "La contraseña debe tener al menos 6 caracteres",
                                "Error de validación",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Verificamos que la contraseña y su confirmación coincidan
                    if (!nuevoPassword.equals(confirmarPassword)) {
                        JOptionPane.showMessageDialog(resetearDialog,
                                "Las contraseñas no coinciden",
                                "Error de validación",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    usuarioController.resetPassword(usuarioId, nuevoPassword);
                    resetearDialog.dispose();
                    JOptionPane.showMessageDialog(this,
                            "Contraseña restablecida para el usuario: " + nombreUsuario,
                            "Contraseña restablecida",
                            JOptionPane.INFORMATION_MESSAGE);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(resetearDialog,
                            "Error restableciendo la contraseña: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });

            // El botón cancelar simplemente cierra el diálogo sin guardar cambios
            cancelarButton.addActionListener(e -> resetearDialog.dispose());

            buttonsPanel.add(resetearButton);
            buttonsPanel.add(cancelarButton);

            // Ensamblamos el diálogo
            resetearDialog.add(formularioPanel, BorderLayout.CENTER);
            resetearDialog.add(buttonsPanel, BorderLayout.SOUTH);

            resetearDialog.pack();
            resetearDialog.setLocationRelativeTo(this);
            resetearDialog.setVisible(true);
        }
    }
}