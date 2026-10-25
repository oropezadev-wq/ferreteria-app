package pe.idat.ferreteria.exception;

/** Petición bien formada que rompe una regla del negocio (código repetido, stock insuficiente...). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
