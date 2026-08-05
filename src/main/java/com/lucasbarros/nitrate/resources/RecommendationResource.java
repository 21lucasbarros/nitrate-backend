package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.RecommendationRequestDTO;
import com.lucasbarros.nitrate.dto.RecommendationResultDTO;
import com.lucasbarros.nitrate.services.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/recommendations")
public class RecommendationResource {

    @Autowired
    private RecommendationService recommendationService;

    @PostMapping
    public List<RecommendationResultDTO> recomendar(@RequestBody RecommendationRequestDTO request) {
        return recommendationService.recomendar(request);
    }
}
