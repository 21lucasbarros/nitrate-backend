package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.TasteCandidateGroupDTO;
import com.lucasbarros.nitrate.dto.TasteProfileDTO;
import com.lucasbarros.nitrate.dto.TasteQuizDTO;
import com.lucasbarros.nitrate.dto.TasteRequestDTO;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.services.TasteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** questionário de gostos do onboarding (página /comecar no site); tudo exige login */
@RestController
public class TasteResource {

    @Autowired
    private TasteService tasteService;

    @GetMapping("/taste/quiz")
    public TasteQuizDTO quiz() {
        return tasteService.quiz();
    }

    // POST porque as respostas vão no corpo (listas de ids); não altera nada
    @PostMapping("/taste/candidates")
    public List<TasteCandidateGroupDTO> candidatos(@AuthenticationPrincipal CustomUserDetails principal,
                                     @RequestBody TasteRequestDTO request) {
        return tasteService.candidatos(principal.getUser(), request);
    }

    @GetMapping("/users/me/taste")
    public TasteProfileDTO meuGosto(@AuthenticationPrincipal CustomUserDetails principal) {
        return tasteService.perfil(principal.getUser());
    }

    @PutMapping("/users/me/taste")
    public TasteProfileDTO salvarGosto(@AuthenticationPrincipal CustomUserDetails principal,
                                       @RequestBody TasteRequestDTO request) {
        return tasteService.salvar(principal.getUser(), request);
    }
}
