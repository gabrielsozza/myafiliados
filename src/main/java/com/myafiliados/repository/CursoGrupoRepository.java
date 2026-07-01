package com.myafiliados.repository;

import com.myafiliados.model.CursoGrupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CursoGrupoRepository extends JpaRepository<CursoGrupo, Long> {
    List<CursoGrupo> findAllByOrderByOrdemAscIdAsc();
    List<CursoGrupo> findByAtivoTrueOrderByOrdemAscIdAsc();
}
