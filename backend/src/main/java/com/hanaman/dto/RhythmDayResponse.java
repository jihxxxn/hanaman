package com.hanaman.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class RhythmDayResponse {

    private LocalDate date;

    /** NONE(기록 없음) / BELOW(목표 미달) / EXACT(목표만 달성) / EXCEEDED(목표 초과 달성) */
    private String status;
}
