package state;

import models.Usuario;
import java.util.prefs.Preferences;

/**
 * Clase que gestiona el estado global de la sesión activa en la aplicación.
 *
 * Implementa el patrón Singleton: solo puede existir una instancia de esta
 * clase durante toda la ejecución. Esto garantiza que todas las vistas y
 * controladores accedan al mismo estado de sesión sin necesidad de pasarlo
 * como parámetro entre clases.
 *
 * Responsabilidades:
 * - Almacenar el usuario que ha iniciado sesión.
 * - Proporcionar métodos de consulta del rol activo (isAdmin).
 * - Persistir el último email usado mediante java.util.prefs.Preferences,
 * para pre-rellenar el campo email en el próximo arranque de la app.
 */
public class AppState {

    // Instancia única de la clase (patrón Singleton)
    private static AppState instance;
    // Usuario actualmente autenticado en el sistema
    private Usuario loggedInUser;
    // API de Java para guardar preferencias del usuario entre sesiones.
    // Persiste datos en el registro del sistema (Windows) o en archivos
    // de configuración del usuario (Linux/Mac).
    private final Preferences preferences;

    /**
     * Constructor privado para evitar instanciación directa desde fuera.
     * Solo se puede obtener la instancia a través de getInstance().
     */
    private AppState() {
        preferences = Preferences.userNodeForPackage(AppState.class);
    }

    /**
     * Devuelve la instancia única de AppState, creándola si no existe.
     *
     * El modificador synchronized garantiza que en entornos multihilo
     * no se creen dos instancias simultáneamente (thread-safe).
     *
     * @return la instancia única de AppState
     */
    public static synchronized AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return instance;
    }

    /**
     * Devuelve el usuario que ha iniciado sesión actualmente.
     *
     * @return el usuario autenticado, o null si no hay sesión activa
     */
    public Usuario getLoggedInUser() {
        return loggedInUser;
    }

    /**
     * Establece el usuario autenticado y persiste su email en las preferencias.
     *
     * Al guardar el email, la próxima vez que se arranque la app se puede
     * pre-rellenar el campo de email en el formulario de login.
     * Si se pasa null, se limpia la sesión y se elimina el email guardado.
     *
     * @param user usuario autenticado, o null para cerrar la sesión
     */
    public void setLoggedInUser(Usuario user) {
        this.loggedInUser = user;
        if (user != null) {
            // Guardamos el email para pre-rellenarlo en el próximo inicio de sesión
            preferences.put("lastLoggedInEmail", user.getEmail());
        } else {
            preferences.remove("lastLoggedInEmail");
        }
    }

    /**
     * Devuelve el último email usado para iniciar sesión.
     * Se usa para pre-rellenar el campo email en el formulario de login.
     *
     * @return el último email usado, o cadena vacía si no hay ninguno guardado
     */
    public String getLastLoggedInEmail() {
        return preferences.get("lastLoggedInEmail", "");
    }

    /**
     * Indica si hay un usuario autenticado actualmente.
     *
     * @return true si hay sesión activa, false en caso contrario
     */
    public boolean isUserLoggedIn() {
        return loggedInUser != null;
    }

    /**
     * Indica si el usuario autenticado tiene rol de Administrador.
     *
     * Comprueba primero que haya sesión activa antes de acceder al rol,
     * para evitar un NullPointerException si no hay usuario logueado.
     *
     * @return true si el usuario tiene rol "Administrador", false en caso contrario
     */
    public boolean isAdmin() {
        return isUserLoggedIn() && "Administrador".equals(loggedInUser.getRol().getNombre());
    }

    /**
     * Limpia el estado de la sesión al cerrar sesión.
     *
     * Elimina el usuario autenticado y el email guardado en las preferencias,
     * dejando la aplicación en el mismo estado que al arrancar por primera vez.
     */
    public void clearState() {
        loggedInUser = null;
        preferences.remove("lastLoggedInEmail");
    }
}