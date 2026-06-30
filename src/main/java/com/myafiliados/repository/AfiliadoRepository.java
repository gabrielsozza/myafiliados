package com.myafiliados.repository;

import com.myafiliados.model.Afiliado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AfiliadoRepository extends JpaRepository<Afiliado, Long> {

    Optional<Afiliado> findByEmailIgnoreCase(String email);

    /** Lookup pelo slug do link (ex: ?afiliado=ab12cd34). */
    Optional<Afiliado> findByCodigo(String codigo);

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByCodigo(String codigo);

    /** Lista filtrada por status — usado no admin. */
    List<Afiliado> findByStatusOrderByCriadoEmDesc(Afiliado.Status status);
}
