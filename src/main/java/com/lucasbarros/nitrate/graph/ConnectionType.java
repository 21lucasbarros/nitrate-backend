package com.lucasbarros.nitrate.graph;

public enum ConnectionType {
    FRANCHISE(1.2, "mesma franquia/saga"),
    DIRECTOR(1.0, "mesmo diretor"),
    // quem curtiu um também curtiu o outro (ver MovieGraph.conectarPorCurtidas)
    CO_LIKED(1.0, "quem curtiu também curtiu"),
    ACTOR(0.8, "elenco em comum"),
    GENRE(0.5, "mesmo gênero");

    private final double baseWeight;
    private final String description;

    ConnectionType(double baseWeight, String description) {
        this.baseWeight = baseWeight;
        this.description = description;
    }

    public double getBaseWeight() {
        return baseWeight;
    }

    public String getDescription() {
        return description;
    }
}
