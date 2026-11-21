package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.FavoritePeopleDTO;
import com.lucasbarros.nitrate.dto.FavoritePersonDTO;
import com.lucasbarros.nitrate.entities.FavoritePerson;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.FavoritePersonRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonDetailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Atores/atrizes e diretores favoritos do perfil (a aba "Favoritos"). */
@Service
public class FavoritePeopleService {

    public static final int MAX_FAVORITOS = 5;

    @Autowired private FavoritePersonRepository favoritePersonRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TmdbClient tmdbClient;
    @Autowired private RecommendationService recommendationService;
    @Autowired private PersonSlugService personSlugService;

    @Transactional(readOnly = true)
    public FavoritePeopleDTO favoritos(String username) {
        User user = userRepository.findFirstByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + username));
        List<FavoritePerson> favoritos = favoritePersonRepository.findByUserOrderByPositionAsc(user);
        Map<Long, String> slugs = slugs(favoritos);
        List<FavoritePersonDTO> atores = new ArrayList<>();
        List<FavoritePersonDTO> diretores = new ArrayList<>();
        for (FavoritePerson f : favoritos) {
            (f.getKind() == FavoritePerson.Kind.ACTOR ? atores : diretores).add(new FavoritePersonDTO(f, slugs.get(f.getTmdbId())));
        }
        return new FavoritePeopleDTO(atores, diretores);
    }

    /**
     * Substitui a lista inteira de um tipo; a ordem recebida é a ordem do perfil.
     * Nome e foto vêm do TMDB (não do cliente); quem já estava na lista reaproveita a cópia salva.
     */
    @Transactional
    public List<FavoritePersonDTO> definirFavoritos(User eu, FavoritePerson.Kind tipo, List<Long> tmdbIds) {
        // repetidos contam uma vez só
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(tmdbIds == null ? List.of() : tmdbIds));
        if (ids.size() > MAX_FAVORITOS) {
            throw new IllegalArgumentException("Escolha no máximo " + MAX_FAVORITOS + " favoritos");
        }

        Map<Long, FavoritePerson> atuais = new HashMap<>();
        for (FavoritePerson f : favoritePersonRepository.findByUserAndKindOrderByPositionAsc(eu, tipo)) {
            atuais.put(f.getTmdbId(), f);
        }

        // busca no TMDB antes de apagar: um id inexistente vira 404 sem mexer na lista salva
        record Pessoa(String nome, String foto) {}
        List<Pessoa> pessoas = new ArrayList<>();
        for (Long id : ids) {
            FavoritePerson atual = atuais.get(id);
            if (atual != null) {
                pessoas.add(new Pessoa(atual.getName(), atual.getProfilePath()));
            } else {
                TmdbPersonDetailDTO tmdb = tmdbClient.buscarPessoa(id);
                if (tmdb == null || tmdb.getName() == null) {
                    throw new ResourceNotFoundException("Pessoa não encontrada. Id: " + id);
                }
                pessoas.add(new Pessoa(tmdb.getName(), tmdb.getProfilePath()));
            }
        }

        favoritePersonRepository.deleteAllByUserAndKind(eu, tipo);
        List<FavoritePerson> salvos = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Pessoa p = pessoas.get(i);
            salvos.add(favoritePersonRepository.save(new FavoritePerson(eu, tipo, ids.get(i), p.nome(), p.foto(), i + 1)));
        }
        Map<Long, String> slugs = slugs(salvos);
        List<FavoritePersonDTO> dtos = salvos.stream().map(f -> new FavoritePersonDTO(f, slugs.get(f.getTmdbId()))).toList();
        // os favoritos pesam nas recomendações da home
        recommendationService.invalidar(eu.getId());
        return dtos;
    }

    private Map<Long, String> slugs(List<FavoritePerson> favoritos) {
        Map<Long, String> nomes = new HashMap<>();
        favoritos.forEach(f -> nomes.put(f.getTmdbId(), f.getName()));
        return personSlugService.slugs(nomes);
    }
}
