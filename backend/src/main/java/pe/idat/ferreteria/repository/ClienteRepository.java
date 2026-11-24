package pe.idat.ferreteria.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.idat.ferreteria.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    boolean existsByNumeroDocumentoAndIdNot(String numeroDocumento, Long id);

    /** Por parte del DNI/RUC o del nombre, sin distinguir mayúsculas. */
    @Query("""
            select c from Cliente c
            where c.numeroDocumento like concat('%', :texto, '%')
               or lower(c.nombre) like lower(concat('%', :texto, '%'))
            order by c.nombre
            """)
    List<Cliente> buscar(@Param("texto") String texto);
}
