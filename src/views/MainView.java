package views;

import config.DatabaseConnection;
import state.AppState;
import views.panels.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainView extends JFrame {
    private final JPanel contentPanel;
    private final JLabel statusLabel;

    public MainView() {
        setTitle("AutoStock Sistema de Inventarios");
        setSize(1024, 768);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Diseño principal
        setLayout(new BorderLayout());

        // Barra de herramientas
        JToolBar toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);

        // StatusBar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusLabel = new JLabel(" ");
        statusBar.add(statusLabel, BorderLayout.WEST);
        JLabel usuarioLabel = new JLabel(AppState.getInstance().getLoggedInUser().getNombre() + " | " +
                AppState.getInstance().getLoggedInUser().getRol().getNombre() + "    ");
        statusBar.add(usuarioLabel, BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);

        // Menú principal
        setJMenuBar(createMenuBar());

        // Panel contenedor
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(contentPanel, BorderLayout.CENTER);

        // Panel de bienvenida
        showBienvenidaPanel();

        // Cerrar de la ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cierreExit();
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        boolean isAdmin = AppState.getInstance().isAdmin();

        // Menu 'Archivo'
        JMenu archivoMenu = new JMenu("Archivo");
        JMenuItem exitItem = new JMenuItem("Salir");
        exitItem.addActionListener(e -> cierreExit());
        archivoMenu.add(exitItem);
        menuBar.add(archivoMenu);

        // Menú de inventario
        JMenu inventarioMenu = new JMenu("Inventario");
        JMenuItem productosItem = new JMenuItem("Productos");
        productosItem.addActionListener(e -> showProductosPanel());
        inventarioMenu.add(productosItem);
        menuBar.add(inventarioMenu);

        // Menú ventas
        JMenu ventasMenu = new JMenu("Ventas");
        JMenuItem newVentaItem = new JMenuItem("Nueva venta");
        newVentaItem.addActionListener(e -> showNewVentaPanel());
        ventasMenu.add(newVentaItem);
        JMenuItem historialVentasItem = new JMenuItem("Historial de ventas");
        historialVentasItem.addActionListener(e -> showHistorialVentaPanel());
        ventasMenu.add(historialVentasItem);
        menuBar.add(ventasMenu);

        // Menú administradores (solo visible para el rol de administrador)
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

        // Menú ayuda
        JMenu ayudaMenu = new JMenu("Ayuda");
        JMenuItem infoItem = new JMenuItem("Info AutoStock...");
        infoItem.addActionListener(e -> showInfoDialog());
        ayudaMenu.add(infoItem);
        menuBar.add(ayudaMenu);

        return menuBar;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        boolean isAdmin = AppState.getInstance().isAdmin();

        // Botones barra herramientas
        JButton newVentaButton = new JButton("Nueva venta");
        newVentaButton.addActionListener(e -> showNewVentaPanel());
        toolBar.add(newVentaButton);

        JButton productosButton = new JButton("Productos");
        productosButton.addActionListener(e -> showProductosPanel());
        toolBar.add(productosButton);

        toolBar.addSeparator();

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

    private void showProductosPanel() {
        contentPanel.removeAll();
        contentPanel.add(new ProductosPanel());
        contentPanel.revalidate();
        contentPanel.repaint();
        setStatusMessage("Panel de productos cargado");
    }

    private void showNewVentaPanel() {
        contentPanel.removeAll();
        contentPanel.add(new NewVentaPanel());
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showHistorialVentaPanel() {
        contentPanel.removeAll();
        contentPanel.add(new HistorialVentasPanel());
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showUsuariosPanel() {
        contentPanel.removeAll();
        contentPanel.add(new UsuariosPanel());
        contentPanel.revalidate();
        contentPanel.repaint();
        setStatusMessage("Panel de usuarios cargado");
    }

    private void showClientesPanel() {
        contentPanel.removeAll();
        contentPanel.add(new ClientesPanel());
        contentPanel.revalidate();
        contentPanel.repaint();
        setStatusMessage("Panel de clientes cargado");
    }

    private void showProveedoresPanel() {
        contentPanel.removeAll();
        contentPanel.add(new ProveedoresPanel());
        contentPanel.revalidate();
        contentPanel.repaint();
        setStatusMessage("Panel de proveedores cargado");
    }

    private void showInformesPanel() {
        contentPanel.removeAll();
        contentPanel.add(new InformesPanel());
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showInfoDialog() {
        JOptionPane.showMessageDialog(this,
                "AutoStock Sitema de Inventarios\\nVersion 1.0\\n© 2024 AutoStock",
                "Sobre nosotros...",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void cierreLogout() {
        int respuesta = JOptionPane.showConfirmDialog(this,
        		"¿Estás segur@ de que quieres cerrar sesión?",
                "Confirmar cierre de sesión",
                JOptionPane.YES_NO_OPTION);

        if (respuesta == JOptionPane.YES_OPTION) {
            AppState.getInstance().clearState();
            EventQueue.invokeLater(() -> {
                new LoginView().setVisible(true);
                this.dispose();
            });
        }
    }

    private void cierreExit() {
        int respuesta = JOptionPane.showConfirmDialog(this,
        		"¿Estás segur@ de que quieres salir??",
                "Confirmar salir",
                JOptionPane.YES_NO_OPTION);

        // Pinchar "No" o cerrar cuadro de diálogo mediante "X"
        if (respuesta == JOptionPane.NO_OPTION || respuesta == JOptionPane.CLOSED_OPTION) {
            System.out.println("Salida cancelada");
            return;
        }

        // Pinchar "Si"
        if (respuesta == JOptionPane.YES_OPTION) {
            AppState.getInstance().clearState();
            DatabaseConnection.closeConnection();
            System.exit(0);
        }
    }

    private void setStatusMessage(String mensaje) {
        statusLabel.setText(mensaje);
    }
}