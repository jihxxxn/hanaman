package com.hanaman.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class TodayMissionResponse {
    private UUID userExerciseId;
    private String exerciseName;
    private String unit;          // REPS / SECONDS
    private Integer targetValue;
    private Integer completedValueToday; // 오늘 이미 기록된 값 (없으면 0)
    private Boolean achievedToday;
    private Integer currentWeek;
    private Integer rollingSuccessCount;  // 최근 7일 중 달성 일수
}
