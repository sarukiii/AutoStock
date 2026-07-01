package views;

import config.DatabaseConnection;
import controllers.UsuarioController;
import models.Usuario;
import state.AppState;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LoginView extends JFrame {
    private final UsuarioController usuarioController;
    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JLabel statusLabel;

    public LoginView() {
        this.usuarioController = new UsuarioController();

        // Configuración frame
        setTitle("AutoStock - Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panel principal
        JPanel loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Panel de login
        gbc.gridx = 0;
        gbc.gridy = 0;
        loginPanel.add(new JLabel("Email:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        emailField = new JTextField(20);
        emailField.setText(AppState.getInstance().getLastLoggedInEmail());
        loginPanel.add(emailField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        loginPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        loginPanel.add(passwordField, gbc);

        // Componentes
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        loginPanel.add(statusLabel, gbc);

        // Etiqueta de estado
        loginButton = new JButton("Login");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        loginPanel.add(loginButton, gbc);

        // Boton login
        loginButton.addActionListener(e -> inicioLogin());

        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel registroLabel = new JLabel("¿No tienes cuenta?");
        JButton registroLink = new JButton("Registro");
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

        // Listener para la tecla Intro en el campo de contraseña
        passwordField.addActionListener(e -> inicioLogin());

        // Paneles
        mainPanel.add(new JLabel("AutoStock Sistema de Inventarios", SwingConstants.CENTER), BorderLayout.NORTH);
        mainPanel.add(loginPanel, BorderLayout.CENTER);
        add(mainPanel);

        // Cierre de la ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DatabaseConnection.closeConnection();
            }
        });
    }

    private void openRegistroView() {
        EventQueue.invokeLater(() -> {
            new RegistroView().setVisible(true);
            this.dispose();
        });
    }

    private void inicioLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Por favor ingrese el correo electrónico y la contraseña");
            return;
        }

        try {
            loginButton.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            Usuario usuario = usuarioController.login(email, password);

            if (usuario != null) {
                AppState.getInstance().setLoggedInUser(usuario);
                openMainView();
            } else {
                statusLabel.setText("Correo electrónico o contraseña no válidos");
                passwordField.setText("");
            }

        } catch (Exception ex) {
            statusLabel.setText("Error durante el inicio de sesión: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            loginButton.setEnabled(true);
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void openMainView() {
        EventQueue.invokeLater(() -> {
            MainView mainView = new MainView();
            mainView.setVisible(true);
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

        // Iniciar la aplicación
        EventQueue.invokeLater(() -> {
            new LoginView().setVisible(true);
        });
    }
}