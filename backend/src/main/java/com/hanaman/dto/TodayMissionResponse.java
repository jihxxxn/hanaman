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

    // 지난주 대비 이번주 목표 조정 정보 — 1주차거나 사이클이 막 시작된 경우 null (프론트에서 안내 문구를 안 띄움)
    private Integer previousWeekTarget;
    private Integer previousWeekSuccessDays;
}
