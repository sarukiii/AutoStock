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

/**
 * Panel de creación de nuevas ventas.
 *
 * Implementa un flujo de venta en tres pasos:
 * 1. Seleccionar el cliente al que se le realiza la venta.
 * 2. Añadir productos al carrito con su cantidad.
 * 3. Confirmar y finalizar la venta.
 *
 * El carrito muestra en tiempo real los productos añadidos, sus cantidades,
 * precios unitarios, subtotales y el total acumulado. El stock disponible
 * de cada producto se actualiza dinámicamente para evitar ventas con
 * stock insuficiente.
 *
 * Extiende JPanel para poder incrustarse en el área de contenido de MainView.
 */
public class NewVentaPanel extends JPanel {

    // Controladores para acceder a la lógica de negocio
    private final VentaController ventaController;
    private final ProductoController productoController;
    private final ClienteController clienteController;

    // Componentes del formulario de selección
    private final JComboBox<Cliente> clienteCombo;
    private final JComboBox<Producto> productoCombo;
    // Spinner para seleccionar la cantidad del producto a añadir al carrito
    private final JSpinner cantidadSpinner;
    // Modelo de datos de la tabla del carrito
    private final DefaultTableModel carritoTableModel;
    // Etiqueta que muestra el total acumulado de la venta
    private final JLabel totalLabel;
    private final JButton añadirAlCarritoButton;
    private final JButton retirarDelCarritoButton;
    private final JButton completarVentaButton;

    // Total acumulado de la venta (se recalcula al modificar el carrito)
    private BigDecimal total = BigDecimal.ZERO;
    // Lista de líneas de detalle que forman el carrito de la venta actual
    private final List<DetalleVenta> carritoItems = new ArrayList<>();

    public NewVentaPanel() {
        this.ventaController = new VentaController();
        this.productoController = new ProductoController();
        this.clienteController = new ClienteController();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel superior: selección de cliente ---
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        topPanel.add(new JLabel("Cliente:"), gbc);

        clienteCombo = new JComboBox<>();
        // Cargamos todos los clientes disponibles en el combo
        loadClientes();
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0;
        topPanel.add(clienteCombo, gbc);

        // Botón para crear un nuevo cliente sin salir del panel de venta
        JButton newClienteButton = new JButton("Nuevo cliente");
        newClienteButton.addActionListener(e -> showNewClienteDialog());
        gbc.gridx = 2; gbc.gridy = 0;
        gbc.weightx = 0.0;
        topPanel.add(newClienteButton, gbc);

        // --- Panel de selección de producto y cantidad ---
        JPanel productoPanel = new JPanel(new GridBagLayout());
        productoPanel.setBorder(BorderFactory.createTitledBorder("Añadir productos"));

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        productoPanel.add(new JLabel("Producto:"), gbc);

        productoCombo = new JComboBox<>();
        // Solo cargamos productos con stock disponible mayor que 0
        loadProductos();
        // Al cambiar el producto seleccionado, actualizamos el stock máximo del spinner
        productoCombo.addActionListener(e -> actualizarCantidadDiponible());
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 1.0;
        productoPanel.add(productoCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0.0;
        productoPanel.add(new JLabel("Cantidad:"), gbc);

        // El spinner limita la cantidad al stock disponible del producto seleccionado
        cantidadSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        gbc.gridx = 1; gbc.gridy = 1;
        productoPanel.add(cantidadSpinner, gbc);

        añadirAlCarritoButton = new JButton("Añadir al carrito");
        añadirAlCarritoButton.addActionListener(e -> añadirAlCarrito());
        gbc.gridx = 1; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        productoPanel.add(añadirAlCarritoButton, gbc);

        // --- Tabla del carrito ---
        // isCellEditable devuelve false para que el usuario no pueda editar
        // las celdas directamente; la edición se hace añadiendo/eliminando líneas
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

        // Botón para eliminar el producto seleccionado del carrito
        retirarDelCarritoButton = new JButton("Eliminar producto seleccionado");
        retirarDelCarritoButton.addActionListener(e -> quitarDelCarrito(carritoTable.getSelectedRow()));
        // El botón solo se activa cuando hay una fila seleccionada en el carrito
        retirarDelCarritoButton.setEnabled(false);

        // Activamos el botón de retirar cuando se selecciona una fila del carrito
        carritoTable.getSelectionModel().addListSelectionListener(e ->
                retirarDelCarritoButton.setEnabled(carritoTable.getSelectedRow() != -1));

        // --- Panel inferior: total y botón de finalizar venta ---
        JPanel bottomPanel = new JPanel(new BorderLayout());

        totalLabel = new JLabel("Total: €0.00");
        totalLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        bottomPanel.add(totalLabel, BorderLayout.WEST);

        completarVentaButton = new JButton("Finalizar venta");
        completarVentaButton.addActionListener(e -> completarVenta());
        // El botón solo se activa cuando hay al menos un producto en el carrito
        completarVentaButton.setEnabled(false);
        bottomPanel.add(completarVentaButton, BorderLayout.EAST);

        // --- Ensamblaje del panel principal ---
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

        // Actualizamos el stock disponible para el producto inicialmente seleccionado
        actualizarCantidadDiponible();
    }

    /**
     * Carga todos los clientes disponibles en el combo de selección.
     * Se llama al inicializar el panel y después de crear un nuevo cliente.
     */
    private void loadClientes() {
        clienteCombo.removeAllItems();
        List<Cliente> clientes = clienteController.findAll();
        for (Cliente cliente : clientes) {
            clienteCombo.addItem(cliente);
        }
    }

    /**
     * Carga en el combo solo los productos con stock disponible mayor que 0.
     * Se llama al inicializar el panel y después de completar una venta,
     * para reflejar el stock actualizado.
     */
    private void loadProductos() {
        productoCombo.removeAllItems();
        List<Producto> productos = productoController.findAll();
        for (Producto producto : productos) {
            // Solo mostramos productos con stock disponible
            if (producto.getCantidadDisponible() > 0) {
                productoCombo.addItem(producto);
            }
        }
    }

    /**
     * Actualiza el máximo del spinner de cantidad según el stock del producto seleccionado.
     *
     * Se llama cada vez que el usuario cambia el producto seleccionado en el combo,
     * para evitar que pueda introducir una cantidad mayor que el stock disponible.
     */
    private void actualizarCantidadDiponible() {
        Producto productoSeleccionado = (Producto) productoCombo.getSelectedItem();
        if (productoSeleccionado != null) {
            SpinnerNumberModel model = (SpinnerNumberModel) cantidadSpinner.getModel();
            // Limitamos el máximo del spinner al stock disponible del producto
            model.setMaximum(productoSeleccionado.getCantidadDisponible());
            cantidadSpinner.setValue(1);
            añadirAlCarritoButton.setEnabled(productoSeleccionado.getCantidadDisponible() > 0);
        }
    }

    /**
     * Añade el producto seleccionado al carrito con la cantidad indicada.
     *
     * Si el producto ya está en el carrito, suma la nueva cantidad a la existente,
     * verificando que no supere el stock disponible. Si el producto no estaba
     * en el carrito, crea una nueva línea de detalle.
     */
    private void añadirAlCarrito() {
        Producto producto = (Producto) productoCombo.getSelectedItem();
        int cantidad = (Integer) cantidadSpinner.getValue();

        if (producto != null && cantidad > 0) {
            // Comprobamos si el producto ya está en el carrito para acumular cantidad
            for (DetalleVenta item : carritoItems) {
                if (item.getProducto().getIdProducto().equals(producto.getIdProducto())) {
                    int nuevaCantidad = item.getCantidad() + cantidad;
                    if (nuevaCantidad <= producto.getCantidadDisponible()) {
                        // Actualizamos la cantidad de la línea existente
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

            // El producto no estaba en el carrito: creamos una nueva línea de detalle
            // El precio unitario se fija en el momento de añadir al carrito,
            // preservando el precio histórico aunque cambie posteriormente
            DetalleVenta newItem = new DetalleVenta();
            newItem.setProducto(producto);
            newItem.setCantidad(cantidad);
            newItem.setPrecioUnitario(producto.getPrecio());
            carritoItems.add(newItem);
            actualizarCarritoTable();
        }
    }

    /**
     * Elimina una línea del carrito por su índice en la tabla.
     *
     * @param filaSeleccionada índice de la fila a eliminar (0-based)
     */
    private void quitarDelCarrito(int filaSeleccionada) {
        if (filaSeleccionada >= 0 && filaSeleccionada < carritoItems.size()) {
            carritoItems.remove(filaSeleccionada);
            actualizarCarritoTable();
        }
    }

    /**
     * Recalcula el total y actualiza la tabla del carrito con los datos actuales.
     *
     * Se llama cada vez que se añade o elimina un producto del carrito.
     * También actualiza el estado del botón de finalizar venta y el
     * stock máximo del spinner.
     */
    private void actualizarCarritoTable() {
        // Vaciamos la tabla y recalculamos el total desde cero
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

        // Actualizamos la etiqueta del total con formato de dos decimales
        totalLabel.setText(String.format("Total: €%.2f", total));
        // El botón de finalizar solo se activa si hay al menos un producto en el carrito
        completarVentaButton.setEnabled(!carritoItems.isEmpty());
        // Actualizamos el stock disponible por si ha cambiado
        actualizarCantidadDiponible();
    }

    /**
     * Confirma y registra la venta en la base de datos.
     *
     * Valida que haya un cliente seleccionado y al menos un producto en el carrito,
     * pide confirmación al usuario y llama al controlador para crear la venta.
     * Si la venta se completa correctamente, limpia el formulario para una nueva venta.
     */
    private void completarVenta() {
        // Validamos que haya un cliente seleccionado
        if (clienteCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                    "Por favor seleccione un cliente",
                    "Error de validación",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Pedimos confirmación antes de registrar la venta
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Confirmar venta por " + String.format("%.2f", total) + " €?",
                "Confirmar venta",
                JOptionPane.YES_NO_OPTION);

        if (respuesta == JOptionPane.YES_OPTION) {
            try {
                // Construimos el objeto Venta con los datos del formulario
                Venta venta = new Venta();
                venta.setCliente((Cliente) clienteCombo.getSelectedItem());
                // El usuario que registra la venta es el que está logueado en la sesión
                venta.setUsuario(AppState.getInstance().getLoggedInUser());
                venta.setFecha(LocalDateTime.now());
                venta.setTotal(total);
                venta.setDetalles(new ArrayList<>(carritoItems));

                // Guardamos la venta y sus detalles en la base de datos
                ventaController.createVenta(venta, carritoItems);

                JOptionPane.showMessageDialog(this,
                        "Venta completada correctamente",
                        "Venta finalizada",
                        JOptionPane.INFORMATION_MESSAGE);

                // Limpiamos el formulario para permitir registrar una nueva venta
                carritoItems.clear();
                actualizarCarritoTable();
                // Recargamos productos para reflejar el stock actualizado tras la venta
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

    /**
     * Muestra un diálogo para crear un nuevo cliente sin salir del panel de venta.
     *
     * Permite registrar rápidamente un cliente nuevo durante el proceso de venta.
     * Tras crearlo, lo selecciona automáticamente en el combo de clientes.
     * Los campos obligatorios son nombre y teléfono.
     */
    private void showNewClienteDialog() {
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

        // El asterisco (*) indica que el campo es obligatorio
        gbc.gridx = 0; gbc.gridy = 0;
        formularioPanel.add(new JLabel("Nombre:*"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(nombreField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formularioPanel.add(new JLabel("Teléfono:*"), gbc);
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

                // Validamos que los campos obligatorios estén rellenos
                if (nombre.isEmpty() || telefono.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog,
                            "Se requiere nombre y teléfono",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Creamos y guardamos el nuevo cliente
                Cliente cliente = new Cliente();
                cliente.setNombre(nombre);
                cliente.setTelefono(telefono);
                cliente.setEmail(emailField.getText().trim());
                cliente.setDireccion(direccionField.getText().trim());
                cliente.setEmpresa(empresaField.getText().trim());

                cliente = clienteController.create(cliente);

                // Recargamos el combo y seleccionamos el cliente recién creado
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

        // El botón cancelar simplemente cierra el diálogo sin guardar cambios
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