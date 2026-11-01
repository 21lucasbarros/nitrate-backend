package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.services.GraphExpansionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/expand-graph")
public class GraphAdminResource {

    @Autowired
    private GraphExpansionService graphExpansionService;

    // TODO: restringir junto com o resto do /admin antes de ir pra produção.
    // roda em segundo plano; o progresso aparece no log do backend
    @PostMapping
    public ResponseEntity<String> expandir() {
        graphExpansionService.expandirTodasAsSementes();
        return ResponseEntity.accepted().body("Expansão do grafo iniciada a partir dos filmes curtidos/bem avaliados.");
    }
}
