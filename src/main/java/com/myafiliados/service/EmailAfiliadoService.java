package com.myafiliados.service;

import com.myafiliados.model.Afiliado;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Templates de email do programa de afiliados.
 * Usa SMTP Gmail (senha de app fornecida pelo dono).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAfiliadoService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:gpsozza3@gmail.com}")
    private String remetente;

    @Value("${mydelivery.afiliados.painel-url:https://afiliados.mydeliveryfood.com.br}")
    private String painelUrl;

    /**
     * ASYNC — não bloqueia o cadastro do afiliado. Se o SMTP travar ou
     * a config estiver errada, o log registra mas o request principal
     * já retornou 200 ao cliente.
     */
    @Async
    public void enviarCadastroRecebido(Afiliado a) {
        String html = baseHtml("Recebemos seu cadastro!",
            "Olá <b>" + escape(a.getNome()) + "</b>,<br><br>" +
            "Recebemos sua solicitação para participar do programa de afiliados do <b>MyDelivery</b>." +
            "<br><br>Nossa equipe vai analisar seu cadastro nas próximas horas. Assim que aprovado, " +
            "você recebe outro email com o link de acesso ao painel.",
            null, null);
        enviar(a.getEmail(), "MyDelivery Afiliados — Cadastro recebido", html);
    }

    @Async
    public void enviarAprovacao(Afiliado a) {
        String html = baseHtml("Seu cadastro foi aprovado!",
            "Olá <b>" + escape(a.getNome()) + "</b>,<br><br>" +
            "Boas notícias! Seu cadastro no programa de afiliados <b>MyDelivery</b> foi aprovado. " +
            "Agora você pode acessar o painel e começar a divulgar usando seu link único.<br><br>" +
            "Seu código de afiliado: <b style=\"font-family:monospace;background:#fff7ed;padding:4px 10px;border-radius:6px;color:#c2410c\">" +
            escape(a.getCodigo()) + "</b>",
            "Acessar painel", painelUrl);
        enviar(a.getEmail(), "MyDelivery Afiliados — Bem-vindo!", html);
    }

    private void enviar(String para, String assunto, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(remetente, "MyDelivery Afiliados");
            h.setTo(para);
            h.setSubject(assunto);
            h.setText(htmlBody, true);
            mailSender.send(msg);
            log.info("[Email] enviado pra {} | assunto: {}", para, assunto);
        } catch (Exception e) {
            log.warn("[Email] falha ao enviar pra {}: {}", para, e.getMessage());
        }
    }

    private String baseHtml(String titulo, String corpoHtml, String btnTexto, String btnUrl) {
        StringBuilder b = new StringBuilder();
        b.append("<!doctype html><html><body style=\"margin:0;padding:0;background:#f5f5f5;font-family:Arial,sans-serif\">");
        b.append("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"padding:24px 16px\"><tr><td align=\"center\">");
        b.append("<table width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#fff;border-radius:14px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,.06)\">");
        b.append("<tr><td style=\"background:linear-gradient(135deg,#ff8a3b,#ff5500);padding:24px 28px;color:#fff\">");
        b.append("<div style=\"font-size:13px;font-weight:700;letter-spacing:.18em;text-transform:uppercase;opacity:.85\">MyDelivery</div>");
        b.append("<div style=\"font-size:22px;font-weight:800;margin-top:4px\">").append(titulo).append("</div>");
        b.append("</td></tr><tr><td style=\"padding:28px;color:#333;line-height:1.6;font-size:14.5px\">");
        b.append(corpoHtml);
        if (btnTexto != null && btnUrl != null) {
            b.append("<div style=\"margin-top:24px\"><a href=\"").append(btnUrl)
             .append("\" style=\"background:#ff5500;color:#fff;text-decoration:none;padding:13px 26px;border-radius:10px;font-weight:700;display:inline-block\">")
             .append(btnTexto).append("</a></div>");
        }
        b.append("</td></tr><tr><td style=\"padding:16px 28px;background:#fafafa;color:#999;font-size:11.5px;border-top:1px solid #eee\">");
        b.append("Você está recebendo este email porque se cadastrou no programa de afiliados do MyDelivery.");
        b.append("</td></tr></table></td></tr></table></body></html>");
        return b.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }
}
