package views.panels;

import controllers.ProductoController;
import controllers.ProveedorController;
import models.Producto;
import models.Proveedor;
import state.AppState;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.List;

/**
 * Panel de gestión de productos del inventario.
 *
 * Muestra todos los productos en una tabla con búsqueda en tiempo real.
 * Los usuarios con rol Administrador pueden añadir, editar y eliminar
 * productos mediante un botón y un menú contextual (clic derecho).
 * Los empleados solo pueden consultar y buscar productos.
 *
 * Extiende JPanel para poder incrustarse en el área de contenido de MainView.
 */
public class ProductosPanel extends JPanel {

    // Controladores para acceder a la lógica de negocio de productos y proveedores
    private final ProductoController productoController;
    private final ProveedorController proveedorController;
    // Tabla que muestra el listado de productos
    private final JTable productosTable;
    // Modelo de datos de la tabla (gestiona filas y columnas)
    private final DefaultTableModel tableModel;
    // Campo de texto para filtrar productos por nombre o descripción
    private final JTextField buscarField;
    // Diálogo reutilizable para crear y editar productos
    private JDialog productoDialog;
    // Producto seleccionado en la tabla para editar o eliminar
    private Producto productoSeleccionado;
    // Menú contextual que aparece al hacer clic derecho sobre un producto
    private final JPopupMenu popupMenu = new JPopupMenu();
    // Indica si el diálogo está en modo edición (true) o creación (false)
    private boolean isEditing = false;

    public ProductosPanel() {
        this.productoController = new ProductoController();
        this.proveedorController = new ProveedorController();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel superior: búsqueda y botón de añadir ---
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        buscarField = new JTextField(20);
        // Permitimos buscar pulsando Intro en el campo de búsqueda
        buscarField.addActionListener(e -> actualizarTabla(buscarField.getText()));

        JButton buscarButton = new JButton("Buscar");
        buscarButton.addActionListener(e -> actualizarTabla(buscarField.getText()));

        JPanel buscarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buscarPanel.add(new JLabel("Buscar:"));
        buscarPanel.add(buscarField);
        buscarPanel.add(buscarButton);

        JButton añadirButton = new JButton("Añadir nuevo producto");
        añadirButton.addActionListener(e -> showProductoDialog(null));

        topPanel.add(buscarPanel, BorderLayout.WEST);
        // El botón de añadir solo es visible para administradores (id_rol = 1)
        if (AppState.getInstance().getLoggedInUser().getRol().getIdRol() == 1) {
            topPanel.add(añadirButton, BorderLayout.EAST);
        }

        // --- Tabla de productos ---
        // isCellEditable devuelve false para que el usuario no pueda editar
        // las celdas directamente; la edición se hace a través del diálogo
        String[] columns = { "ID", "Nombre", "Descripción", "Precio", "Stock", "Proveedor" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        productosTable = new JTable(tableModel);
        // Solo permitimos seleccionar una fila a la vez
        productosTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Ajustamos el ancho preferido de cada columna
        productosTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        productosTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        productosTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        productosTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        productosTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        productosTable.getColumnModel().getColumn(5).setPreferredWidth(150);

        // --- Menú contextual para editar y borrar (solo administradores) ---
        // El menú contextual solo se rellena con opciones si el usuario es
        // administrador.
        // Los empleados no tienen opciones de edición ni borrado.
        if (AppState.getInstance().getLoggedInUser().getRol().getIdRol() == 1) {
            JMenuItem editarItem = new JMenuItem("Editar");
            JMenuItem borrarItem = new JMenuItem("Borrar");

            editarItem.addActionListener(e -> editProductoSeleccionado());
            borrarItem.addActionListener(e -> deleteProductoSeleccionado());

            popupMenu.add(editarItem);
            popupMenu.add(borrarItem);
        }

        // Listener del ratón para mostrar el menú contextual al hacer clic derecho.
        // Se comprueban tanto mousePressed como mouseReleased porque el evento
        // de menú contextual varía según el sistema operativo (Windows vs Mac/Linux).
        productosTable.addMouseListener(new MouseAdapter() {
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
                int row = productosTable.rowAtPoint(e.getPoint());
                if (row >= 0 && row < productosTable.getRowCount()) {
                    // Seleccionamos la fila sobre la que se hizo clic derecho
                    productosTable.setRowSelectionInterval(row, row);
                    // Solo mostramos el menú si tiene elementos (es decir, si es admin)
                    if (popupMenu.getComponentCount() > 0) {
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });

        // --- Ensamblaje del panel principal ---
        add(topPanel, BorderLayout.NORTH);
        // Envolvemos la tabla en un JScrollPane para que tenga barra de desplazamiento
        add(new JScrollPane(productosTable), BorderLayout.CENTER);

        // Cargamos todos los productos al inicializar el panel
        actualizarTabla("");
    }

    /**
     * Actualiza el contenido de la tabla filtrando por el término de búsqueda.
     *
     * Carga todos los productos de la base de datos y filtra en memoria
     * los que contienen el término en el nombre o la descripción.
     * Si el término está vacío, muestra todos los productos.
     *
     * @param buscarTerm texto a buscar (puede estar vacío para mostrar todos)
     */
    private void actualizarTabla(String buscarTerm) {
        // Vaciamos la tabla antes de rellenarla con los nuevos datos
        tableModel.setRowCount(0);
        List<Producto> productos = productoController.findAll();

        for (Producto producto : productos) {
            // Filtramos por nombre o descripción si hay término de búsqueda
            if (buscarTerm.isEmpty() ||
                    producto.getNombre().toLowerCase().contains(buscarTerm.toLowerCase()) ||
                    producto.getDescripcion().toLowerCase().contains(buscarTerm.toLowerCase())) {

                tableModel.addRow(new Object[] {
                        producto.getIdProducto(),
                        producto.getNombre(),
                        producto.getDescripcion(),
                        producto.getPrecio(),
                        producto.getCantidadDisponible(),
                        producto.getProveedor().getNombre()
                });
            }
        }
    }

    /**
     * Muestra el diálogo de creación o edición de un producto.
     *
     * Si se pasa un producto existente, el diálogo se abre en modo edición
     * con los campos pre-rellenados. Si se pasa null, se abre en modo creación.
     *
     * @param producto producto a editar, o null para crear uno nuevo
     */
    private void showProductoDialog(Producto producto) {
        isEditing = (producto != null);
        productoSeleccionado = producto;

        // Creamos un diálogo modal para que el usuario no pueda interactuar
        // con la tabla mientras está editando un producto
        productoDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEditing ? "Editar Producto" : "Nuevo producto",
                true);
        productoDialog.setLayout(new BorderLayout(10, 10));

        // --- Formulario del producto ---
        JPanel formularioPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Campos del formulario
        JTextField nombreField = new JTextField(20);
        JTextArea descripcionArea = new JTextArea(3, 20);
        JScrollPane descScrollPane = new JScrollPane(descripcionArea);
        JTextField precioField = new JTextField(10);
        // JSpinner limita el stock a valores entre 0 y 10000 en incrementos de 1
        JSpinner stockSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        JComboBox<Proveedor> proveedorCombo = new JComboBox<>();

        // Cargamos los proveedores disponibles en el combo
        List<Proveedor> proveedores = proveedorController.findAll();
        for (Proveedor proveedor : proveedores) {
            proveedorCombo.addItem(proveedor);
        }

        // Si estamos editando, pre-rellenamos los campos con los datos actuales
        if (isEditing) {
            nombreField.setText(producto.getNombre());
            descripcionArea.setText(producto.getDescripcion());
            precioField.setText(producto.getPrecio().toString());
            stockSpinner.setValue(producto.getCantidadDisponible());
            // Seleccionamos el proveedor actual en el combo
            for (int i = 0; i < proveedorCombo.getItemCount(); i++) {
                if (proveedorCombo.getItemAt(i).getIdProveedor().equals(producto.getProveedor().getIdProveedor())) {
                    proveedorCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Añadimos los campos al formulario con su etiqueta correspondiente
        gbc.gridx = 0;
        gbc.gridy = 0;
        formularioPanel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(nombreField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formularioPanel.add(new JLabel("Descripción:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(descScrollPane, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formularioPanel.add(new JLabel("Precio:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(precioField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formularioPanel.add(new JLabel("Stock:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(stockSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        formularioPanel.add(new JLabel("Proveedor:"), gbc);
        gbc.gridx = 1;
        formularioPanel.add(proveedorCombo, gbc);

        // --- Botones del diálogo ---
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton guardarButton = new JButton("Guardar");
        JButton cancelarButton = new JButton("Cancelar");

        guardarButton.addActionListener(e -> {
            try {
                // Recogemos los valores del formulario
                String nombre = nombreField.getText().trim();
                String descripcion = descripcionArea.getText().trim();
                String precioText = precioField.getText().trim();
                int stock = (Integer) stockSpinner.getValue();
                Proveedor proveedor = (Proveedor) proveedorCombo.getSelectedItem();

                // Validamos que todos los campos obligatorios estén rellenos
                if (nombre.isEmpty() || descripcion.isEmpty() || precioText.isEmpty() || proveedor == null) {
                    JOptionPane.showMessageDialog(productoDialog,
                            "Por favor, complete todos los campos",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Validamos que el precio sea un número positivo
                BigDecimal precio;
                try {
                    precio = new BigDecimal(precioText);
                    if (precio.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(productoDialog,
                            "Por favor, introduce un precio válido",
                            "Error de validación",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Creamos o actualizamos el producto según el modo del diálogo
                Producto productoToSave = isEditing ? productoSeleccionado : new Producto();
                productoToSave.setNombre(nombre);
                productoToSave.setDescripcion(descripcion);
                productoToSave.setPrecio(precio);
                productoToSave.setCantidadDisponible(stock);
                productoToSave.setProveedor(proveedor);

                if (isEditing) {
                    productoController.update(productoToSave);
                } else {
                    productoController.create(productoToSave);
                }

                // Cerramos el diálogo y refrescamos la tabla
                productoDialog.dispose();
                actualizarTabla(buscarField.getText());

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(productoDialog,
                        "Error guardando el producto: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        // El botón cancelar simplemente cierra el diálogo sin guardar cambios
        cancelarButton.addActionListener(e -> productoDialog.dispose());

        buttonsPanel.add(guardarButton);
        buttonsPanel.add(cancelarButton);

        // Ensamblamos el diálogo
        productoDialog.add(formularioPanel, BorderLayout.CENTER);
        productoDialog.add(buttonsPanel, BorderLayout.SOUTH);

        // pack() ajusta el tamaño del diálogo al contenido
        productoDialog.pack();
        productoDialog.setLocationRelativeTo(this);
        productoDialog.setVisible(true);
    }

    /**
     * Abre el diálogo de edición con el producto seleccionado en la tabla.
     * Si no hay ninguna fila seleccionada, no hace nada.
     */
    private void editProductoSeleccionado() {
        int filaSeleccionada = productosTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            // Obtenemos el ID del producto de la primera columna de la fila seleccionada
            Integer productoId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            Producto producto = productoController.findById(productoId);
            if (producto != null) {
                showProductoDialog(producto);
            }
        }
    }

    /**
     * Elimina el producto seleccionado en la tabla tras pedir confirmación.
     * Si no hay ninguna fila seleccionada, no hace nada.
     */
    private void deleteProductoSeleccionado() {
        int filaSeleccionada = productosTable.getSelectedRow();
        if (filaSeleccionada >= 0) {
            Integer productoId = (Integer) tableModel.getValueAt(filaSeleccionada, 0);
            String productoNombre = (String) tableModel.getValueAt(filaSeleccionada, 1);

            // Pedimos confirmación antes de eliminar para evitar borrados accidentales
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "¿Estás seguro de que deseas eliminar el producto: " + productoNombre + "?",
                    "Confirmar eliminación",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (respuesta == JOptionPane.YES_OPTION) {
                try {
                    productoController.delete(productoId);
                    // Refrescamos la tabla para reflejar el cambio
                    actualizarTabla(buscarField.getText());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                            "Error borrando el producto: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}