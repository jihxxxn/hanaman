package com.hanaman.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 선택 가능한 운동 동작 마스터 데이터 (스쿼트, 런지, 플랭크 등)
 */
@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Unit unit;

    /** 처음 시작할 때 기본 목표치 */
    @Column(nullable = false)
    private Integer defaultStartValue;

    /** 적응형 하향 조정 시 절대 이 아래로는 내려가지 않는 하한값 */
    @Column(nullable = false)
    private Integer minValue;

    public enum Unit {
        REPS, SECONDS
    }
}
