package com.hanaman.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {

    @NotBlank
    private String nickname;

    private String triggerHabit;

    @NotBlank
    private String firstExerciseName; // 온보딩 시 선택한 첫 동작 이름 (마스터 데이터에서 조회)
}
