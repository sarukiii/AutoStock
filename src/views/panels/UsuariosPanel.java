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

public class UsuariosPanel extends JPanel {
    private final UsuarioController usuarioController;
    private final JTable usuariosTable;
    private final DefaultTableModel tableModel;
    private final JTextField busquedaField;
    private JDialog usuarioDialog;
    private Usuario usuarioSeleccionado;
    private boolean isEditing = false;

    public UsuariosPanel() {
        this.usuarioController = new UsuarioController();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel con barra de búsqueda y boton
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));

        // Panel de búsqueda
        JPanel busquedaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        busquedaField = new JTextField(20);
        busquedaField.addActionListener(e -> actualizarTable(busquedaField.getText()));

        JButton busquedaButton = new JButton("Buscar");
        busquedaButton.addActionListener(e -> actualizarTable(busquedaField.getText()));

        busquedaPanel.add(new JLabel("Buscar:"));
        busquedaPanel.add(busquedaField);
        busquedaPanel.add(busquedaButton);

        // Añadir button
        JButton addButton = new JButton("Añadir nuevo usuario");
        addButton.addActionListener(e -> showUsuarioDialog(null));

        topPanel.add(busquedaPanel, BorderLayout.WEST);
        topPanel.add(addButton, BorderLayout.EAST);

        // Tabla
        String[] columns = {"ID", "Nombre", "Email", "Rol", "Ultimo Login"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        usuariosTable = new JTable(tableModel);
        usuariosTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        usuariosTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        usuariosTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        usuariosTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        usuariosTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        usuariosTable.getColumnModel().getColumn(4).setPreferredWidth(150);

        // Menú contextual
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem editarItem = new JMenuItem("Editar");
        JMenuItem borrarItem = new JMenuItem("Borrar");
        JMenuItem resetPasswordItem = new JMenuItem("Reset Password");

        editarItem.addActionListener(e -> editUsuarioSeleccionado());
        borrarItem.addActionListener(e -> deleteUsuarioSeleccionado());
        resetPasswordItem.addActionListener(e -> resetearUserPassword());

        popupMenu.add(editarItem);
        popupMenu.add(borrarItem);
        popupMenu.addSeparator();
        popupMenu.add(resetPasswordItem);

        usuariosTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = usuariosTable.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < usuariosTable.getRowCount()) {
                        usuariosTable.setRowSelectionInterval(row, row);

                        // Deshabilitar la autoeliminación para el usuario y 
                        //administrador que ha iniciado sesión actualmente
                        int usuarioId = (int) tableModel.getValueAt(row, 0);
                        boolean isSelfOrAdmin = usuarioId == AppState.getInstance().getLoggedInUser().getIdUsuario()
                                || "Administrador".equals(tableModel.getValueAt(row, 3));
                        borrarItem.setEnabled(!isSelfOrAdmin);

                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        // Componentes panel principal
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(usuariosTable), BorderLayout.CENTER);

        // Carga inicial
        actualizarTable("");
    }

    private void actualizarTable(String busquedaTerm) {
        tableModel.setRowCount(0);
        List<Usuario> usuarios = usuarioController.findAll();

        for (Usuario usuario : usuarios) {
            if (busquedaTerm.isEmpty() ||
                    usuario.getNombre().toLowerCase().contains(busquedaTerm.toLowerCase()) ||
                    usuario.getEmail().toLowerCase().contains(busquedaTerm.toLowerCase())) {

                tableModel.addRow(new Object[]{
                        usuario.getIdUsuario(),
                        usuario.getNombre(),
                        usuario.getEmail(),
                        usuario.getRol().getNombre(),
                        usuario.getLastLogin() != null ? usuario.getLastLogin() : "N/A"
                });
            }
        }
    }

    private void showUsuarioDialog(Usuario usuario) {
        isEditing = (usuario != null);
        usuarioSeleccionado = usuario;

        usuarioDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEditing ? "Editar usuario" : "Nuevo usuario",
                true);
        usuarioDialog.setLayout(new BorderLayout(10, 10));

        // Panel principal
        JPanel formularioPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Formulario
        JTextField nombreField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JPasswordField confirmarPasswordField = new JPasswordField(20);
        JComboBox<String> rolCombo = new JComboBox<>(new String[]{"Empleado", "Administrador"});

        // Edición
        if (isEditing) {
            nombreField.setText(usuario.getNombre());
            emailField.setText(usuario.getEmail());
            rolCombo.setSelectedItem(usuario.getRol().getNombre());
            // No mostrar contraseña en modo edición
            passwordField.setEnabled(false);
            confirmarPasswordField.setEnabled(false);
        }

        // Componentes del formulario
        gbc.gridx = 0; gbc.gridy = 0;
        formularioPanel.add(new JLabel("Nombre:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(nombreField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formularioPanel.add(new JLabel("Email:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(emailField, gbc);

        if (!isEditing) {
            gbc.gridx = 0; gbc.gridy = 2;
            formularioPanel.add(new JLabel("Password:*"), gbc);
            gbc.gridx = 1;
            formularioPanel.add(passwordField, gbc);

            gbc.gridx = 0; gbc.gridy = 3;
            formularioPanel.add(new JLabel("Confirmar password:*"), gbc);
            gbc.gridx = 1;
            formularioPanel.add(confirmarPasswordField, gbc);
        }

        gbc.gridx = 0; gbc.gridy = isEditing ? 2 : 4;
        formularioPanel.add(new JLabel("Rol:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(rolCombo, gbc);

        // Añadir campos obligatorios
        gbc.gridx = 0; gbc.gridy = isEditing ? 3 : 5;
        gbc.gridwidth = 2;
        formularioPanel.add(new JLabel("* Campos obligatorios"), gbc);

        // Botones panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton guardarButton = new JButton("Guardar");
        JButton cancelarButton = new JButton("Cancelar");

        guardarButton.addActionListener(e -> {
            try {
                // Validar datos entrada
                String nombre = nombreField.getText().trim();
                String email = emailField.getText().trim();
                String rol = (String) rolCombo.getSelectedItem();

                if (nombre.isEmpty() || email.isEmpty()) {
                    JOptionPane.showMessageDialog(usuarioDialog,
                    		"Por favor complete todos los campos requeridos",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Validar formato email
                if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    JOptionPane.showMessageDialog(usuarioDialog,
                            "Por favor, introduce una dirección de correo electrónico válida",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Validación formato de password
                String password = null;
                if (!isEditing) {
                    password = new String(passwordField.getPassword());
                    String confirmarPassword = new String(confirmarPasswordField.getPassword());

                    if (password.length() < 6) {
                        JOptionPane.showMessageDialog(usuarioDialog,
                                "El password debe tener al menos 6 caracteres",
                                "Error de validación",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (!password.equals(confirmarPassword)) {
                        JOptionPane.showMessageDialog(usuarioDialog,
                        		"La contraseña debe tener al menos 6 caracteres",
                                "Error de validación",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // Crear o actualizar usuario
                Usuario editarUsuario = isEditing ? usuarioSeleccionado : new Usuario();
                editarUsuario.setNombre(nombre);
                editarUsuario.setEmail(email);
                if (password != null) {
                    editarUsuario.setPassword(password);
                }

                Rol usuarioRol = new Rol();
                usuarioRol.setIdRol("Administrador".equals(rol) ? 1 : 2);
                usuarioRol.setNombre(rol);
                editarUsuario.setRol(usuarioRol);

                if (isEditing) {
                    usuarioController.update(editarUsuario);
                } else {
                    usuarioController.create(editarUsuario);
                }

                usuarioDialog.dispose();
                actualizarTable(busquedaField.getText());

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(usuarioDialog,
                        "Error al guardar usuario: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelarButton.addActionListener(e -> usuarioDialog.dispose());

        buttonsPanel.add(guardarButton);
        buttonsPanel.add(cancelarButton);

        // Panel dialogo
        usuarioDialog.add(formularioPanel, BorderLayout.CENTER);
        usuarioDialog.add(buttonsPanel, BorderLayout.SOUTH);

        // Mostrar dialogo
        usuarioDialog.pack();
        usuarioDialog.setLocationRelativeTo(this);
        usuarioDialog.setVisible(true);
    }

    private void editUsuarioSeleccionado() {
        int filaSeleccionada = usuariosTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer usuarioId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            Usuario usuario = usuarioController.findById(usuarioId);
            if (usuario != null) {
                showUsuarioDialog(usuario);
            }
        }
    }

    private void deleteUsuarioSeleccionado() {
        int filaSeleccionada = usuariosTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer usuarioId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            String usuarioNombre = (String) tableModel.getValueAt(filaSeleccionada, 1);

            // Impedir eliminarme a mí misma o al último administrador
            if (usuarioId.equals(AppState.getInstance().getLoggedInUser().getIdUsuario())) {
                JOptionPane.showMessageDialog(this,
                		"No puedes eliminar tu propia cuenta",
                        "Operación no permitida",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            int respuesta = JOptionPane.showConfirmDialog(this,
                    "Estás seguro de que deseas eliminar al usuario: " + usuarioNombre + "?",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (respuesta == JOptionPane.YES_OPTION) {
                try {
                    usuarioController.delete(usuarioId);
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

    private void resetearUserPassword() {
        int filaSeleccionada = usuariosTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer usuarioId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            String nombreUsuario = (String) tableModel.getValueAt(filaSeleccionada, 1);

            // Cuadro de diálogo para restablecer password
            JDialog resetearDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                    "Resetear password",
                    true);
            resetearDialog.setLayout(new BorderLayout(10, 10));

            JPanel formularioPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            JPasswordField newPasswordField = new JPasswordField(20);
            JPasswordField confirmarPasswordField = new JPasswordField(20);

            gbc.gridx = 0; gbc.gridy = 0;
            formularioPanel.add(new JLabel("Nuevo password:"), gbc);
            gbc.gridx = 1;
            formularioPanel.add(newPasswordField, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            formularioPanel.add(new JLabel("Confirmar password:"), gbc);
            gbc.gridx = 1;
            formularioPanel.add(confirmarPasswordField, gbc);

            JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton resetearButton = new JButton("Resetear");
            JButton cancelarButton = new JButton("Cancelar");

            resetearButton.addActionListener(e -> {
                try {
                    String nuevoPassword = new String(newPasswordField.getPassword());
                    String confirmarPassword = new String(confirmarPasswordField.getPassword());

                    if (nuevoPassword.length() < 6) {
                        JOptionPane.showMessageDialog(resetearDialog,
                        		"La contraseña debe tener al menos 6 caracteres",
                                "Error de validación",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
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
                    		"Password restablecido para el usuari@: " + nombreUsuario,
                            "Password reestablecido",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(resetearDialog,
                    		"Error reestableciendo el password: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            
            cancelarButton.addActionListener(e -> resetearDialog.dispose());
            
            buttonsPanel.add(resetearButton);
            buttonsPanel.add(cancelarButton);
            
            resetearDialog.add(formularioPanel, BorderLayout.CENTER);
            resetearDialog.add(buttonsPanel, BorderLayout.SOUTH);
            
            resetearDialog.pack();
            resetearDialog.setLocationRelativeTo(this);
            resetearDialog.setVisible(true);
            
        }
    }
}
                    