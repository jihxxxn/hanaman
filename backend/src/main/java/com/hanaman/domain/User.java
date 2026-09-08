package com.hanaman.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String nickname;

    /** "양치 후", "커피 내리는 동안" 같은 사용자 지정 습관 트리거 */
    private String triggerHabit;

    @Builder.Default
    private Instant createdAt = Instant.now();

    /** 지금까지 찍어본 "최근 7일 중 달성 일수"(rolling streak)의 최고값. 동작이 바뀌어도 계속 이어지는 개인 기록. */
    @Builder.Default
    private Integer bestRollingStreak = 0;

    /** bestRollingStreak가 마지막으로 갱신된 날짜 — 그날 하루만 "새 기록" 문구를 보여주기 위함 */
    private LocalDate bestRollingStreakDate;
}
