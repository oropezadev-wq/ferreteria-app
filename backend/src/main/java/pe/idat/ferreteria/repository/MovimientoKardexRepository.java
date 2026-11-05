package pe.idat.ferreteria.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.idat.ferreteria.model.MovimientoKardex;

public interface MovimientoKardexRepository extends JpaRepository<MovimientoKardex, Long> {

    List<MovimientoKardex> findByProductoIdOrderByFechaDescIdDesc(Long productoId);

    List<MovimientoKardex> findTop50ByOrderByFechaDescIdDesc();

    List<MovimientoKardex> findByNumeroDocumentoIgnoreCaseOrderByFechaDescIdDesc(String numeroDocumento);

    long countByFechaAfter(LocalDateTime desde);
}
