package com.hanaman.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 사용자가 현재(혹은 과거에) 진행 중인 동작 사이클.
 * 한 사용자는 status = ACTIVE 인 UserExercise를 동시에 1개만 가질 수 있다 (서비스 레이어에서 강제).
 */
@Entity
@Table(name = "user_exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserExercise {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id")
    private Exercise exercise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private LocalDate cycleStartDate;

    /** 1~4 */
    @Column(nullable = false)
    private Integer currentWeek;

    /** 이번 주 목표치 (적응형 조정이 반영된 값) */
    @Column(nullable = false)
    private Integer currentTarget;

    /** 몇 번째로 해금된 동작인지 (0 = 첫 동작) */
    @Column(nullable = false)
    private Integer orderIndex;

    /**
     * 이 동작을 몇 번째 4주 사이클째 진행 중인지 (1부터 시작, 연장될 때마다 +1).
     * UserExercise는 연장돼도 같은 행을 재사용하기 때문에, weekNumber(1~4)만으로는
     * 이전 사이클과 새 사이클의 WeeklyGoal을 구분할 수 없어서 별도로 둔다.
     */
    @Builder.Default
    @Column(nullable = false)
    private Integer cycleNumber = 1;

    public enum Status {
        ACTIVE,     // 현재 4주 사이클 진행 중
        MASTERED,   // 4주 중 3주 이상 성공하여 완료
        EXTENDED    // 마스터하지 못해 동일 동작으로 사이클 연장됨 (기록용, 새 UserExercise 대신 currentWeek/cycleStartDate 리셋)
    }
}
