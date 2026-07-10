package views;

import config.DatabaseConnection;
import controllers.UsuarioController;
import models.Usuario;
import models.Rol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Vista del formulario de registro de nuevos usuarios.
 *
 * Permite crear una cuenta nueva introduciendo nombre, email y contraseña.
 * Incluye validaciones básicas antes de enviar los datos al controlador:
 * campos obligatorios, formato de email, longitud mínima de contraseña
 * y confirmación de contraseña.
 *
 * Todo usuario registrado desde este formulario recibe automáticamente
 * el rol de Empleado (id_rol = 2). El rol de Administrador solo puede
 * asignarlo un administrador desde el panel de gestión de usuarios.
 *
 * Extiende JFrame para ser una ventana independiente de la aplicación.
 */
public class RegistroView extends JFrame {

    // Controlador que gestiona la creación de usuarios en la base de datos
    private final UsuarioController usuarioController;
    // Campos del formulario de registro
    private final JTextField nombreField;
    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmarPasswordField;
    private final JButton registroButton;
    private final JButton volverLoginButton;
    // Etiqueta para mostrar mensajes de error o validación al usuario
    private final JLabel statusLabel;

    public RegistroView() {
        this.usuarioController = new UsuarioController();

        // --- Configuración de la ventana ---
        setTitle("AutoStock - Registro");
        setSize(450, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Centramos la ventana en la pantalla
        setLocationRelativeTo(null);

        // Panel exterior con márgenes para que el contenido no quede pegado al borde
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel interior con GridBagLayout para alinear etiquetas y campos en columnas
        JPanel registroPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // --- Campo de nombre ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        registroPanel.add(new JLabel("Nombre:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        nombreField = new JTextField(20);
        registroPanel.add(nombreField, gbc);

        // --- Campo de email ---
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        registroPanel.add(new JLabel("Email:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        emailField = new JTextField(20);
        registroPanel.add(emailField, gbc);

        // --- Campo de contraseña ---
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        registroPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        // JPasswordField oculta los caracteres escritos por seguridad
        passwordField = new JPasswordField(20);
        registroPanel.add(passwordField, gbc);

        // --- Campo de confirmación de contraseña ---
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        registroPanel.add(new JLabel("Confirmar Password:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        confirmarPasswordField = new JPasswordField(20);
        registroPanel.add(confirmarPasswordField, gbc);

        // --- Etiqueta de estado para mensajes de error ---
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        registroPanel.add(statusLabel, gbc);

        // --- Panel de botones ---
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

        // Permitimos registrarse pulsando Intro en el campo de confirmación
        // sin necesidad de hacer clic en el botón
        confirmarPasswordField.addActionListener(e -> handleRegistration());

        // --- Ensamblaje del panel principal ---
        JLabel tituloLabel = new JLabel("Crear una nueva cuenta", SwingConstants.CENTER);
        tituloLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        mainPanel.add(tituloLabel, BorderLayout.NORTH);
        mainPanel.add(registroPanel, BorderLayout.CENTER);
        add(mainPanel);

        // Al cerrar la ventana cerramos también la conexión a la base de datos
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DatabaseConnection.closeConnection();
            }
        });
    }

    /**
     * Procesa el intento de registro de un nuevo usuario.
     *
     * Realiza las siguientes validaciones antes de crear el usuario:
     * - Todos los campos son obligatorios
     * - El email debe tener formato válido
     * - La contraseña debe tener al menos 6 caracteres
     * - La contraseña y su confirmación deben coincidir
     *
     * Si las validaciones pasan, crea el usuario con rol Empleado y
     * redirige al login para que el usuario inicie sesión.
     */
    private void handleRegistration() {
        String nombre = nombreField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmarPassword = new String(confirmarPasswordField.getPassword());

        // --- Validaciones del formulario ---

        // Todos los campos son obligatorios
        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty() || confirmarPassword.isEmpty()) {
            statusLabel.setText("Todos los campos son obligatorios");
            return;
        }

        // Validamos el formato del email con una expresión regular básica
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            statusLabel.setText("Formato de email no válido");
            return;
        }

        // La contraseña debe tener al menos 6 caracteres
        if (password.length() < 6) {
            statusLabel.setText("La contraseña debe tener al menos 6 caracteres");
            return;
        }

        // La contraseña y su confirmación deben ser idénticas
        if (!password.equals(confirmarPassword)) {
            statusLabel.setText("Las contraseñas no coinciden");
            return;
        }

        try {
            // Desactivamos los botones para evitar envíos múltiples mientras se procesa
            registroButton.setEnabled(false);
            volverLoginButton.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            // Construimos el objeto usuario con los datos del formulario
            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setNombre(nombre);
            nuevoUsuario.setEmail(email);
            nuevoUsuario.setPassword(password);

            // Asignamos el rol de Empleado por defecto (id_rol = 2).
            // El rol de Administrador solo puede asignarlo un administrador
            // desde el panel de gestión de usuarios.
            Rol rolEmpleado = new Rol();
            rolEmpleado.setIdRol(2);
            nuevoUsuario.setRol(rolEmpleado);

            usuarioController.create(nuevoUsuario);

            // Informamos al usuario del éxito y lo redirigimos al login
            JOptionPane.showMessageDialog(this,
                    "¡Registro correcto! Por favor inicie sesión con sus credenciales.",
                    "Correcto",
                    JOptionPane.INFORMATION_MESSAGE);

            volverAlLogin();

        } catch (Exception ex) {
            // Detectamos si el error es por email duplicado para dar un mensaje más claro
            String errorMessage = ex.getMessage();
            if (errorMessage.contains("Duplicado")) {
                statusLabel.setText("El email ya existe");
            } else {
                statusLabel.setText("Error durante el registro: " + ex.getMessage());
            }
            ex.printStackTrace();
        } finally {
            // Restauramos los botones y el cursor independientemente del resultado
            registroButton.setEnabled(true);
            volverLoginButton.setEnabled(true);
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * Cierra la vista de registro y vuelve a la ventana de login.
     */
    private void volverAlLogin() {
        EventQueue.invokeLater(() -> {
            new LoginView().setVisible(true);
            // Cerramos esta ventana para liberar recursos
            this.dispose();
        });
    }

    /**
     * Método main alternativo para lanzar esta vista directamente.
     * Aplica el Look and Feel del sistema operativo.
     *
     * @param args argumentos de línea de comandos (no se utilizan)
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        EventQueue.invokeLater(() -> {
            new RegistroView().setVisible(true);
        });
    }
}