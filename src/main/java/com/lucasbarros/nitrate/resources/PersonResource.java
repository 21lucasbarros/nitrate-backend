package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.PersonDetailDTO;
import com.lucasbarros.nitrate.dto.PersonSummaryDTO;
import com.lucasbarros.nitrate.services.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/people")
public class PersonResource {

    @Autowired
    private PersonService personService;

    @GetMapping("/search")
    public List<PersonSummaryDTO> pesquisar(@RequestParam(defaultValue = "") String q) {
        return personService.pesquisar(q);
    }

    @GetMapping("/{tmdbId}")
    public PersonDetailDTO detalhe(@PathVariable Long tmdbId) {
        return personService.detalhe(tmdbId);
    }
}
