package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.AchievementDTO;
import com.lucasbarros.nitrate.dto.MovieCreditsDTO;
import com.lucasbarros.nitrate.dto.PersonProgressDTO;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.entities.UserAchievement;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.UserAchievementRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ForbiddenException;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonDetailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Conquistas do perfil. Por enquanto, a filmografia completa: ter visto todos os filmes já
 * lançados de um diretor ou ator (os mesmos que a página da pessoa mostra). Confere ao abrir a
 * página da pessoa e, em segundo plano, depois de registrar um filme (direção e elenco principal).
 */
@Service
public class AchievementService {

    private static final Logger log = LoggerFactory.getLogger(AchievementService.class);

    // com um ou dois filmes lançados, "viu tudo" não diz nada
    public static final int MINIMO_FILMES = 5;
    // depois de registrar um filme, quantos do elenco conferir (cada um é uma filmografia do TMDB)
    private static final int ELENCO_CONFERIDO = 5;

    @Autowired private UserAchievementRepository achievementRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private PersonService personService;
    @Autowired private PersonSlugService personSlugService;
    @Autowired private MovieService movieService;

    /** página da pessoa: quanto você já viu e, se chegou a 100%, a conquista (criada aqui na hora) */
    public PersonProgressDTO progresso(User user, String ref) {
        return progresso(user, personService.resolver(ref));
    }

    private PersonProgressDTO progresso(User user, Long tmdbId) {
        PersonService.Filmografia filmografia = personService.filmografia(tmdbId);
        Set<Long> lancados = filmografia.lancados();
        int vistos = lancados.isEmpty() ? 0 : reviewRepository.findTmdbIdsAssistidos(user, lancados).size();

        Optional<UserAchievement> conquista = achievementRepository
                .findByUserAndTypeAndPersonTmdbId(user, UserAchievement.FILMOGRAFIA_COMPLETA, tmdbId);
        if (conquista.isEmpty() && lancados.size() >= MINIMO_FILMES && vistos == lancados.size()) {
            conquista = Optional.of(conceder(user, filmografia.pessoa(), lancados.size()));
        }
        TmdbPersonDetailDTO pessoa = filmografia.pessoa();
        String slug = personSlugService.slug(pessoa.getId(), pessoa.getName());
        return new PersonProgressDTO(vistos, lancados.size(), conquista.map(c -> new AchievementDTO(c, slug)).orElse(null));
    }

    private UserAchievement conceder(User user, TmdbPersonDetailDTO pessoa, int filmes) {
        try {
            return achievementRepository.save(new UserAchievement(user, UserAchievement.FILMOGRAFIA_COMPLETA,
                    pessoa.getId(), pessoa.getName(), pessoa.getProfilePath(), filmes));
        } catch (DataIntegrityViolationException e) {
            // a conferência em segundo plano e a página chegaram juntas: fica a que entrou primeiro
            return achievementRepository
                    .findByUserAndTypeAndPersonTmdbId(user, UserAchievement.FILMOGRAFIA_COMPLETA, pessoa.getId())
                    .orElseThrow(() -> e);
        }
    }

    /**
     * Depois de registrar um filme: confere a direção e o começo do elenco, que é onde alguém
     * costuma completar uma filmografia. Chamado depois do commit, para a review nova contar.
     */
    @Async
    public void conferirDepoisDoRegistro(Long userId, Long movieId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;
            MovieCreditsDTO creditos = movieService.creditos(movieId);
            Set<Long> pessoas = new LinkedHashSet<>();
            creditos.directors().forEach(p -> pessoas.add(p.tmdbId()));
            creditos.cast().stream().limit(ELENCO_CONFERIDO).forEach(p -> pessoas.add(p.tmdbId()));
            for (Long tmdbId : pessoas) {
                try {
                    progresso(user, tmdbId);
                } catch (Exception e) {
                    log.warn("Falha ao conferir a filmografia da pessoa {}: {}", tmdbId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao conferir conquistas depois do registro do filme {}: {}", movieId, e.getMessage());
        }
    }

    /** aba Conquistas: a dona vê também as que tirou do perfil; quem visita, só as visíveis */
    @Transactional(readOnly = true)
    public List<AchievementDTO> doUsuario(String username, User quemVe) {
        User user = userRepository.findFirstByUsernameIgnoreCase(Objects.requireNonNullElse(username, "").trim())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + username));
        boolean dona = quemVe != null && quemVe.getId().equals(user.getId());
        List<UserAchievement> conquistas = dona
                ? achievementRepository.findByUserOrderByAchievedAtDesc(user)
                : achievementRepository.findByUserAndHiddenFalseOrderByAchievedAtDesc(user);
        Map<Long, String> nomes = new HashMap<>();
        conquistas.forEach(c -> nomes.put(c.getPersonTmdbId(), c.getPersonName()));
        Map<Long, String> slugs = personSlugService.slugs(nomes);
        return conquistas.stream().map(c -> new AchievementDTO(c, slugs.get(c.getPersonTmdbId()))).toList();
    }

    @Transactional
    public AchievementDTO definirVisibilidade(User eu, Long id, boolean oculta) {
        UserAchievement conquista = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conquista não encontrada. Id: " + id));
        if (!conquista.getUser().getId().equals(eu.getId())) {
            throw new ForbiddenException("Você só pode alterar as suas próprias conquistas.");
        }
        conquista.setHidden(oculta);
        String slug = personSlugService.slugs(Map.of(conquista.getPersonTmdbId(), conquista.getPersonName()))
                .get(conquista.getPersonTmdbId());
        return new AchievementDTO(conquista, slug);
    }
}
