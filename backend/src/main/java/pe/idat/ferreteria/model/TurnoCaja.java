package pe.idat.ferreteria.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Turno de caja de un vendedor: se abre con un monto y se cierra con el efectivo contado. */
@Entity
public class TurnoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario usuario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoApertura;

    @Column(precision = 10, scale = 2)
    private BigDecimal montoEsperado;

    @Column(precision = 10, scale = 2)
    private BigDecimal montoContado;

    @Column(precision = 10, scale = 2)
    private BigDecimal diferencia;

    @Column(length = 200)
    private String observacion;

    @Column(nullable = false)
    private boolean abierto = true;

    @Column(nullable = false)
    private LocalDateTime fechaApertura;

    private LocalDateTime fechaCierre;

    protected TurnoCaja() {
    }

    public TurnoCaja(Usuario usuario, BigDecimal montoApertura) {
        this.usuario = usuario;
        this.montoApertura = montoApertura;
        this.fechaApertura = LocalDateTime.now();
    }

    /** El sobrante (positivo) o faltante (negativo) queda guardado con el cierre. */
    public void cerrar(BigDecimal esperado, BigDecimal contado, String observacion) {
        this.montoEsperado = esperado;
        this.montoContado = contado;
        this.diferencia = contado.subtract(esperado);
        this.observacion = observacion;
        this.abierto = false;
        this.fechaCierre = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public BigDecimal getMontoApertura() {
        return montoApertura;
    }

    public BigDecimal getMontoEsperado() {
        return montoEsperado;
    }

    public BigDecimal getMontoContado() {
        return montoContado;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public String getObservacion() {
        return observacion;
    }

    public boolean isAbierto() {
        return abierto;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }
}
