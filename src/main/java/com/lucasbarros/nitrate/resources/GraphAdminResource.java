package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.services.GraphExpansionService;
import com.lucasbarros.nitrate.services.GraphExpansionService.ResultadoExpansao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/admin/expand-graph")
public class GraphAdminResource {

    // fica bem abaixo do limite de duração de uma função na Vercel
    private static final Duration ORCAMENTO = Duration.ofSeconds(90);

    @Autowired
    private GraphExpansionService graphExpansionService;

    // GET porque os Cron Jobs da Vercel chamam com GET; POST para chamadas manuais
    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    public ResultadoExpansao expandir() {
        return graphExpansionService.expandirPendentes(ORCAMENTO);
    }
}
