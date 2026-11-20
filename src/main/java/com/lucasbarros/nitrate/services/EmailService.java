package com.lucasbarros.nitrate.services;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Envio de e-mails pelo Resend. Sem RESEND_API_KEY (desenvolvimento local), o e-mail não sai:
 * o conteúdo vai para o log, para dar para testar o fluxo copiando o link de lá.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final Resend resend;
    private final String remetente;

    public EmailService(@Value("${resend.api-key:}") String apiKey,
                        @Value("${email.from:Nitrate <onboarding@resend.dev>}") String remetente) {
        this.resend = apiKey == null || apiKey.isBlank() ? null : new Resend(apiKey);
        this.remetente = remetente;
    }

    /**
     * Envia em uma virtual thread e volta na hora: a resposta da API não espera o Resend, e o
     * tempo de resposta não denuncia se o e-mail existe ou não (ver PasswordResetService).
     */
    public void enviarEmSegundoPlano(String para, String assunto, String html, String texto) {
        Thread.startVirtualThread(() -> enviar(para, assunto, html, texto));
    }

    void enviar(String para, String assunto, String html, String texto) {
        if (resend == null) {
            log.warn("RESEND_API_KEY não configurada; e-mail para {} não enviado. Assunto: {}\n{}", para, assunto, texto);
            return;
        }
        CreateEmailOptions email = CreateEmailOptions.builder()
                .from(remetente)
                .to(para)
                .subject(assunto)
                .html(html)
                .text(texto)
                .build();
        try {
            resend.emails().send(email);
        } catch (ResendException e) {
            // quem pediu já recebeu a resposta; só dá para registrar a falha
            log.error("Falha ao enviar e-mail \"{}\" pelo Resend: {}", assunto, e.getMessage());
        }
    }
}
