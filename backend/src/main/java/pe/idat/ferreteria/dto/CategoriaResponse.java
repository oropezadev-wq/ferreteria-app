package pe.idat.ferreteria.dto;

import pe.idat.ferreteria.model.Categoria;

public record CategoriaResponse(Long id, String nombre) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre());
    }
}
