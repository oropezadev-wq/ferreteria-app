package pe.idat.ferreteria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.idat.ferreteria.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<Categoria> findAllByOrderByNombreAsc();
}
