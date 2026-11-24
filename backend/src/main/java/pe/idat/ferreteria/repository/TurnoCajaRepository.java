package pe.idat.ferreteria.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.idat.ferreteria.model.TurnoCaja;

public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {

    Optional<TurnoCaja> findByUsuarioUsuarioAndAbiertoTrue(String usuario);
}
