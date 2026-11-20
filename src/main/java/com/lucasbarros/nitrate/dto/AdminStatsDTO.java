package com.lucasbarros.nitrate.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** números do painel de admin (GET /admin/stats) */
public record AdminStatsDTO(
        Users users,
        Content content,
        List<Day> days,
        List<RecentUser> recentUsers,
        List<TopUser> topReviewers) {

    /**
     * `active*`: quem usou o site logado no período (ver User.lastSeenAt). O registro começou com
     * esta versão, então nos primeiros dias os números ficam abaixo do real.
     */
    public record Users(long total, long new7d, long new30d, long active1d, long active7d, long active30d) {
    }

    public record Content(long reviews, long reviews7d, long reviews30d, long writtenReviews, long movies,
                          long reviewLikes, long reviewComments, long lists, long listLikes, long listComments,
                          long follows, long watchlist, long badges) {
    }

    /** um dia do gráfico (fuso de São Paulo): cadastros, registros no diário e pessoas ativas */
    public record Day(LocalDate date, long signups, long reviews, long reviewers) {
    }

    public record RecentUser(UserSummaryDTO user, Instant createdAt, Instant lastSeenAt, long reviews) {
    }

    public record TopUser(UserSummaryDTO user, long reviews) {
    }
}
