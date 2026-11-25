package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.services.RecommendationEvaluationService;
import com.lucasbarros.nitrate.services.RecommendationEvaluationService.Resultado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Medição da recomendação (protegida pelo segredo do /admin). Exemplos:
 * /admin/recommendations/evaluate                         tudo ligado
 * /admin/recommendations/evaluate?sem=CO_LIKED            sem o "quem curtiu também curtiu"
 * /admin/recommendations/evaluate?sem=NEGATIVOS,GENRE     sem sinais negativos e sem gênero
 */
@RestController
@RequestMapping("/admin/recommendations")
public class RecommendationAdminResource {

    @Autowired
    private RecommendationEvaluationService evaluationService;

    @GetMapping("/evaluate")
    public Resultado avaliar(@RequestParam(defaultValue = "") List<String> sem,
                             @RequestParam(defaultValue = "3") int escondidos,
                             @RequestParam(defaultValue = "300") int maxPessoas) {
        return evaluationService.avaliar(sem, Math.max(1, Math.min(escondidos, 10)), Math.max(1, Math.min(maxPessoas, 2000)));
    }
}
