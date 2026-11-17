package com.lucasbarros.nitrate.dto;

import java.time.Instant;
import java.util.List;

/** Uma página do feed. Para a próxima, peça de novo com `before = nextBefore` (null = acabou). */
public record ActivityPageDTO(List<ActivityDTO> items, Instant nextBefore) {
}
