package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.entities.PersonSlug;
import com.lucasbarros.nitrate.repositories.PersonSlugRepository;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonDetailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Endereço legível das pessoas: "/person/ben-stiller". O slug sai do nome em inglês (o TMDB
 * devolve alguns nomes no alfabeto original, como diretores gregos) e homônimos ganham sufixo
 * ("-2"). Uma vez criado, o slug não muda, então fica em memória para sempre.
 */
@Service
public class PersonSlugService {

    private static final Logger log = LoggerFactory.getLogger(PersonSlugService.class);
    private static final int TAMANHO_MAX = 80;

    @Autowired private PersonSlugRepository personSlugRepository;
    @Autowired private TmdbClient tmdbClient;

    public PersonSlugService(PlatformTransactionManager transactionManager) {
        this.novaTransacao = new TransactionTemplate(transactionManager);
        this.novaTransacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // transação própria: quem chama pode estar numa transação só de leitura (que nunca grava)
    // ou ser derrubado por um conflito de slug que não tem nada a ver com ele
    private final TransactionTemplate novaTransacao;

    private final Map<Long, String> cache = new ConcurrentHashMap<>();
    // dois pedidos ao mesmo tempo não podem dar o mesmo slug a pessoas diferentes.
    // ReentrantLock e não synchronized pelo mesmo motivo do MovieService (virtual threads)
    private final ReentrantLock criacao = new ReentrantLock();

    /**
     * Slug de cada pessoa (id do TMDB → nome), criando os que faltam numa ida só ao banco.
     * Nomes fora do alfabeto latino ficam sem slug (o link usa o id) até alguém abrir a página
     * da pessoa: buscar o nome em inglês de um elenco inteiro custaria uma chamada ao TMDB por pessoa.
     */
    public Map<Long, String> slugs(Map<Long, String> nomes) {
        Map<Long, String> resultado = new HashMap<>();
        List<Long> faltam = new ArrayList<>();
        for (Long id : nomes.keySet()) {
            String slug = cache.get(id);
            if (slug != null) resultado.put(id, slug);
            else faltam.add(id);
        }
        if (faltam.isEmpty()) return resultado;

        for (PersonSlug salvo : personSlugRepository.findByTmdbIdIn(faltam)) {
            cache.put(salvo.getTmdbId(), salvo.getSlug());
            resultado.put(salvo.getTmdbId(), salvo.getSlug());
        }
        Map<Long, String> novos = new LinkedHashMap<>();
        for (Long id : faltam) {
            if (!resultado.containsKey(id) && latino(nomes.get(id))) novos.put(id, nomes.get(id));
        }
        if (!novos.isEmpty()) resultado.putAll(criar(novos));
        return resultado;
    }

    /** página da pessoa: aqui vale buscar o nome em inglês para quem tem o nome em outro alfabeto */
    public String slug(Long tmdbId, String nome) {
        String slug = slugs(Map.of(tmdbId, Objects.requireNonNullElse(nome, ""))).get(tmdbId);
        if (slug != null) return slug;
        TmdbPersonDetailDTO emIngles = tmdbClient.buscarPessoaEmIngles(tmdbId);
        if (emIngles == null || !latino(emIngles.getName())) return null;
        return criar(Map.of(tmdbId, emIngles.getName())).get(tmdbId);
    }

    public Optional<Long> tmdbIdDe(String slug) {
        return personSlugRepository.findBySlug(slug).map(PersonSlug::getTmdbId);
    }

    private Map<Long, String> criar(Map<Long, String> nomes) {
        criacao.lock();
        try {
            Map<Long, String> resultado = new HashMap<>();
            // outro pedido pode ter criado enquanto este esperava a trava
            for (PersonSlug salvo : personSlugRepository.findByTmdbIdIn(nomes.keySet())) {
                cache.put(salvo.getTmdbId(), salvo.getSlug());
                resultado.put(salvo.getTmdbId(), salvo.getSlug());
            }

            Set<String> usados = new HashSet<>(); // inclui homônimos no mesmo elenco
            Set<String> consultadas = new HashSet<>();
            List<PersonSlug> novos = new ArrayList<>();
            for (Map.Entry<Long, String> pessoa : nomes.entrySet()) {
                if (resultado.containsKey(pessoa.getKey())) continue;
                String base = MovieSlugService.slugify(pessoa.getValue(), TAMANHO_MAX);
                if (base.isEmpty()) continue;
                if (consultadas.add(base)) usados.addAll(personSlugRepository.findSlugsComBase(base));
                String slug = MovieSlugService.livre(base, usados::contains);
                usados.add(slug);
                novos.add(new PersonSlug(pessoa.getKey(), slug));
            }
            if (novos.isEmpty()) return resultado;

            try {
                novaTransacao.executeWithoutResult(status -> personSlugRepository.saveAll(novos));
            } catch (DataIntegrityViolationException e) {
                // corrida com outra instância: fica sem slug agora, a próxima visita tenta de novo
                log.warn("slug de pessoa em conflito: {}", e.getMostSpecificCause().getMessage());
                return resultado;
            }
            for (PersonSlug novo : novos) {
                cache.put(novo.getTmdbId(), novo.getSlug());
                resultado.put(novo.getTmdbId(), novo.getSlug());
            }
            return resultado;
        } finally {
            criacao.unlock();
        }
    }

    /** só letras latinas (com ou sem acento): "Γιώργος Λάνθιμος" não vira um slug legível */
    static boolean latino(String nome) {
        if (nome == null || nome.isBlank()) return false;
        return nome.codePoints()
                .filter(Character::isLetter)
                .allMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.LATIN);
    }
}
