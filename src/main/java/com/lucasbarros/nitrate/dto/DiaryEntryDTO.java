package com.lucasbarros.nitrate.dto;

/** Um registro do diário do perfil: de filme (`review`) ou de série (`seriesReview`); o outro vem nulo. */
public record DiaryEntryDTO(String kind, ReviewDTO review, SeriesReviewDTO seriesReview) {

    public static DiaryEntryDTO filme(ReviewDTO review) {
        return new DiaryEntryDTO("MOVIE", review, null);
    }

    public static DiaryEntryDTO serie(SeriesReviewDTO review) {
        return new DiaryEntryDTO("SERIES", null, review);
    }
}
