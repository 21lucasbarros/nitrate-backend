package com.lucasbarros.nitrate.dto;

import java.util.List;

/** as duas listas de pessoas favoritas do perfil, cada uma em ordem */
public record FavoritePeopleDTO(List<FavoritePersonDTO> actors, List<FavoritePersonDTO> directors) {}
