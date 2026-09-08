package com.hanaman.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 4주 사이클 중 한 주차의 결과 스냅샷.
 * 적응형 목표치 조정과 마스터 판정의 근거 데이터로 쓰인다.
 */
@Entity
@Table(name = "weekly_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyGoal {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_exercise_id")
    private UserExercise userExercise;

    /** 이 주가 속한 사이클 번호 — 생성 시점의 UserExercise.cycleNumber 스냅샷. 사이클이 연장돼도 weekNumber가 겹치지 않게 구분한다. */
    @Column(nullable = false)
    private Integer cycleNumber;

    /** 1~4 */
    @Column(nullable = false)
    private Integer weekNumber;

    @Column(nullable = false)
    private Integer targetValue;

    /** 목표치를 달성한 일수 (0~7) */
    @Builder.Default
    private Integer successDays = 0;

    @Enumerated(EnumType.STRING)
    private Result result;

    public enum Result {
        SUCCESS, FAIL, IN_PROGRESS
    }
}
