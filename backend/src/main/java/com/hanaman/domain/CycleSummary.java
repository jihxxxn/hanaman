package com.hanaman.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 4주 사이클이 끝나는 시점(WeeklyEvaluationService.finalizeCycle)에 한 번 생성되는 요약 레코드.
 * 프론트엔드가 앱 진입 시 "확인 안 한 사이클 요약이 있는지"를 조회해 종료 화면을 보여주는 데 쓴다.
 * acknowledged=false인 동안만 노출되고, 사용자가 확인하면 true로 바뀌어 다시 뜨지 않는다.
 */
@Entity
@Table(name = "cycle_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleSummary {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_exercise_id")
    private UserExercise userExercise;

    /** 동작명 스냅샷 — 이후 다른 동작으로 넘어가도 이 요약이 가리키던 동작명은 그대로 보존 */
    @Column(nullable = false)
    private String exerciseName;

    @Column(nullable = false)
    private Integer successWeeks;

    @Column(nullable = false)
    private Integer totalWeeks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Outcome outcome;

    @Column(nullable = false)
    private LocalDate cycleEndDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean acknowledged = false;

    public enum Outcome {
        MASTERED, EXTENDED
    }
}
