package views;

import config.DatabaseConnection;
import controllers.UsuarioController;
import models.Usuario;
import models.Rol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class RegistroView extends JFrame {
    private final UsuarioController usuarioController;
    private final JTextField nombreField;
    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmarPasswordField;
    private final JButton registroButton;
    private final JButton volverLoginButton;
    private final JLabel statusLabel;

    public RegistroView() {
        this.usuarioController = new UsuarioController();

        // Configuración frame
        setTitle("AutoStock - Registro");
        setSize(450, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel principal
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel de registro
        JPanel registroPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Componentes
        // Nombre
        gbc.gridx = 0;
        gbc.gridy = 0;
        registroPanel.add(new JLabel("Nombre:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        nombreField = new JTextField(20);
        registroPanel.add(nombreField, gbc);

        // Email
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        registroPanel.add(new JLabel("Email:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        emailField = new JTextField(20);
        registroPanel.add(emailField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        registroPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        registroPanel.add(passwordField, gbc);

        // Confirmación password 
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        registroPanel.add(new JLabel("Confirmar Password:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        confirmarPasswordField = new JPasswordField(20);
        registroPanel.add(confirmarPasswordField, gbc);

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        registroPanel.add(statusLabel, gbc);

        // Barra de estado
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        registroButton = new JButton("Registro");
        registroButton.addActionListener(e -> handleRegistration());
        buttonsPanel.add(registroButton);

        volverLoginButton = new JButton("Volver al login");
        volverLoginButton.addActionListener(e -> volverAlLogin());
        buttonsPanel.add(volverLoginButton);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        registroPanel.add(buttonsPanel, gbc);

        // Listener para tecla Enter
        confirmarPasswordField.addActionListener(e -> handleRegistration());

        // Paneles
        JLabel tituloLabel = new JLabel("Crear una nueva cuenta", SwingConstants.CENTER);
        tituloLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        mainPanel.add(tituloLabel, BorderLayout.NORTH);
        mainPanel.add(registroPanel, BorderLayout.CENTER);
        add(mainPanel);

        // Cierre de ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DatabaseConnection.closeConnection();
            }
        });
    }

    private void handleRegistration() {
        // Obtener y validar entradas
        String nombre = nombreField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmarPassword = new String(confirmarPasswordField.getPassword());

        // Validación
        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty() || confirmarPassword.isEmpty()) {
            statusLabel.setText("Todos los campos son obligatorios");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            statusLabel.setText("Formato de email no válido");
            return;
        }

        if (password.length() < 6) {
            statusLabel.setText("La contraseña debe tener al menos 6 caracteres");
            return;
        }

        if (!password.equals(confirmarPassword)) {
            statusLabel.setText("Las contraseñas no coinciden");
            return;
        }

        try {
            registroButton.setEnabled(false);
            volverLoginButton.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            // Crear nuevo usuario
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setNombre(nombre);
            nuevoUsuario.setEmail(email);
            nuevoUsuario.setPassword(password);

            // Establecer rol predeterminado como usuario normal
            Rol defaultRol = new Rol();
            defaultRol.setIdRol(2); // Suponiendo que 2 es el ID del rol de usuario habitual
            nuevoUsuario.setRol(defaultRol);

            // Crear el usuario
            usuarioController.create(nuevoUsuario);

            // Mostrar mensaje de registro correcto y redirigir a la sesión
            JOptionPane.showMessageDialog(this,
            		"¡Registro correcto! Por favor inicie sesión con sus credenciales.",
                    "Correcto",
                    JOptionPane.INFORMATION_MESSAGE);

            volverAlLogin();

        } catch (Exception ex) {
            String errorMessage = ex.getMessage();
            if (errorMessage.contains("Duplicado")) {
                statusLabel.setText("El email ya existe");
            } else {
                statusLabel.setText("Error durante el registro: " + ex.getMessage());
            }
            ex.printStackTrace();
        } finally {
            registroButton.setEnabled(true);
            volverLoginButton.setEnabled(true);
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void volverAlLogin() {
        EventQueue.invokeLater(() -> {
            new LoginView().setVisible(true);
            this.dispose();
        });
    }

    public static void main(String[] args) {
        // Apariencia predeterminada del sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Inicio de la aplicación
        EventQueue.invokeLater(() -> {
            new RegistroView().setVisible(true);
        });
    }
}