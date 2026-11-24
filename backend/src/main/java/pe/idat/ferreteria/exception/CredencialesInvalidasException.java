package pe.idat.ferreteria.exception;

/** Usuario inexistente, desactivado o contraseña incorrecta: se responde lo mismo en los tres casos. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Usuario o contraseña incorrectos");
    }
}
