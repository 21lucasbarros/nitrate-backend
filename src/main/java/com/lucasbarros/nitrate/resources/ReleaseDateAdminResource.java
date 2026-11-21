package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.services.ReleaseDateBackfillService;
import com.lucasbarros.nitrate.services.ReleaseDateBackfillService.ResultadoDatas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Preenche a data de estreia dos filmes antigos. Roda sozinho em segundo plano quando a API
 * sobe e também pode ser chamado em /admin/release-dates (protegido pelo segredo do /admin).
 */
@RestController
@RequestMapping("/admin/release-dates")
public class ReleaseDateAdminResource implements ApplicationRunner {

    // limita cada chamada; o que faltar continua na próxima
    private static final Duration ORCAMENTO = Duration.ofSeconds(90);
    // na inicialização não há quem espere a resposta: dá para ir mais longe
    private static final Duration ORCAMENTO_INICIALIZACAO = Duration.ofMinutes(15);

    @Autowired
    private ReleaseDateBackfillService releaseDateBackfillService;

    @Override
    public void run(ApplicationArguments args) {
        releaseDateBackfillService.preencherEmSegundoPlano(ORCAMENTO_INICIALIZACAO);
    }

    // GET para as tarefas agendadas; POST para chamadas manuais
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    public ResultadoDatas preencher() {
        return releaseDateBackfillService.preencher(ORCAMENTO);
    }
}
