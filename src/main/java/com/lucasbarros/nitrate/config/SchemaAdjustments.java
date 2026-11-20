package com.lucasbarros.nitrate.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ajustes de schema que o ddl-auto=update não faz: ele cria tabelas e colunas novas, mas
 * nunca altera o tipo de uma coluna existente. Cada ajuste confere o estado atual antes,
 * então rodar de novo a cada inicialização não muda nada.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // o banco precisa estar ajustado antes de qualquer outro passo de inicialização
public class SchemaAdjustments implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaAdjustments.class);

    private final JdbcTemplate jdbc;

    public SchemaAdjustments(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // tb_review.text era varchar(5000); reviews importadas do Letterboxd passam disso
        List<String> tipo = jdbc.queryForList(
                "select data_type from information_schema.columns " +
                "where table_name = 'tb_review' and column_name = 'text'", String.class);
        if (!tipo.isEmpty() && !"text".equals(tipo.get(0))) {
            jdbc.execute("alter table tb_review alter column text type text");
            log.info("tb_review.text convertida de {} para text", tipo.get(0));
        }

        // tb_review tinha unique(user_id, movie_id); agora pode haver várias reviews do mesmo
        // filme (reassistidos). A tabela não tem outra restrição única, então removemos todas.
        List<String> unicas = jdbc.queryForList(
                "select conname from pg_constraint where conrelid = 'tb_review'::regclass and contype = 'u'",
                String.class);
        for (String nome : unicas) {
            // o nome vem do catálogo do Postgres, não do usuário; mesmo assim, aspas dobradas
            // garantem que ele nunca saia do identificador
            jdbc.execute("alter table tb_review drop constraint \"" + nome.replace("\"", "\"\"") + "\"");
            log.info("restrição única {} removida de tb_review (reviews de reassistidos)", nome);
        }

        // o nome de usuário passou a aceitar maiúsculas: a unicidade continua sem diferenciá-las
        // ("Ana" e "ana" não podem coexistir), garantida também pelo banco
        jdbc.execute("create unique index if not exists ux_user_username_lower on tb_user (lower(username))");

        // notificações de "começou a seguir" não têm filme: movie_id deixou de ser obrigatório
        List<String> filmeObrigatorio = jdbc.queryForList(
                "select is_nullable from information_schema.columns " +
                "where table_name = 'tb_notification' and column_name = 'movie_id'", String.class);
        if (!filmeObrigatorio.isEmpty() && "NO".equals(filmeObrigatorio.get(0))) {
            jdbc.execute("alter table tb_notification alter column movie_id drop not null");
            log.info("tb_notification.movie_id agora aceita nulo (notificações de seguidor)");
        }

        // o Hibernate criou um check com os valores do enum (CINEMA, STREAMING) e não o atualiza;
        // sem removê-lo, o tipo FOLLOW seria recusado pelo banco
        List<String> checks = jdbc.queryForList(
                "select conname from pg_constraint where conrelid = 'tb_notification'::regclass " +
                "and contype = 'c' and pg_get_constraintdef(oid) like '%STREAMING%' " +
                "and pg_get_constraintdef(oid) not like '%FOLLOW%'",
                String.class);
        for (String nome : checks) {
            jdbc.execute("alter table tb_notification drop constraint \"" + nome.replace("\"", "\"\"") + "\"");
            log.info("check {} removido de tb_notification (novo tipo FOLLOW)", nome);
        }
    }
}
