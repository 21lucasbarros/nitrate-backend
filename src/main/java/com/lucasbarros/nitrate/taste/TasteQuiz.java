package com.lucasbarros.nitrate.taste;

import com.lucasbarros.nitrate.entities.Genre;
import com.lucasbarros.nitrate.entities.Movie;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * O questionário de gostos do onboarding: gêneros, uma pergunta de "como" para os gêneros que têm
 * variações. Cada resposta vira um filtro sobre o acervo (combinação de gêneros do TMDB e ano),
 * usado para montar a grade de filmes e para dar um empurrão nas recomendações.
 * Fica no backend para o site e o app mostrarem as mesmas perguntas.
 */
public final class TasteQuiz {

    private TasteQuiz() {
    }

    /**
     * `nome` é o gênero como vem do TMDB (tb_genre.name); `label` é o que aparece na tela.
     * Com `palavraChave`, o "gênero" é na verdade uma palavra-chave do TMDB (o TMDB não tem gênero
     * "musical") e `nome` vira o plano B para quando a lista dela não estiver disponível.
     */
    public record GenreOption(String id, String label, String nome, Integer palavraChave) {

        GenreOption(String id, String label, String nome) {
            this(id, label, nome, null);
        }

        public boolean aceita(Movie filme, PalavrasChave palavras) {
            if (palavraChave != null && palavras.disponivel(palavraChave)) return palavras.tem(filme, palavraChave);
            return generosDe(filme).contains(nome);
        }
    }

    /**
     * Filtro de uma resposta: o filme precisa ter todos de `todos`, pelo menos um de `algum`
     * (se houver), nenhum de `nenhum` e o ano entre `anoDe` e `anoAte` (se houver). Com
     * `palavraChave`, precisa também da palavra-chave do TMDB; sem a lista dela (TMDB fora do ar),
     * vale ter todos os gêneros de `planoB`.
     */
    public record Filtro(Set<String> todos, Set<String> algum, Set<String> nenhum, Integer anoDe, Integer anoAte,
                         Integer palavraChave, Set<String> planoB) {

        Filtro(Set<String> todos, Set<String> algum, Set<String> nenhum, Integer anoDe, Integer anoAte) {
            this(todos, algum, nenhum, anoDe, anoAte, null, Set.of());
        }

        /** gêneros que a resposta pede (todos e algum): o resto do filme é "fora do assunto" */
        public Set<String> generosDaResposta() {
            Set<String> pedidos = new java.util.HashSet<>(todos);
            pedidos.addAll(algum);
            return pedidos;
        }

        public boolean aceita(Movie filme, PalavrasChave palavras) {
            Set<String> generos = generosDe(filme);
            if (palavraChave != null) {
                boolean ok = palavras.disponivel(palavraChave)
                        ? palavras.tem(filme, palavraChave)
                        : generos.containsAll(planoB);
                if (!ok) return false;
            }
            if (!generos.containsAll(todos)) return false;
            if (!algum.isEmpty() && algum.stream().noneMatch(generos::contains)) return false;
            if (nenhum.stream().anyMatch(generos::contains)) return false;
            Integer ano = filme.getReleaseYear();
            if (anoDe != null && (ano == null || ano < anoDe)) return false;
            if (anoAte != null && (ano == null || ano > anoAte)) return false;
            return true;
        }
    }

    /** `motivo` completa "Porque você curte ..." nas recomendações */
    public record Option(String id, String label, String hint, String motivo, Filtro filtro) {
    }

    public record Question(String id, String genre, String title, List<Option> options) {
    }

    // nomes dos gêneros do TMDB em pt-BR, normalizados (minúsculas, sem acento)
    private static final String ACAO = "acao", AVENTURA = "aventura", ANIMACAO = "animacao", COMEDIA = "comedia",
            CRIME = "crime", DOCUMENTARIO = "documentario", DRAMA = "drama", FAMILIA = "familia",
            FANTASIA = "fantasia", FICCAO = "ficcao cientifica", GUERRA = "guerra", HISTORIA = "historia",
            MISTERIO = "misterio", MUSICA = "musica", ROMANCE = "romance", SUSPENSE = "thriller",
            TERROR = "terror", FAROESTE = "faroeste";

    // o TMDB marca como comédia filmes como Pulp Fiction (crime, comédia, suspense): sem isto,
    // "comédia romântica" trazia tiroteio
    private static final Set<String> FORA_DA_COMEDIA_ROMANTICA = Set.of(ACAO, CRIME, SUSPENSE, TERROR, GUERRA);

    public static final List<GenreOption> GENEROS = List.of(
            new GenreOption("ACAO", "Ação", ACAO),
            new GenreOption("AVENTURA", "Aventura", AVENTURA),
            new GenreOption("COMEDIA", "Comédia", COMEDIA),
            new GenreOption("DRAMA", "Drama", DRAMA),
            new GenreOption("ROMANCE", "Romance", ROMANCE),
            new GenreOption("SUSPENSE", "Suspense", SUSPENSE),
            new GenreOption("TERROR", "Terror", TERROR),
            new GenreOption("FICCAO", "Ficção científica", FICCAO),
            new GenreOption("FANTASIA", "Fantasia", FANTASIA),
            new GenreOption("CRIME", "Crime", CRIME),
            new GenreOption("MISTERIO", "Mistério", MISTERIO),
            new GenreOption("ANIMACAO", "Animação", ANIMACAO),
            new GenreOption("FAMILIA", "Família", FAMILIA),
            new GenreOption("HISTORIA", "História", HISTORIA),
            new GenreOption("GUERRA", "Guerra", GUERRA),
            new GenreOption("MUSICA", "Musical", MUSICA, PalavrasChave.MUSICAL),
            new GenreOption("DOCUMENTARIO", "Documentário", DOCUMENTARIO),
            new GenreOption("FAROESTE", "Faroeste", FAROESTE));

    public static final List<Question> PERGUNTAS = List.of(
            pergunta("ROMANCE", "Que tipo de romance te pega?",
                    opcao("ROMANCE_COMEDIA", "Comédia romântica", "Leve, pra sair sorrindo", "comédia romântica",
                            filtro(Set.of(ROMANCE, COMEDIA), Set.of(), FORA_DA_COMEDIA_ROMANTICA, null, null)),
                    opcao("ROMANCE_DRAMA", "Romance que faz chorar", "Amor e coração partido", "romance dramático",
                            filtro(Set.of(ROMANCE, DRAMA), Set.of(), Set.of(COMEDIA), null, null)),
                    opcao("ROMANCE_FANTASIA", "Romance com fantasia", "Viagem no tempo, magia, ficção", "romance com fantasia",
                            filtro(Set.of(ROMANCE), Set.of(FANTASIA, FICCAO), Set.of(), null, null)),
                    opcao("ROMANCE_CLASSICO", "Clássicos do romance", "Os que todo mundo cita", "clássicos do romance",
                            filtro(Set.of(ROMANCE), Set.of(), Set.of(), null, 1999))),
            pergunta("TERROR", "Que tipo de terror?",
                    opcao("TERROR_PSICOLOGICO", "Psicológico", "Mexe com a cabeça, tensão até o fim", "terror psicológico",
                            filtro(Set.of(TERROR), Set.of(SUSPENSE, MISTERIO), Set.of(COMEDIA), null, null)),
                    opcao("TERROR_SOBRENATURAL", "Monstros e sobrenatural", "Criaturas, alienígenas, o desconhecido", "terror sobrenatural",
                            filtro(Set.of(TERROR), Set.of(FICCAO, FANTASIA), Set.of(), null, null)),
                    opcao("TERROR_COMEDIA", "Terror com humor", "Susto e risada juntos", "terror com humor",
                            todos(TERROR, COMEDIA)),
                    opcao("TERROR_RECENTE", "Terror recente", "O que saiu nos últimos anos", "terror recente",
                            filtro(Set.of(TERROR), Set.of(), Set.of(), 2015, null))),
            pergunta("COMEDIA", "Que humor é a sua cara?",
                    opcao("COMEDIA_ROMANTICA", "Comédia romântica", "Rir e se apaixonar", "comédia romântica",
                            filtro(Set.of(COMEDIA, ROMANCE), Set.of(), FORA_DA_COMEDIA_ROMANTICA, null, null)),
                    opcao("COMEDIA_DRAMATICA", "Comédia dramática", "Rir e se emocionar", "comédia dramática",
                            todos(COMEDIA, DRAMA)),
                    opcao("COMEDIA_ACAO", "Comédia de ação e aventura", "Correria e piada", "comédia de ação",
                            filtro(Set.of(COMEDIA), Set.of(ACAO, AVENTURA), Set.of(), null, null)),
                    opcao("COMEDIA_FAMILIA", "Pra ver em família", "Do tipo que todo mundo assiste junto", "comédia para a família",
                            filtro(Set.of(COMEDIA), Set.of(FAMILIA, ANIMACAO), Set.of(), null, null)),
                    opcao("COMEDIA_CRIME", "Comédia policial", "Golpes, trapaças e confusão", "comédia policial",
                            todos(COMEDIA, CRIME))),
            pergunta("DRAMA", "Que drama te prende?",
                    opcao("DRAMA_ROMANCE", "Drama romântico", "Histórias de amor intensas", "drama romântico",
                            todos(DRAMA, ROMANCE)),
                    opcao("DRAMA_CRIME", "Drama policial", "Crime, investigação, tensão", "drama policial",
                            filtro(Set.of(DRAMA), Set.of(CRIME, SUSPENSE), Set.of(), null, null)),
                    opcao("DRAMA_HISTORIA", "Histórias reais e de época", "Fatos históricos e guerra", "drama histórico",
                            filtro(Set.of(DRAMA), Set.of(HISTORIA, GUERRA), Set.of(), null, null)),
                    opcao("DRAMA_FICCAO", "Drama com ficção científica", "Ideias grandes, futuro, fantasia", "drama com ficção científica",
                            filtro(Set.of(DRAMA), Set.of(FICCAO, FANTASIA), Set.of(), null, null)),
                    opcao("DRAMA_LEVE", "Drama leve e familiar", "Emociona sem pesar", "drama leve",
                            filtro(Set.of(DRAMA), Set.of(COMEDIA, FAMILIA), Set.of(), null, null))),
            pergunta("ACAO", "Que tipo de ação?",
                    opcao("ACAO_HEROIS", "Super-heróis e ficção científica", "Poderes, robôs, futuro", "ação com ficção científica",
                            todos(ACAO, FICCAO)),
                    opcao("ACAO_AVENTURA", "Aventura épica", "Jornadas, mundos e batalhas", "aventura épica",
                            filtro(Set.of(ACAO), Set.of(AVENTURA, FANTASIA), Set.of(), null, null)),
                    opcao("ACAO_POLICIAL", "Policial e perseguição", "Tiroteio, assalto, tensão", "ação policial",
                            filtro(Set.of(ACAO), Set.of(CRIME, SUSPENSE), Set.of(), null, null)),
                    opcao("ACAO_COMEDIA", "Ação com humor", "Explosão e piada", "ação com humor",
                            todos(ACAO, COMEDIA)),
                    opcao("ACAO_GUERRA", "Guerra", "Batalhas e histórias reais", "filmes de guerra",
                            filtro(Set.of(GUERRA), Set.of(), Set.of(), null, null))),
            pergunta("FICCAO", "Que lado da ficção científica?",
                    opcao("FICCAO_ESPACO", "Espaço e aventura", "Naves, planetas, exploração", "ficção científica de aventura",
                            todos(FICCAO, AVENTURA)),
                    opcao("FICCAO_ACAO", "Ficção científica de ação", "Heróis, robôs, batalhas", "ficção científica de ação",
                            todos(FICCAO, ACAO)),
                    opcao("FICCAO_REFLEXIVA", "Que faz pensar", "Mistério, dilemas, finais abertos", "ficção científica reflexiva",
                            filtro(Set.of(FICCAO), Set.of(DRAMA, MISTERIO, SUSPENSE), Set.of(COMEDIA), null, null)),
                    opcao("FICCAO_TERROR", "Ficção científica de terror", "Alienígenas e experimentos", "ficção científica de terror",
                            todos(FICCAO, TERROR))),
            pergunta("SUSPENSE", "Que tipo de suspense?",
                    opcao("SUSPENSE_INVESTIGACAO", "Investigação", "Quem foi? Pistas e reviravoltas", "suspense de investigação",
                            filtro(Set.of(SUSPENSE), Set.of(CRIME, MISTERIO), Set.of(), null, null)),
                    opcao("SUSPENSE_PSICOLOGICO", "Psicológico", "Tensão, paranoia, jogos mentais", "suspense psicológico",
                            filtro(Set.of(SUSPENSE, DRAMA), Set.of(), Set.of(ACAO), null, null)),
                    opcao("SUSPENSE_ACAO", "Suspense com ação", "Perseguição e adrenalina", "suspense de ação",
                            todos(SUSPENSE, ACAO)),
                    opcao("SUSPENSE_TERROR", "Suspense que assusta", "Beirando o terror", "suspense com terror",
                            todos(SUSPENSE, TERROR))),
            pergunta("CRIME", "Que tipo de filme de crime?",
                    opcao("CRIME_DRAMA", "Crime dramático", "Máfia, cadeia, consequências", "drama de crime",
                            todos(CRIME, DRAMA)),
                    opcao("CRIME_MISTERIO", "Investigação e mistério", "Detetives e reviravoltas", "mistério policial",
                            todos(CRIME, MISTERIO)),
                    opcao("CRIME_ACAO", "Crime com ação", "Assaltos e perseguições", "crime com ação",
                            todos(CRIME, ACAO)),
                    opcao("CRIME_COMEDIA", "Crime com humor", "Golpes que dão errado", "crime com humor",
                            todos(CRIME, COMEDIA))),
            pergunta("AVENTURA", "Que tipo de aventura?",
                    opcao("AVENTURA_FANTASIA", "Fantasia épica", "Magia, reinos, jornadas", "fantasia épica",
                            todos(AVENTURA, FANTASIA)),
                    opcao("AVENTURA_FICCAO", "Aventura no espaço", "Ficção científica e exploração", "aventura de ficção científica",
                            todos(AVENTURA, FICCAO)),
                    opcao("AVENTURA_FAMILIA", "Pra família", "Do tipo que todo mundo assiste junto", "aventura para a família",
                            filtro(Set.of(AVENTURA), Set.of(FAMILIA, ANIMACAO), Set.of(), null, null)),
                    opcao("AVENTURA_COMEDIA", "Aventura com humor", "Leve e divertida", "aventura com humor",
                            todos(AVENTURA, COMEDIA))),
            pergunta("FANTASIA", "Que tipo de fantasia?",
                    opcao("FANTASIA_EPICA", "Épica", "Batalhas, reinos e magia", "fantasia épica",
                            filtro(Set.of(FANTASIA), Set.of(AVENTURA, ACAO), Set.of(), null, null)),
                    opcao("FANTASIA_FAMILIA", "Pra família", "Encantadora, pra todas as idades", "fantasia para a família",
                            filtro(Set.of(FANTASIA), Set.of(FAMILIA, ANIMACAO), Set.of(), null, null)),
                    opcao("FANTASIA_ROMANCE", "Fantasia romântica", "Amor com um toque de magia", "fantasia romântica",
                            todos(FANTASIA, ROMANCE)),
                    opcao("FANTASIA_SOMBRIA", "Sombria", "Beirando o terror", "fantasia sombria",
                            filtro(Set.of(FANTASIA), Set.of(TERROR, SUSPENSE), Set.of(), null, null))),
            pergunta("ANIMACAO", "Que tipo de animação?",
                    opcao("ANIMACAO_FAMILIA", "Pra família", "Pixar, Disney e companhia", "animação para a família",
                            todos(ANIMACAO, FAMILIA)),
                    opcao("ANIMACAO_AVENTURA", "Aventura e ação", "Heróis e grandes jornadas", "animação de aventura",
                            filtro(Set.of(ANIMACAO), Set.of(AVENTURA, ACAO), Set.of(), null, null)),
                    opcao("ANIMACAO_FICCAO", "Ficção científica", "Robôs, espaço, futuro", "animação de ficção científica",
                            todos(ANIMACAO, FICCAO)),
                    opcao("ANIMACAO_COMEDIA", "Comédia animada", "Pra rir do começo ao fim", "comédia animada",
                            todos(ANIMACAO, COMEDIA))),
            pergunta("MISTERIO", "Que tipo de mistério?",
                    opcao("MISTERIO_INVESTIGACAO", "Quem foi?", "Detetives, pistas e suspeitos", "mistério de investigação",
                            todos(MISTERIO, CRIME)),
                    opcao("MISTERIO_TERROR", "Mistério com terror", "O desconhecido que assusta", "mistério com terror",
                            todos(MISTERIO, TERROR)),
                    opcao("MISTERIO_PSICOLOGICO", "Psicológico", "Nada é o que parece", "mistério psicológico",
                            filtro(Set.of(MISTERIO), Set.of(DRAMA, SUSPENSE), Set.of(TERROR), null, null))),
            // o gênero "Música" do TMDB é de filmes sobre música (cinebiografias, documentários); musical
            // de verdade, em que os personagens cantam, é a palavra-chave "musical"
            pergunta("MUSICA", "Que tipo de filme musical?",
                    opcao("MUSICA_MUSICAL", "Musicais", "Os personagens cantam: La La Land, Mamma Mia!", "musicais",
                            musical(Set.of(), Set.of(ANIMACAO))),
                    opcao("MUSICA_ANIMACAO", "Musicais animados", "Disney, Pixar e companhia", "musicais animados",
                            musical(Set.of(ANIMACAO), Set.of())),
                    opcao("MUSICA_BIOGRAFIA", "Cinebiografias de músicos", "Bohemian Rhapsody, Rocketman, Elvis", "cinebiografias musicais",
                            filtro(Set.of(MUSICA), Set.of(DRAMA, HISTORIA), Set.of(DOCUMENTARIO, ANIMACAO), null, null)),
                    opcao("MUSICA_DOCUMENTARIO", "Documentários e shows", "Bastidores, turnês e concertos", "documentários musicais",
                            todos(MUSICA, DOCUMENTARIO))));

    public static final Map<String, GenreOption> GENERO_POR_ID = porId(GENEROS, GenreOption::id);
    /** todas as respostas, na ordem do questionário */
    public static final List<Option> OPCOES = PERGUNTAS.stream().flatMap(p -> p.options().stream()).toList();
    public static final Map<String, Option> OPCAO_POR_ID = porId(OPCOES, Option::id);
    /** id da resposta -> id do gênero da pergunta dela */
    public static final Map<String, String> GENERO_DA_OPCAO = PERGUNTAS.stream()
            .flatMap(p -> p.options().stream().map(o -> Map.entry(o.id(), p.genre())))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    /** gêneros do filme normalizados, para comparar sem depender de maiúsculas e acentos */
    public static Set<String> generosDe(Movie filme) {
        return filme.getGenres().stream().map(Genre::getName).map(TasteQuiz::normalizar).collect(Collectors.toSet());
    }

    public static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    private static Question pergunta(String genero, String titulo, Option... opcoes) {
        return new Question(genero, genero, titulo, List.of(opcoes));
    }

    private static Option opcao(String id, String label, String hint, String motivo, Filtro filtro) {
        return new Option(id, label, hint, motivo, filtro);
    }

    /** musical de verdade (palavra-chave do TMDB); sem a lista, o plano B é o gênero "Música" */
    private static Filtro musical(Set<String> todos, Set<String> nenhum) {
        return new Filtro(todos, Set.of(), nenhum, null, null, PalavrasChave.MUSICAL, Set.of(MUSICA));
    }

    private static Filtro todos(String... generos) {
        return filtro(Set.of(generos), Set.of(), Set.of(), null, null);
    }

    private static Filtro filtro(Set<String> todos, Set<String> algum, Set<String> nenhum, Integer anoDe, Integer anoAte) {
        return new Filtro(todos, algum, nenhum, anoDe, anoAte);
    }

    private static <T> Map<String, T> porId(List<T> itens, Function<T, String> id) {
        Map<String, T> mapa = new LinkedHashMap<>();
        itens.forEach(item -> mapa.put(id.apply(item), item));
        return Map.copyOf(mapa);
    }
}
