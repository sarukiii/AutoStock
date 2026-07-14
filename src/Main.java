import java.awt.EventQueue;
import javax.swing.UIManager;
import com.formdev.flatlaf.FlatDarkLaf;
import views.LoginView;

/**
 * Punto de entrada de la aplicación AutoStock.
 *
 * Esta clase contiene únicamente el método main, que es el que ejecuta
 * la JVM al arrancar el programa. Su única responsabilidad es configurar
 * el aspecto visual de la aplicación y crear la ventana de login,
 * delegando toda la lógica en las capas de vistas y controladores.
 */
public class Main {

    /**
     * Método principal de la aplicación.
     *
     * Aplica el tema FlatDarkLaf antes de crear cualquier componente Swing,
     * ya que el Look and Feel debe establecerse antes de que se instancie
     * cualquier ventana para que el tema se aplique correctamente.
     *
     * @param args argumentos de línea de comandos (no se utilizan)
     */
    public static void main(String[] args) {
        try {
            // FlatDarkLaf proporciona un tema oscuro moderno para aplicaciones Swing.
            // Es una librería externa que transforma el aspecto por defecto de Java
            // en una interfaz similar a la de herramientas profesionales como IntelliJ
            // IDEA.
            UIManager.setLookAndFeel(new FlatDarkLaf());

        } catch (Exception e) {
            // Si FlatLaf no está disponible, la app sigue funcionando
            // con el Look and Feel por defecto del sistema operativo.
            e.printStackTrace();
        }

        // Lanzamos la ventana de login en el hilo de eventos de Swing (EDT).
        // Es importante crear componentes Swing solo desde el EDT para evitar
        // problemas de concurrencia en la interfaz gráfica.
        EventQueue.invokeLater(() -> {
            new LoginView().setVisible(true);
        });
    }
}