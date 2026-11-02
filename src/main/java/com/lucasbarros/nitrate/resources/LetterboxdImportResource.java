package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.LetterboxdImportDTO;
import com.lucasbarros.nitrate.dto.LetterboxdImportDTO.ItemResult;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.services.LetterboxdImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/import/letterboxd")
public class LetterboxdImportResource {

    @Autowired
    private LetterboxdImportService importService;

    // um lote por chamada (até LetterboxdImportService.MAX_LOTE filmes); o front envia em sequência
    @PostMapping
    public List<ItemResult> importar(@AuthenticationPrincipal CustomUserDetails principal,
                                     @RequestBody LetterboxdImportDTO.Request request) {
        return importService.importarLote(principal.getUser(), request.items());
    }
}
