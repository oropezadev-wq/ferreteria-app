package pe.idat.ferreteria.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Cabecera de la venta y de su comprobante (boleta o factura). */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"serie", "numero"}))
public class Venta {

    /** Los precios de venta ya incluyen el IGV (18 %). */
    public static final BigDecimal FACTOR_IGV = new BigDecimal("1.18");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoComprobante tipoComprobante;

    @Column(nullable = false, length = 4)
    private String serie;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne
    private Cliente cliente;

    @ManyToOne(optional = false)
    private Usuario vendedor;

    @ManyToOne
    private TurnoCaja turno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MetodoPago metodoPago;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal gravada = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoVenta estado = EstadoVenta.REGISTRADA;

    /** En esta versión el envío al proveedor PSE queda pendiente. */
    @Column(nullable = false, length = 15)
    private String estadoSunat = "PENDIENTE";

    @Column(length = 150)
    private String motivoAnulacion;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
    private List<DetalleVenta> detalles = new ArrayList<>();

    protected Venta() {
    }

    public Venta(TipoComprobante tipoComprobante, int numero, Cliente cliente, Usuario vendedor, TurnoCaja turno,
                 MetodoPago metodoPago) {
        this.tipoComprobante = tipoComprobante;
        this.serie = tipoComprobante.serie();
        this.numero = numero;
        this.cliente = cliente;
        this.vendedor = vendedor;
        this.turno = turno;
        this.metodoPago = metodoPago;
        this.fecha = LocalDateTime.now();
    }

    /** Agrega una línea y vuelve a calcular el total, la operación gravada y el IGV. */
    public void agregar(Producto producto, int cantidad) {
        detalles.add(new DetalleVenta(this, producto, cantidad));
        total = detalles.stream().map(DetalleVenta::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        gravada = total.divide(FACTOR_IGV, 2, RoundingMode.HALF_UP);
        igv = total.subtract(gravada);
    }

    public void anular(String motivo) {
        this.estado = EstadoVenta.ANULADA;
        this.motivoAnulacion = motivo;
    }

    public boolean isAnulada() {
        return estado == EstadoVenta.ANULADA;
    }

    public String getComprobante() {
        return serie + "-" + numero;
    }

    public Long getId() {
        return id;
    }

    public TipoComprobante getTipoComprobante() {
        return tipoComprobante;
    }

    public String getSerie() {
        return serie;
    }

    public int getNumero() {
        return numero;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Usuario getVendedor() {
        return vendedor;
    }

    public TurnoCaja getTurno() {
        return turno;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public BigDecimal getGravada() {
        return gravada;
    }

    public BigDecimal getIgv() {
        return igv;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public String getEstadoSunat() {
        return estadoSunat;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }
}
