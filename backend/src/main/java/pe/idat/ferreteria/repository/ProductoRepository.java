package pe.idat.ferreteria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.idat.ferreteria.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    /** Búsqueda del mostrador: por parte del código o de la descripción, sin distinguir mayúsculas. */
    @Query("""
            select p from Producto p
            where (:soloActivos = false or p.activo = true)
              and (lower(p.codigo) like lower(concat('%', :texto, '%'))
                   or lower(p.descripcion) like lower(concat('%', :texto, '%')))
            order by p.descripcion
            """)
    List<Producto> buscar(@Param("texto") String texto, @Param("soloActivos") boolean soloActivos);

    @Query("select p from Producto p where p.activo = true and p.stock <= p.stockMinimo order by p.stock")
    List<Producto> conStockBajo();

    List<Producto> findByActivoTrue();
}
