package com.lucasbarros.nitrate.dto;

public class UsernameAvailabilityDTO {
    private boolean available;

    public UsernameAvailabilityDTO(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}