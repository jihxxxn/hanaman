package com.hanaman.controller;

import com.hanaman.domain.*;
import com.hanaman.dto.*;
import com.hanaman.repository.*;
import com.hanaman.service.CheckInService;
import com.hanaman.service.ExerciseUnlockService;
import com.hanaman.service.RollingStreakService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserExerciseRepository userExerciseRepository;
    private final CheckInRepository checkInRepository;
    private final CheckInService checkInService;
    private final ExerciseUnlockService exerciseUnlockService;
    private final RollingStreakService rollingStreakService;

    @PostMapping
    public ResponseEntity<UUID> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userRepository.save(User.builder()
                .nickname(request.getNickname())
                .triggerHabit(request.getTriggerHabit())
                .build());

        Exercise firstExercise = exerciseRepository.findAll().stream()
                .filter(e -> e.getName().equals(request.getFirstExerciseName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 동작입니다: " + request.getFirstExerciseName()));

        exerciseUnlockService.startNewExercise(user, firstExercise.getId());

        return ResponseEntity.ok(user.getId());
    }

    @GetMapping("/{userId}/today")
    public ResponseEntity<TodayMissionResponse> getTodayMission(@PathVariable UUID userId) {
        User user = getUserOrThrow(userId);
        UserExercise active = getActiveExerciseOrThrow(user);

        LocalDate today = LocalDate.now();
        CheckIn todayCheckIn = checkInRepository.findByUserExerciseAndDate(active, today).orElse(null);
        int rollingCount = rollingStreakService.getRollingSuccessCount(userId, today);

        return ResponseEntity.ok(TodayMissionResponse.builder()
                .userExerciseId(active.getId())
                .exerciseName(active.getExercise().getName())
                .unit(active.getExercise().getUnit().name())
                .targetValue(active.getCurrentTarget())
                .completedValueToday(todayCheckIn != null ? todayCheckIn.getCompletedValue() : 0)
                .achievedToday(todayCheckIn != null && todayCheckIn.getAchieved())
                .currentWeek(active.getCurrentWeek())
                .rollingSuccessCount(rollingCount)
                .build());
    }

    @PostMapping("/{userId}/checkins")
    public ResponseEntity<Void> checkIn(@PathVariable UUID userId, @Valid @RequestBody CheckInRequest request) {
        User user = getUserOrThrow(userId);
        UserExercise active = getActiveExerciseOrThrow(user);

        checkInService.recordCheckIn(active, LocalDate.now(), request.getCompletedValue());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{userId}/exercises")
    public ResponseEntity<UUID> startNewExercise(@PathVariable UUID userId, @Valid @RequestBody StartExerciseRequest request) {
        User user = getUserOrThrow(userId);
        UserExercise userExercise = exerciseUnlockService.startNewExercise(user, request.getExerciseId());
        return ResponseEntity.ok(userExercise.getId());
    }

    @GetMapping("/{userId}/exercises")
    public ResponseEntity<List<UserExercise>> getExerciseHistory(@PathVariable UUID userId) {
        User user = getUserOrThrow(userId);
        return ResponseEntity.ok(userExerciseRepository.findAllByUserOrderByOrderIndexAsc(user));
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    private UserExercise getActiveExerciseOrThrow(User user) {
        return userExerciseRepository.findByUserAndStatus(user, UserExercise.Status.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("진행 중인 활성 동작이 없습니다. 새 동작을 시작해주세요."));
    }
}
