package lazyHawk.security_service.mensaje;

import java.util.ResourceBundle;

public final class Mensajes {

    private Mensajes() {}

    public static String obtener(String clave) {
        return ResourceBundle.getBundle("ValidationMessages").getString(clave);
    }
}
