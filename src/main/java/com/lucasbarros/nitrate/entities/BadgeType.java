package com.lucasbarros.nitrate.entities;

/**
 * Catálogo de selos que o admin concede no painel. A ordem aqui é a ordem em que aparecem no perfil.
 * O ícone e a cor ficam no front (chaveados pelo nome), o texto fica aqui para o site e o app.
 * Para criar um selo novo basta acrescentar uma constante: o banco guarda o nome como texto livre.
 */
public enum BadgeType {
    FUNDADOR("Fundador", "Criou o Nitrate."),
    EQUIPE("Equipe", "Faz parte do time do Nitrate."),
    INVESTIDOR("Investidor", "Investiu no Nitrate."),
    APOIADOR("Apoiador", "Deu uma força para o Nitrate existir."),
    FAMILIA("Família", "Da família de quem faz o Nitrate."),
    INSPIRACAO("Inspiração", "Inspirou a criação do Nitrate."),
    COLABORADOR("Colaborador", "Contribuiu com código, design ou ideias."),
    BETA_TESTER("Beta tester", "Testou o Nitrate antes de todo mundo."),
    PIONEIRO("Pioneiro", "Entre as primeiras pessoas no Nitrate."),
    CACADOR_DE_BUGS("Caçador de bugs", "Encontrou e reportou bugs."),
    EMBAIXADOR("Embaixador", "Trouxe muita gente para o Nitrate."),
    CRITICO("Crítico em destaque", "Escreve reviews que valem a leitura."),
    CINEFILO("Cinéfilo", "Vive dentro de uma sala de cinema."),
    VERIFICADO("Verificado", "Perfil oficial confirmado."),
    LENDA("Lenda", "Uma lenda do Nitrate.");

    private final String label;
    private final String description;

    BadgeType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
}
