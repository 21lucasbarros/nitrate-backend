package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.PersonDetailDTO;
import com.lucasbarros.nitrate.dto.PersonSummaryDTO;
import com.lucasbarros.nitrate.services.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.util.List;

@RestController
@RequestMapping("/people")
public class PersonResource {

    @Autowired
    private PersonService personService;

    @GetMapping("/search")
    public List<PersonSummaryDTO> pesquisar(@RequestParam(defaultValue = "") String q) {
        return personService.pesquisar(Normalizer.normalize(q, Normalizer.Form.NFC));
    }

    // slug ("ben-stiller") ou id do TMDB: os links antigos e o app usam o id
    @GetMapping("/{ref}")
    public PersonDetailDTO detalhe(@PathVariable String ref) {
        return personService.detalhe(ref);
    }
}
