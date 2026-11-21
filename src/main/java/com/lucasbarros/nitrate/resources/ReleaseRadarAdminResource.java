package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.services.ReleaseRadarService;
import com.lucasbarros.nitrate.services.SeriesRadarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/admin/release-radar")
public class ReleaseRadarAdminResource {

    // juntos ficam bem abaixo do limite de duração de uma função na Vercel. As séries vêm antes,
    // com uma fatia menor: é uma consulta ao TMDB por série, e as que não couberem hoje são as
    // primeiras amanhã
    private static final Duration ORCAMENTO_SERIES = Duration.ofSeconds(25);
    private static final Duration ORCAMENTO_FILMES = Duration.ofSeconds(65);

    @Autowired
    private ReleaseRadarService releaseRadarService;

    @Autowired
    private SeriesRadarService seriesRadarService;

    public record Resultado(ReleaseRadarService.ResultadoRadar filmes, SeriesRadarService.ResultadoRadar series) {}

    // GET porque os Cron Jobs da Vercel chamam com GET; POST para chamadas manuais
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    public Resultado verificar() {
        SeriesRadarService.ResultadoRadar series = seriesRadarService.verificar(ORCAMENTO_SERIES);
        return new Resultado(releaseRadarService.verificar(ORCAMENTO_FILMES), series);
    }
}
