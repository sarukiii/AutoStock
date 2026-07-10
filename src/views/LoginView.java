package views;

import config.DatabaseConnection;
import controllers.UsuarioController;
import models.Usuario;
import state.AppState;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Vista del formulario de inicio de sesión.
 *
 * Es la primera ventana que ve el usuario al arrancar la aplicación.
 * Recoge el email y la contraseña, delega la autenticación en
 * UsuarioController y, si las credenciales son correctas, abre la
 * ventana principal (MainView). Si son incorrectas, muestra un mensaje
 * de error sin revelar si el email o la contraseña son los incorrectos
 * (por seguridad).
 *
 * Extiende JFrame para ser una ventana independiente de la aplicación.
 */
public class LoginView extends JFrame {

    // Controlador que gestiona la lógica de autenticación
    private final UsuarioController usuarioController;
    // Campos del formulario de login
    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    // Etiqueta para mostrar mensajes de error al usuario
    private final JLabel statusLabel;

    public LoginView() {
        this.usuarioController = new UsuarioController();

        // --- Configuración de la ventana ---
        setTitle("AutoStock - Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Centramos la ventana en la pantalla
        setLocationRelativeTo(null);

        // Panel exterior con márgenes para que el contenido no quede pegado al borde
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel interior con GridBagLayout para alinear etiquetas y campos en columnas
        JPanel loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // --- Campo de email ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        loginPanel.add(new JLabel("Email:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        emailField = new JTextField(20);
        // Pre-rellenamos el email con el último usado (guardado en AppState)
        emailField.setText(AppState.getInstance().getLastLoggedInEmail());
        loginPanel.add(emailField, gbc);

        // --- Campo de contraseña ---
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        loginPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        // JPasswordField oculta los caracteres escritos por seguridad
        passwordField = new JPasswordField(20);
        loginPanel.add(passwordField, gbc);

        // --- Etiqueta de estado para mensajes de error ---
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        loginPanel.add(statusLabel, gbc);

        // --- Botón de login ---
        loginButton = new JButton("Login");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        loginPanel.add(loginButton, gbc);

        // Al pulsar el botón se lanza el proceso de autenticación
        loginButton.addActionListener(e -> inicioLogin());

        // --- Enlace a la vista de registro ---
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel registroLabel = new JLabel("¿No tienes cuenta?");
        JButton registroLink = new JButton("Registro");
        // Estilamos el botón para que parezca un enlace (sin borde ni fondo)
        registroLink.setBorderPainted(false);
        registroLink.setContentAreaFilled(false);
        registroLink.setForeground(Color.BLUE);
        registroLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registroLink.addActionListener(e -> openRegistroView());
        linkPanel.add(registroLabel);
        linkPanel.add(registroLink);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        loginPanel.add(linkPanel, gbc);

        // Permitimos iniciar sesión pulsando Intro en el campo de contraseña,
        // sin necesidad de hacer clic en el botón
        passwordField.addActionListener(e -> inicioLogin());

        // --- Ensamblaje del panel principal ---
        mainPanel.add(new JLabel("AutoStock Sistema de Inventarios", SwingConstants.CENTER), BorderLayout.NORTH);
        mainPanel.add(loginPanel, BorderLayout.CENTER);
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
     * Abre la vista de registro y cierra la ventana de login.
     *
     * Se usa EventQueue.invokeLater para garantizar que los cambios en la
     * interfaz se ejecutan en el hilo de eventos de Swing (Event Dispatch Thread),
     * evitando problemas de concurrencia en la interfaz gráfica.
     */
    private void openRegistroView() {
        EventQueue.invokeLater(() -> {
            new RegistroView().setVisible(true);
            // Cerramos la ventana de login para liberar recursos
            this.dispose();
        });
    }

    /**
     * Procesa el intento de inicio de sesión.
     *
     * Valida que los campos no estén vacíos, llama al controlador para
     * verificar las credenciales y, según el resultado, abre la ventana
     * principal o muestra un mensaje de error.
     *
     * Durante el proceso desactiva el botón y muestra el cursor de espera
     * para evitar envíos múltiples y dar feedback visual al usuario.
     */
    private void inicioLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        // Validación básica: los campos no pueden estar vacíos
        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Por favor ingrese el correo electrónico y la contraseña");
            return;
        }

        try {
            // Desactivamos el botón para evitar envíos múltiples mientras se procesa
            loginButton.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            Usuario usuario = usuarioController.login(email, password);

            if (usuario != null) {
                // Guardamos el usuario en el estado global de la sesión
                AppState.getInstance().setLoggedInUser(usuario);
                openMainView();
            } else {
                // Mensaje genérico: no indicamos si el error es en email o contraseña
                statusLabel.setText("Correo electrónico o contraseña no válidos");
                passwordField.setText("");
            }

        } catch (Exception ex) {
            statusLabel.setText("Error durante el inicio de sesión: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            // Restauramos el botón y el cursor independientemente del resultado
            loginButton.setEnabled(true);
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * Abre la ventana principal y cierra la ventana de login.
     */
    private void openMainView() {
        EventQueue.invokeLater(() -> {
            MainView mainView = new MainView();
            mainView.setVisible(true);
            this.dispose();
        });
    }

    /**
     * Método main alternativo para lanzar la aplicación directamente desde esta
     * clase.
     *
     * Aplica el Look and Feel del sistema operativo para que la interfaz
     * tenga el aspecto nativo de Windows, Mac o Linux.
     *
     * @param args argumentos de línea de comandos (no se utilizan)
     */
    public static void main(String[] args) {
        try {
            // Look and Feel nativo del sistema operativo
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Lanzamos la ventana de login en el hilo de eventos de Swing
        EventQueue.invokeLater(() -> {
            new LoginView().setVisible(true);
        });
    }
}