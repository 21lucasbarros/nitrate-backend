package com.lucasbarros.nitrate.graph;

import com.lucasbarros.nitrate.entities.Movie;

public class Edge {
    private Movie target;
    private ConnectionType type;
    private double weight;
    private String reason;
    private String attribute;

    public Edge(Movie target, ConnectionType type, double weight, String reason, String attribute) {
        this.target = target;
        this.type = type;
        this.weight = weight;
        this.reason = reason;
        this.attribute = attribute;
    }

    public Movie getTarget() {
        return target;
    }

    public ConnectionType getType() {
        return type;
    }

    public double getWeight() {
        return weight;
    }

    public String getReason() {
        return reason;
    }

    public String getAttribute() {
        return attribute;
    }
}
