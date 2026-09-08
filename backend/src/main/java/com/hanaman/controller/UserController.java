package com.hanaman.controller;

import com.hanaman.domain.*;
import com.hanaman.dto.*;
import com.hanaman.exception.UserNotFoundException;
import com.hanaman.repository.*;
import com.hanaman.service.CheckInService;
import com.hanaman.service.ExerciseUnlockService;
import com.hanaman.service.NotificationService;
import com.hanaman.service.RollingStreakService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserExerciseRepository userExerciseRepository;
    private final CheckInRepository checkInRepository;
    private final WeeklyGoalRepository weeklyGoalRepository;
    private final CycleSummaryRepository cycleSummaryRepository;
    private final CheckInService checkInService;
    private final ExerciseUnlockService exerciseUnlockService;
    private final RollingStreakService rollingStreakService;
    private final NotificationService notificationService;

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

        // 지난주 WeeklyGoal이 있으면(=2주차 이상이면) 목표 조정 안내에 쓸 정보를 같이 내려준다.
        // (같은 사이클 안에서만 비교 — 사이클이 막 연장된 직후엔 "지난주"가 없는 게 맞다)
        WeeklyGoal previousWeek = active.getCurrentWeek() > 1
                ? weeklyGoalRepository.findByUserExerciseAndCycleNumberAndWeekNumber(
                        active, active.getCycleNumber(), active.getCurrentWeek() - 1).orElse(null)
                : null;

        WeeklyGoal currentWeekGoal = weeklyGoalRepository
                .findByUserExerciseAndCycleNumberAndWeekNumber(active, active.getCycleNumber(), active.getCurrentWeek())
                .orElse(null);
        String notificationMessage = notificationService.buildTodayNotification(active, currentWeekGoal, today);
        String recordMessage = notificationService.buildRollingStreakRecordMessage(user, rollingCount, today);

        // 연장(EXTENDED)은 사용자가 결정할 게 없는 케이스라, 블로킹 화면 대신 여기서 바로 확인 처리하고
        // 오늘의 미션 화면에 배너로 한 번만 보여준다. (마스터는 다음 동작 선택이 필요해서 별도로 블로킹 처리 — getPendingCycleSummary 참고)
        CycleSummaryResponse extensionNotice = cycleSummaryRepository
                .findFirstByUserAndAcknowledgedFalseAndOutcomeOrderByCycleEndDateDesc(user, CycleSummary.Outcome.EXTENDED)
                .map(s -> {
                    s.setAcknowledged(true);
                    cycleSummaryRepository.save(s);
                    return toCycleSummaryResponse(s);
                })
                .orElse(null);

        return ResponseEntity.ok(TodayMissionResponse.builder()
                .userExerciseId(active.getId())
                .exerciseName(active.getExercise().getName())
                .unit(active.getExercise().getUnit().name())
                .targetValue(active.getCurrentTarget())
                .completedValueToday(todayCheckIn != null ? todayCheckIn.getCompletedValue() : 0)
                .achievedToday(todayCheckIn != null && todayCheckIn.getAchieved())
                .currentWeek(active.getCurrentWeek())
                .rollingSuccessCount(rollingCount)
                .previousWeekTarget(previousWeek != null ? previousWeek.getTargetValue() : null)
                .previousWeekSuccessDays(previousWeek != null ? previousWeek.getSuccessDays() : null)
                .notificationMessage(notificationMessage)
                .rollingStreakRecordMessage(recordMessage)
                .extensionNotice(extensionNotice)
                .build());
    }

    @GetMapping("/{userId}/rhythm")
    public ResponseEntity<List<RhythmDayResponse>> getRhythm(@PathVariable UUID userId) {
        User user = getUserOrThrow(userId);
        UserExercise active = getActiveExerciseOrThrow(user);

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);

        Map<LocalDate, CheckIn> checkInsByDate = checkInRepository
                .findAllByUserExerciseAndDateBetween(active, start, today).stream()
                .collect(Collectors.toMap(CheckIn::getDate, c -> c));

        List<RhythmDayResponse> days = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            CheckIn checkIn = checkInsByDate.get(date);
            days.add(RhythmDayResponse.builder()
                    .date(date)
                    .status(classify(checkIn))
                    .build());
        }
        return ResponseEntity.ok(days);
    }

    private String classify(CheckIn checkIn) {
        if (checkIn == null) {
            return "NONE";
        }
        int diff = checkIn.getCompletedValue() - checkIn.getTargetValueSnapshot();
        if (diff > 0) {
            return "EXCEEDED";
        }
        if (diff == 0) {
            return "EXACT";
        }
        return "BELOW";
    }

    @PostMapping("/{userId}/checkins")
    public ResponseEntity<Void> checkIn(@PathVariable UUID userId, @Valid @RequestBody CheckInRequest request) {
        User user = getUserOrThrow(userId);
        UserExercise active = getActiveExerciseOrThrow(user);

        checkInService.recordCheckIn(active, LocalDate.now(), request.getCompletedValue());
        return ResponseEntity.ok().build();
    }

    /**
     * 확인하지 않은 "마스터" 요약이 있으면 반환 — 없으면 204.
     * 마스터는 다음 동작을 골라야 하는 진짜 결정이 있어서 블로킹 화면으로 보여준다.
     * 연장(EXTENDED)은 여기서 다루지 않는다 — getTodayMission이 자동으로 확인 처리하고 배너로 보여준다.
     */
    @GetMapping("/{userId}/cycle-summary")
    public ResponseEntity<CycleSummaryResponse> getPendingCycleSummary(@PathVariable UUID userId) {
        User user = getUserOrThrow(userId);
        return cycleSummaryRepository
                .findFirstByUserAndAcknowledgedFalseAndOutcomeOrderByCycleEndDateDesc(user, CycleSummary.Outcome.MASTERED)
                .map(s -> ResponseEntity.ok(toCycleSummaryResponse(s)))
                .orElse(ResponseEntity.noContent().build());
    }

    private CycleSummaryResponse toCycleSummaryResponse(CycleSummary s) {
        return CycleSummaryResponse.builder()
                .id(s.getId())
                .exerciseName(s.getExerciseName())
                .successWeeks(s.getSuccessWeeks())
                .totalWeeks(s.getTotalWeeks())
                .outcome(s.getOutcome().name())
                .cycleEndDate(s.getCycleEndDate())
                .build();
    }

    @PostMapping("/{userId}/cycle-summary/{summaryId}/ack")
    public ResponseEntity<Void> acknowledgeCycleSummary(@PathVariable UUID userId, @PathVariable UUID summaryId) {
        User user = getUserOrThrow(userId);
        CycleSummary summary = cycleSummaryRepository.findById(summaryId)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사이클 요약입니다."));

        summary.setAcknowledged(true);
        cycleSummaryRepository.save(summary);
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
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));
    }

    private UserExercise getActiveExerciseOrThrow(User user) {
        return userExerciseRepository.findByUserAndStatus(user, UserExercise.Status.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("진행 중인 활성 동작이 없습니다. 새 동작을 시작해주세요."));
    }
}
