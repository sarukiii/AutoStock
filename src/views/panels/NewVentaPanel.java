package views.panels;

import controllers.*;
import models.*;
import state.AppState;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NewVentaPanel extends JPanel {
    private final VentaController ventaController;
    private final ProductoController productoController;
    private final ClienteController clienteController;

    private final JComboBox<Cliente> clienteCombo;
    private final JComboBox<Producto> productoCombo;
    private final JSpinner cantidadSpinner;
    private final DefaultTableModel carritoTableModel;
    private final JLabel totalLabel;
    private final JButton añadirAlCarritoButton;
    private final JButton retirarDelCarritoButton;
    private final JButton completarVentaButton;

    private BigDecimal total = BigDecimal.ZERO;
    private final List<DetalleVenta> carritoItems = new ArrayList<>();

    public NewVentaPanel() {
        this.ventaController = new VentaController();
        this.productoController = new ProductoController();
        this.clienteController = new ClienteController();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel superior para seleccionar cliente 
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Selección de clientes
        gbc.gridx = 0; gbc.gridy = 0;
        topPanel.add(new JLabel("Cliente:"), gbc);

        clienteCombo = new JComboBox<>();
        loadClientes();
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0;
        topPanel.add(clienteCombo, gbc);

        JButton newClienteButton = new JButton("Nuevo cliente");
        newClienteButton.addActionListener(e -> showNewClienteDialog());
        gbc.gridx = 2; gbc.gridy = 0;
        gbc.weightx = 0.0;
        topPanel.add(newClienteButton, gbc);

        // Selección de productos
        JPanel productoPanel = new JPanel(new GridBagLayout());
        productoPanel.setBorder(BorderFactory.createTitledBorder("Añadir productos"));

        // Producto combo
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        productoPanel.add(new JLabel("Producto:"), gbc);

        productoCombo = new JComboBox<>();
        loadProductos();
        productoCombo.addActionListener(e -> actualizarCantidadDiponible());
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0;
        productoPanel.add(productoCombo, gbc);

        // Spinner para elegir cantidad
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0.0;
        productoPanel.add(new JLabel("Cantidad:"), gbc);

        cantidadSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        gbc.gridx = 1; gbc.gridy = 1;
        productoPanel.add(cantidadSpinner, gbc);

        // Añadir al carrito button
        añadirAlCarritoButton = new JButton("Añadir al carrito");
        añadirAlCarritoButton.addActionListener(e -> añadirAlCarrito());
        gbc.gridx = 1; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        productoPanel.add(añadirAlCarritoButton, gbc);

        // Carrito table
        String[] columns = {"Producto", "Cantidad", "Precio/unidad", "Subtotal"};
        carritoTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable carritoTable = new JTable(carritoTableModel);
        JScrollPane scrollPane = new JScrollPane(carritoTable);
        carritoTable.getColumnModel().getColumn(0).setPreferredWidth(200);

        // Quitar un producto del carrito
        retirarDelCarritoButton = new JButton("Eliminar producto seleccionado");
        retirarDelCarritoButton.addActionListener(e -> quitarDelCarrito(carritoTable.getSelectedRow()));
        retirarDelCarritoButton.setEnabled(false);

        carritoTable.getSelectionModel().addListSelectionListener(e ->
                retirarDelCarritoButton.setEnabled(carritoTable.getSelectedRow() != -1));

        // Panel inferior con botón de venta total y completar
        JPanel bottomPanel = new JPanel(new BorderLayout());

        totalLabel = new JLabel("Total: €0.00");
        totalLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        bottomPanel.add(totalLabel, BorderLayout.WEST);

        completarVentaButton = new JButton("Finalizar venta");
        completarVentaButton.addActionListener(e -> completarVenta());
        completarVentaButton.setEnabled(false);
        bottomPanel.add(completarVentaButton, BorderLayout.EAST);

        // Layout 
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(productoPanel, BorderLayout.CENTER);

        JPanel carritoPanel = new JPanel(new BorderLayout(5, 5));
        carritoPanel.setBorder(BorderFactory.createTitledBorder("Carrito de compra"));
        carritoPanel.add(scrollPane, BorderLayout.CENTER);
        carritoPanel.add(retirarDelCarritoButton, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.NORTH);
        add(carritoPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        actualizarCantidadDiponible();
    }

    private void loadClientes() {
        clienteCombo.removeAllItems();
        List<Cliente> clientes = clienteController.findAll();
        for (Cliente cliente : clientes) {
            clienteCombo.addItem(cliente);
        }
    }

    private void loadProductos() {
        productoCombo.removeAllItems();
        List<Producto> productos = productoController.findAll();
        for (Producto producto : productos) {
            if (producto.getCantidadDisponible() > 0) {
                productoCombo.addItem(producto);
            }
        }
    }

    private void actualizarCantidadDiponible() {
        Producto productoSeleccionado = (Producto) productoCombo.getSelectedItem();
        if (productoSeleccionado != null) {
            SpinnerNumberModel model = (SpinnerNumberModel) cantidadSpinner.getModel();
            model.setMaximum(productoSeleccionado.getCantidadDisponible());
            cantidadSpinner.setValue(1);
            añadirAlCarritoButton.setEnabled(productoSeleccionado.getCantidadDisponible() > 0);
        }
    }

    private void añadirAlCarrito() {
        Producto producto = (Producto) productoCombo.getSelectedItem();
        int cantidad = (Integer) cantidadSpinner.getValue();

        if (producto != null && cantidad > 0) {
            // Verificar si el producto ya está en el carrito
            for (DetalleVenta item : carritoItems) {
                if (item.getProducto().getIdProducto().equals(producto.getIdProducto())) {
                    int nuevaCantidad = item.getCantidad() + cantidad;
                    if (nuevaCantidad <= producto.getCantidadDisponible()) {
                        item.setCantidad(nuevaCantidad);
                        actualizarCarritoTable();
                        return;
                    } else {
                        JOptionPane.showMessageDialog(this,
                        		"No hay suficiente stock disponible",
                                "Error de stock",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            // Añadir nuevo artículo al carrito
            DetalleVenta newItem = new DetalleVenta();
            newItem.setProducto(producto);
            newItem.setCantidad(cantidad);
            newItem.setPrecioUnitario(producto.getPrecio());
            carritoItems.add(newItem);
            actualizarCarritoTable();
        }
    }

    private void quitarDelCarrito(int filaSeleccionada) {
        if (filaSeleccionada >= 0 && filaSeleccionada < carritoItems.size()) {
            carritoItems.remove(filaSeleccionada);
            actualizarCarritoTable();
        }
    }

    private void actualizarCarritoTable() {
        carritoTableModel.setRowCount(0);
        total = BigDecimal.ZERO;

        for (DetalleVenta item : carritoItems) {
            BigDecimal subtotal = item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
            total = total.add(subtotal);

            carritoTableModel.addRow(new Object[]{
                    item.getProducto().getNombre(),
                    item.getCantidad(),
                    item.getPrecioUnitario(),
                    subtotal
            });
        }

        totalLabel.setText(String.format("Total: €%.2f", total));
        completarVentaButton.setEnabled(!carritoItems.isEmpty());
        actualizarCantidadDiponible();
    }

    private void completarVenta() {
        if (clienteCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
            		"Por favor seleccione un cliente",
                    "Error de validación",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(this,
                "Confirmar venta por " + String.format("%.2f", total) + " €?",
                "Confirmar venta",
                JOptionPane.YES_NO_OPTION);

        if (respuesta == JOptionPane.YES_OPTION) {
            try {
                // Crear venta
                Venta venta = new Venta();
                venta.setCliente((Cliente) clienteCombo.getSelectedItem());
                venta.setUsuario(AppState.getInstance().getLoggedInUser());
                venta.setFecha(LocalDateTime.now());
                venta.setTotal(total);
                venta.setDetalles(new ArrayList<>(carritoItems));

                // Guardar venta
                ventaController.createVenta(venta, carritoItems);

                JOptionPane.showMessageDialog(this,
                		"Venta completada correctamente",
                        "Fin venta",
                        JOptionPane.INFORMATION_MESSAGE);

                // Limpiar formulario
                carritoItems.clear();
                actualizarCarritoTable();
                loadProductos();
                clienteCombo.setSelectedIndex(0);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                		"Error al completar la venta: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showNewClienteDialog() {
        // Cuadro de diálogo simple para agregar un nuevo cliente
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Nuevo cliente", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formularioPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Campos del formulario
        JTextField nombreField = new JTextField(20);
        JTextField telefonoField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField direccionField = new JTextField(20);
        JTextField empresaField = new JTextField(20);

        // Componentes
        gbc.gridx = 0; gbc.gridy = 0;
        formularioPanel.add(new JLabel("Nombre:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(nombreField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formularioPanel.add(new JLabel("Telefono:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(telefonoField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formularioPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formularioPanel.add(new JLabel("Dirección:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(direccionField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formularioPanel.add(new JLabel("Empresa:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(empresaField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton guardarButton = new JButton("Guardar");
        JButton cancelarButton = new JButton("Cancelar");

        guardarButton.addActionListener(e -> {
            try {
                String nombre = nombreField.getText().trim();
                String telefono = telefonoField.getText().trim();

                if (nombre.isEmpty() || telefono.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog,
                    		"Se requiere nombre y teléfono",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Cliente cliente = new Cliente();
                cliente.setNombre(nombre);
                cliente.setTelefono(telefono);
                cliente.setEmail(emailField.getText().trim());
                cliente.setDireccion(direccionField.getText().trim());
                cliente.setEmpresa(empresaField.getText().trim());

                cliente = clienteController.create(cliente);
                loadClientes();
                clienteCombo.setSelectedItem(cliente);
                dialog.dispose();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                		"Error al crear el cliente: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelarButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(guardarButton);
        buttonPanel.add(cancelarButton);

        dialog.add(formularioPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}