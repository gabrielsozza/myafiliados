package com.myafiliados.repository;

import com.myafiliados.model.MaterialApoio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialApoioRepository extends JpaRepository<MaterialApoio, Long> {

    List<MaterialApoio> findByAtivoTrueOrderByOrdemAscIdAsc();
    List<MaterialApoio> findAllByOrderByOrdemAscIdAsc();
}
