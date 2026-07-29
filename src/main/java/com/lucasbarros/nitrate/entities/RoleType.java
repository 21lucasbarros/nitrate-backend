package com.lucasbarros.nitrate.entities;

public enum RoleType {
    PROTAGONIST(1.0),
    SUPPORTING(0.6),
    CAMEO(0.25);

    private final double relevance;

    RoleType(double relevance) {
        this.relevance = relevance;
    }

    public double getRelevance() {
        return relevance;
    }
}
