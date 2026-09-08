package com.hanaman.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "check_ins", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_exercise_id", "date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckIn {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_exercise_id")
    private UserExercise userExercise;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Integer completedValue;

    /** 체크인 당시의 목표치 — 이후 목표치가 조정되어도 이력은 그대로 보존 */
    @Column(nullable = false)
    private Integer targetValueSnapshot;

    @Column(nullable = false)
    private Boolean achieved;
}
