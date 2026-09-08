package com.hanaman.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class StartExerciseRequest {

    @NotNull
    private UUID exerciseId;
}
