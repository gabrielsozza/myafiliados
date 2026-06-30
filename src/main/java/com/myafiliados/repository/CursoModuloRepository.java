package com.myafiliados.repository;

import com.myafiliados.model.CursoModulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CursoModuloRepository extends JpaRepository<CursoModulo, Long> {

    /** Listagem pro afiliado — só ativos, na ordem definida pelo admin. */
    List<CursoModulo> findByAtivoTrueOrderByOrdemAscIdAsc();

    /** Listagem pro admin — inclui inativos. */
    List<CursoModulo> findAllByOrderByOrdemAscIdAsc();
}
