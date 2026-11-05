package pe.idat.ferreteria.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * Una fila del kardex: cada cambio de stock queda registrado y no se edita después.
 * Se crea solo desde las fábricas del paquete service.kardex.
 */
@Entity
public class MovimientoKardex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    /** Cantidad con signo: positiva si entra mercadería, negativa si sale. */
    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false)
    private int stockAnterior;

    @Column(nullable = false)
    private int stockResultante;

    @Column(nullable = false, length = 150)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Documento del proveedor; solo lo llevan las entradas. */
    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private TipoDocumento tipoDocumento;

    @Column(length = 20)
    private String numeroDocumento;

    private LocalDate fechaDocumento;

    @Column(length = 120)
    private String proveedor;

    protected MovimientoKardex() {
    }

    public MovimientoKardex(Producto producto, TipoMovimiento tipo, int cantidad,
                            int stockAnterior, String motivo) {
        this.producto = producto;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.stockAnterior = stockAnterior;
        this.stockResultante = stockAnterior + cantidad;
        this.motivo = motivo;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public int getStockAnterior() {
        return stockAnterior;
    }

    public int getStockResultante() {
        return stockResultante;
    }

    public String getMotivo() {
        return motivo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void asignarDocumento(DocumentoIngreso documento) {
        this.tipoDocumento = documento.tipo();
        this.numeroDocumento = documento.numero();
        this.fechaDocumento = documento.fecha();
        this.proveedor = documento.proveedor();
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public LocalDate getFechaDocumento() {
        return fechaDocumento;
    }

    public String getProveedor() {
        return proveedor;
    }
}
