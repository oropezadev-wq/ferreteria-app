package pe.idat.ferreteria.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.idat.ferreteria.model.Venta;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    /** Último número usado en la serie; 0 si todavía no hay comprobantes. */
    @Query("select coalesce(max(v.numero), 0) from Venta v where v.serie = :serie")
    int ultimoNumero(@Param("serie") String serie);

    List<Venta> findByFechaGreaterThanEqualAndFechaLessThanOrderByFechaDesc(LocalDateTime desde,
                                                                             LocalDateTime hasta);

    List<Venta> findByTurnoId(Long turnoId);
}
