package com.hanaman.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
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
}
