import views.LoginView;

/**
 * Punto de entrada de la aplicación AutoStock.
 *
 * Esta clase contiene únicamente el método main, que es el que ejecuta
 * la JVM al arrancar el programa. Su única responsabilidad es crear la
 * ventana de login y hacerla visible, delegando toda la lógica en las
 * capas de vistas y controladores.
 */
public class Main {

    /**
     * Método principal de la aplicación.
     *
     * Crea una instancia de LoginView (la ventana de inicio de sesión)
     * y la hace visible. A partir de aquí, el flujo de la aplicación
     * lo gestiona la interfaz gráfica Swing mediante eventos de usuario.
     *
     * @param args argumentos de línea de comandos (no se utilizan)
     */
    public static void main(String[] args) {
        LoginView loginView = new LoginView();
        loginView.setVisible(true);
    }
}