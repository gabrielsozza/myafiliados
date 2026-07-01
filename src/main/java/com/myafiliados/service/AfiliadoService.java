package com.myafiliados.service;

import com.myafiliados.dto.Dtos;
import com.myafiliados.model.Afiliado;
import com.myafiliados.repository.AfiliadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfiliadoService {

    private static final SecureRandom RNG = new SecureRandom();
    private static final String ALFABETO = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final AfiliadoRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final EmailAfiliadoService emailService;

    @Value("${mydelivery.afiliados.comissao-percentual-padrao:33.33}")
    private BigDecimal percentualPadrao;

    @Transactional
    public Afiliado signup(Dtos.SignupRequest req) {
        validar(req);
        if (repo.existsByEmailIgnoreCase(req.email.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe conta com esse email.");
        }
        Afiliado a = new Afiliado();
        a.setCodigo(gerarCodigoUnico());
        a.setNome(req.nome.trim());
        a.setEmail(req.email.trim().toLowerCase());
        a.setSenhaHash(passwordEncoder.encode(req.senha));
        a.setTelefone(req.telefone);
        a.setCpf(req.cpf);
        a.setStatus(Afiliado.Status.PENDENTE);
        a.setComissaoPercentual(percentualPadrao);
        Afiliado salvo = repo.save(a);
        // Email async — jamais bloqueia o cadastro. Se SMTP travar/falhar, só loga.
        emailService.enviarCadastroRecebido(salvo);
        return salvo;
    }

    private void validar(Dtos.SignupRequest r) {
        if (r.nome == null || r.nome.trim().length() < 3) bad("Nome muito curto.");
        if (r.email == null || !r.email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) bad("Email inválido.");
        if (r.senha == null || r.senha.length() < 6) bad("Senha precisa ter pelo menos 6 caracteres.");
    }
    private void bad(String m) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }

    private String gerarCodigoUnico() {
        for (int i = 0; i < 30; i++) {
            StringBuilder sb = new StringBuilder(8);
            for (int j = 0; j < 8; j++) sb.append(ALFABETO.charAt(RNG.nextInt(ALFABETO.length())));
            String c = sb.toString();
            if (!repo.existsByCodigo(c)) return c;
        }
        throw new IllegalStateException("Não foi possível gerar código único");
    }

    @Transactional
    public Afiliado atualizarDadosPix(Long afiliadoId, Dtos.DadosPixRequest req) {
        Afiliado a = repo.findById(afiliadoId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        a.setRecNomeCompleto(safe(req.nomeCompleto));
        a.setRecBanco(safe(req.banco));
        a.setRecChavePix(safe(req.chavePix));
        if (req.tipoChavePix != null && !req.tipoChavePix.isBlank()) {
            try { a.setRecTipoChavePix(Afiliado.TipoChavePix.valueOf(req.tipoChavePix.toUpperCase())); }
            catch (Exception e) { bad("Tipo de chave PIX inválido."); }
        }
        return repo.save(a);
    }

    private String safe(String s) { return s == null ? null : s.trim(); }

    @Transactional
    public Afiliado aprovar(Long id, String adminEmail) {
        Afiliado a = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (a.getStatus() == Afiliado.Status.APROVADO) return a;
        a.setStatus(Afiliado.Status.APROVADO);
        a.setAprovadoEm(LocalDateTime.now());
        a.setAprovadoPor(adminEmail);
        Afiliado salvo = repo.save(a);
        emailService.enviarAprovacao(salvo);
        return salvo;
    }

    @Transactional
    public Afiliado bloquear(Long id, String adminEmail, String motivo) {
        Afiliado a = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        a.setStatus(Afiliado.Status.BLOQUEADO);
        a.setBloqueadoEm(LocalDateTime.now());
        a.setBloqueadoPor(adminEmail);
        a.setMotivoBloqueio(motivo);
        return repo.save(a);
    }

    @Transactional
    public Afiliado desbloquear(Long id) {
        Afiliado a = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        a.setStatus(Afiliado.Status.APROVADO);
        a.setBloqueadoEm(null);
        a.setBloqueadoPor(null);
        a.setMotivoBloqueio(null);
        return repo.save(a);
    }

    @Transactional
    public Afiliado ajustarComissao(Long id, BigDecimal percentual) {
        if (percentual == null || percentual.compareTo(BigDecimal.ZERO) < 0
                || percentual.compareTo(new BigDecimal("100")) > 0) {
            bad("Percentual fora do intervalo 0-100");
        }
        Afiliado a = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        a.setComissaoPercentual(percentual);
        return repo.save(a);
    }

    @Transactional
    public Afiliado alterarSenhaAdmin(Long id, String novaSenha) {
        if (novaSenha == null || novaSenha.length() < 6) bad("Senha precisa 6+ chars.");
        Afiliado a = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        a.setSenhaHash(passwordEncoder.encode(novaSenha));
        return repo.save(a);
    }
}
