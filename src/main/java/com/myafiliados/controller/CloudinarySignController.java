package com.myafiliados.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Gera signatures pra upload direto do browser pro Cloudinary — assim o
 * arquivo NÃO passa pelo nosso backend (evita banda, memória, timeouts).
 *
 * Fluxo:
 *   1. Frontend chama POST /api/admin/cloudinary/sign com { folder, resourceType }
 *   2. Backend devolve { signature, timestamp, apiKey, cloudName, uploadUrl, params }
 *   3. Frontend faz POST multipart pra uploadUrl com o file + params + signature
 *   4. Cloudinary devolve secure_url — frontend salva no CursoModulo.videoUrl
 *
 * Somente ADMIN_AFILIADOS pode assinar (só admin sobe vídeo/anexo).
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/cloudinary")
@PreAuthorize("hasRole('ADMIN_AFILIADOS')")
public class CloudinarySignController {

    @Value("${cloudinary.cloud-name:${CLOUDINARY_CLOUD_NAME:}}")
    private String cloudName;

    @Value("${cloudinary.api-key:${CLOUDINARY_API_KEY:}}")
    private String apiKey;

    @Value("${cloudinary.api-secret:${CLOUDINARY_API_SECRET:}}")
    private String apiSecret;

    @PostMapping("/sign")
    public ResponseEntity<Map<String, Object>> assinar(@RequestBody Map<String, Object> body) {
        if (cloudName == null || cloudName.isBlank()
                || apiKey == null || apiKey.isBlank()
                || apiSecret == null || apiSecret.isBlank()) {
            return ResponseEntity.status(500).body(Map.of(
                    "erro", "Cloudinary não configurado — setar CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET"));
        }

        // Parâmetros que serão enviados no upload — precisam ser assinados JUNTOS.
        // Ordenado alfabeticamente pela chave (regra do Cloudinary).
        TreeMap<String, String> params = new TreeMap<>();
        long timestamp = System.currentTimeMillis() / 1000L;
        params.put("timestamp", String.valueOf(timestamp));

        // Folder organizado por tipo → "afiliados/cursos", "afiliados/anexos", etc.
        String folder = String.valueOf(body.getOrDefault("folder", "afiliados/cursos"));
        params.put("folder", folder);

        // resourceType (video/image/raw): não entra na signature, vai só na URL
        String resourceType = String.valueOf(body.getOrDefault("resourceType", "auto"));
        if (!resourceType.matches("^(auto|image|video|raw)$")) resourceType = "auto";

        // Assinatura: SHA1 de "chave1=valor1&chave2=valor2..." + apiSecret
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var e : params.entrySet()) {
            if (!first) sb.append("&");
            sb.append(e.getKey()).append("=").append(e.getValue());
            first = false;
        }
        sb.append(apiSecret);
        String signature = sha1Hex(sb.toString());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("cloudName", cloudName);
        out.put("apiKey", apiKey);
        out.put("timestamp", timestamp);
        out.put("signature", signature);
        out.put("folder", folder);
        out.put("resourceType", resourceType);
        out.put("uploadUrl", "https://api.cloudinary.com/v1_1/" + cloudName + "/" + resourceType + "/upload");
        return ResponseEntity.ok(out);
    }

    private static String sha1Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar SHA-1", e);
        }
    }
}
