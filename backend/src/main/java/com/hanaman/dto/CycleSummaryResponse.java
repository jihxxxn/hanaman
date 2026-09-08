package com.hanaman.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class CycleSummaryResponse {
    private UUID id;
    private String exerciseName;
    private Integer successWeeks;
    private Integer totalWeeks;
    private String outcome; // MASTERED | EXTENDED
    private LocalDate cycleEndDate;
}
