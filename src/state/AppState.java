package state;

import models.Usuario;
import java.util.prefs.Preferences;

public class AppState {
    private static AppState instance;
    private Usuario loggedInUser;
    private final Preferences preferences;

    private AppState() {
        preferences = Preferences.userNodeForPackage(AppState.class);
    }

    public static synchronized AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return instance;
    }

    public Usuario getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(Usuario user) {
        this.loggedInUser = user;
        if (user != null) {
            preferences.put("lastLoggedInEmail", user.getEmail());
        } else {
            preferences.remove("lastLoggedInEmail");
        }
    }

    public String getLastLoggedInEmail() {
        return preferences.get("lastLoggedInEmail", "");
    }

    public boolean isUserLoggedIn() {
        return loggedInUser != null;
    }

    public boolean isAdmin() {
        return isUserLoggedIn() && "Administrador".equals(loggedInUser.getRol().getNombre());
    }

    public void clearState() {
        loggedInUser = null;
        preferences.remove("lastLoggedInEmail");
    }
}