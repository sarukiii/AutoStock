package views;

import config.DatabaseConnection;
import state.AppState;
import views.panels.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Ventana principal de la aplicación AutoStock.
 *
 * Es la ventana central que el usuario ve después de iniciar sesión.
 * Contiene una barra de menú, una barra de herramientas y un panel
 * de contenido central donde se cargan dinámicamente los distintos
 * módulos de la aplicación (productos, ventas, clientes, etc.).
 *
 * El menú y la barra de herramientas se construyen de forma dinámica
 * según el rol del usuario: los empleados solo ven los módulos de
 * productos y ventas, mientras que los administradores también tienen
 * acceso a usuarios, clientes, proveedores e informes.
 *
 * Extiende JFrame para ser la ventana principal de la aplicación.
 */
public class MainView extends JFrame {

    // Panel central donde se cargan dinámicamente los paneles de cada módulo
    private final JPanel contentPanel;
    // Etiqueta en la barra de estado inferior para mostrar mensajes informativos
    private final JLabel statusLabel;

    public MainView() {
        setTitle("AutoStock Sistema de Inventarios");
        setSize(1024, 768);
        // DO_NOTHING_ON_CLOSE permite interceptar el cierre y mostrar un diálogo
        // de confirmación antes de salir, en lugar de cerrar directamente
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // --- Barra de herramientas superior ---
        JToolBar toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);

        // --- Barra de estado inferior ---
        // Muestra el nombre y rol del usuario logueado a la derecha,
        // y mensajes informativos del sistema a la izquierda
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusLabel = new JLabel(" ");
        statusBar.add(statusLabel, BorderLayout.WEST);
        JLabel usuarioLabel = new JLabel(AppState.getInstance().getLoggedInUser().getNombre() + " | " +
                AppState.getInstance().getLoggedInUser().getRol().getNombre() + "    ");
        statusBar.add(usuarioLabel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);

        // --- Barra de menú ---
        setJMenuBar(createMenuBar());

        // --- Panel de contenido central ---
        // Los paneles de cada módulo se cargan aquí dinámicamente al navegar
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(contentPanel, BorderLayout.CENTER);

        // Mostramos el panel de bienvenida al arrancar
        showBienvenidaPanel();

        // Interceptamos el cierre de ventana para mostrar confirmación antes de salir
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cierreExit();
            }
        });
    }

    /**
     * Construye la barra de menú de la aplicación.
     *
     * El menú "Gestiones Admin" solo se añade si el usuario tiene rol
     * de Administrador, ocultando las opciones restringidas a empleados.
     *
     * @return la barra de menú configurada
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        boolean isAdmin = AppState.getInstance().isAdmin();

        // --- Menú Archivo ---
        JMenu archivoMenu = new JMenu("Archivo");
        JMenuItem exitItem = new JMenuItem("Salir");
        exitItem.addActionListener(e -> cierreExit());
        archivoMenu.add(exitItem);
        menuBar.add(archivoMenu);

        // --- Menú Inventario (accesible para todos los roles) ---
        JMenu inventarioMenu = new JMenu("Inventario");
        JMenuItem productosItem = new JMenuItem("Productos");
        productosItem.addActionListener(e -> showProductosPanel());
        inventarioMenu.add(productosItem);
        menuBar.add(inventarioMenu);

        // --- Menú Ventas (accesible para todos los roles) ---
        JMenu ventasMenu = new JMenu("Ventas");
        JMenuItem newVentaItem = new JMenuItem("Nueva venta");
        newVentaItem.addActionListener(e -> showNewVentaPanel());
        ventasMenu.add(newVentaItem);
        JMenuItem historialVentasItem = new JMenuItem("Historial de ventas");
        historialVentasItem.addActionListener(e -> showHistorialVentaPanel());
        ventasMenu.add(historialVentasItem);
        menuBar.add(ventasMenu);

        // --- Menú Gestiones Admin (solo visible para administradores) ---
        if (isAdmin) {
            JMenu adminMenu = new JMenu("Gestiones Admin");

            JMenuItem usuariosItem = new JMenuItem("Usuarios");
            usuariosItem.addActionListener(e -> showUsuariosPanel());
            adminMenu.add(usuariosItem);

            JMenuItem clientesItem = new JMenuItem("Clientes");
            clientesItem.addActionListener(e -> showClientesPanel());
            adminMenu.add(clientesItem);

            JMenuItem proveedoresItem = new JMenuItem("Proveedores");
            proveedoresItem.addActionListener(e -> showProveedoresPanel());
            adminMenu.add(proveedoresItem);

            JMenuItem informesItem = new JMenuItem("Informes");
            informesItem.addActionListener(e -> showInformesPanel());
            adminMenu.add(informesItem);

            menuBar.add(adminMenu);
        }

        // --- Menú Ayuda ---
        JMenu ayudaMenu = new JMenu("Ayuda");
        JMenuItem infoItem = new JMenuItem("Info AutoStock...");
        infoItem.addActionListener(e -> showInfoDialog());
        ayudaMenu.add(infoItem);
        menuBar.add(ayudaMenu);

        return menuBar;
    }

    /**
     * Construye la barra de herramientas de acceso rápido.
     *
     * Incluye los accesos más frecuentes: nueva venta y productos para todos,
     * y usuarios e informes solo para administradores. setFloatable(false)
     * impide que el usuario pueda arrastrar y despegar la barra.
     *
     * @return la barra de herramientas configurada
     */
    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        // Impedimos que el usuario pueda arrastrar la barra de herramientas
        toolBar.setFloatable(false);
        boolean isAdmin = AppState.getInstance().isAdmin();

        // Botones accesibles para todos los roles
        JButton newVentaButton = new JButton("Nueva venta");
        newVentaButton.addActionListener(e -> showNewVentaPanel());
        toolBar.add(newVentaButton);

        JButton productosButton = new JButton("Productos");
        productosButton.addActionListener(e -> showProductosPanel());
        toolBar.add(productosButton);

        toolBar.addSeparator();

        // Botones exclusivos para administradores
        if (isAdmin) {
            JButton usuariosButton = new JButton("Usuarios");
            usuariosButton.addActionListener(e -> showUsuariosPanel());
            toolBar.add(usuariosButton);

            JButton informesButton = new JButton("Informes");
            informesButton.addActionListener(e -> showInformesPanel());
            toolBar.add(informesButton);
        }

        toolBar.addSeparator();

        JButton logoutButton = new JButton("Cierre de sesión");
        logoutButton.addActionListener(e -> cierreLogout());
        toolBar.add(logoutButton);

        return toolBar;
    }

    /**
     * Muestra el panel de bienvenida en el área de contenido central.
     * Es el panel inicial que se muestra al entrar a la aplicación.
     */
    private void showBienvenidaPanel() {
        contentPanel.removeAll();
        JPanel bienvenidaPanel = new JPanel(new BorderLayout());

        JLabel bienvenidaLabel = new JLabel("Bienvenid@ a AutoStock Sistema de Inventarios", SwingConstants.CENTER);
        bienvenidaLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        bienvenidaPanel.add(bienvenidaLabel, BorderLayout.CENTER);

        contentPanel.add(bienvenidaPanel);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Carga el panel de gestión de productos en el área de contenido.
     * Accesible para todos los roles.
     */
    private void showProductosPanel() {
        contentPanel.removeAll();
        contentPanel.add(new ProductosPanel());
        contentPanel.revalidate();
        contentPanel.repaint();
        setStatusMessage("Panel de productos cargado");
    }

    /**
     * Carga el panel de nueva venta en el área de contenido.
     * Accesible para todos los roles.
     */
    private void showNewVentaPanel() {
        contentPanel.removeAll();
        contentPanel.add(new NewVentaPanel());
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Carga el panel de historial de ventas en el área de contenido.
     * Accesible para todos los roles.
     */
    private void showHistorialVentaPanel() {
        contentPanel.removeAll();
        contentPanel.add(new HistorialVentasPanel());
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Carga el panel de gestión de usuarios en el área de contenido.
     * Solo accesible para administradores.
     */
    private void showUsuariosPanel() {
        contentPanel.removeAll();
        contentPanel.add(new UsuariosPanel());
        contentPanel.revalidate();
        contentPanel.repaint();
        setStatusMessage("Panel de usuarios cargado");
    }

    /**
     * Carga el panel de gestión de clientes en el área de contenido.
     * Solo accesible para administradores.
     */
    private void showClientesPanel() {
        contentPanel.removeAll();
        contentPanel.add(new ClientesPanel());
        contentPanel.revalidate();
        contentPanel.repaint();
        setStatusMessage("Panel de clientes cargado");
    }

    /**
     * Carga el panel de gestión de proveedores en el área de contenido.
     * Solo accesible para administradores.
     */
    private void showProveedoresPanel() {
        contentPanel.removeAll();
        contentPanel.add(new ProveedoresPanel());
        contentPanel.revalidate();
        contentPanel.repaint();
        setStatusMessage("Panel de proveedores cargado");
    }

    /**
     * Carga el panel de informes en el área de contenido.
     * Solo accesible para administradores.
     */
    private void showInformesPanel() {
        contentPanel.removeAll();
        contentPanel.add(new InformesPanel());
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Muestra el diálogo de información sobre la aplicación.
     */
    private void showInfoDialog() {
        JOptionPane.showMessageDialog(this,
                "AutoStock Sistema de Inventarios\nVersión 1.0\n© 2024 AutoStock",
                "Sobre nosotros...",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Gestiona el cierre de sesión del usuario.
     *
     * Muestra un diálogo de confirmación antes de cerrar la sesión.
     * Si el usuario confirma, limpia el estado de la sesión en AppState
     * y vuelve a la pantalla de login.
     */
    private void cierreLogout() {
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Estás segur@ de que quieres cerrar sesión?",
                "Confirmar cierre de sesión",
                JOptionPane.YES_NO_OPTION);

        if (respuesta == JOptionPane.YES_OPTION) {
            // Limpiamos el estado de la sesión antes de volver al login
            AppState.getInstance().clearState();
            EventQueue.invokeLater(() -> {
                new LoginView().setVisible(true);
                this.dispose();
            });
        }
    }

    /**
     * Gestiona el cierre de la aplicación.
     *
     * Muestra un diálogo de confirmación antes de salir. Si el usuario
     * confirma, limpia la sesión, cierra la conexión a la base de datos
     * y termina el proceso de la JVM.
     */
    private void cierreExit() {
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Estás segur@ de que quieres salir?",
                "Confirmar salir",
                JOptionPane.YES_NO_OPTION);

        // Si el usuario pulsa "No" o cierra el diálogo con la X, cancelamos la salida
        if (respuesta == JOptionPane.NO_OPTION || respuesta == JOptionPane.CLOSED_OPTION) {
            return;
        }

        // Si el usuario pulsa "Sí", limpiamos la sesión y cerramos la aplicación
        if (respuesta == JOptionPane.YES_OPTION) {
            AppState.getInstance().clearState();
            DatabaseConnection.closeConnection();
            System.exit(0);
        }
    }

    /**
     * Actualiza el mensaje de la barra de estado inferior.
     *
     * @param mensaje texto a mostrar en la barra de estado
     */
    private void setStatusMessage(String mensaje) {
        statusLabel.setText(mensaje);
    }
}