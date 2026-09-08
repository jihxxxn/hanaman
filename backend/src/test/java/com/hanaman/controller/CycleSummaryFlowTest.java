package com.hanaman.controller;

import com.hanaman.domain.CycleSummary;
import com.hanaman.domain.Exercise;
import com.hanaman.domain.User;
import com.hanaman.domain.UserExercise;
import com.hanaman.domain.WeeklyGoal;
import com.hanaman.repository.CycleSummaryRepository;
import com.hanaman.repository.ExerciseRepository;
import com.hanaman.repository.UserExerciseRepository;
import com.hanaman.repository.UserRepository;
import com.hanaman.repository.WeeklyGoalRepository;
import com.hanaman.service.WeeklyEvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 사이클 종료 요약이 마스터/연장에 따라 다르게 노출되는지 API 레벨에서 검증한다 (Round 6 후속 반영).
 * - 마스터: /cycle-summary로 조회, 확인 전까지 계속 노출 (블로킹 화면용)
 * - 연장: /cycle-summary엔 안 뜨고, /today 조회 시 자동으로 확인 처리되며 한 번만 배너로 노출
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CycleSummaryFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WeeklyEvaluationService weeklyEvaluationService;
    @Autowired private UserRepository userRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private UserExerciseRepository userExerciseRepository;
    @Autowired private WeeklyGoalRepository weeklyGoalRepository;
    @Autowired private CycleSummaryRepository cycleSummaryRepository;

    @Test
    void 연장_요약은_블로킹하지_않고_today_조회_시_자동으로_확인되고_한_번만_배너로_내려온다() throws Exception {
        User user = userRepository.save(User.builder().nickname("연장흐름테스트").build());
        Exercise exercise = exerciseRepository.findAll().get(0);

        UserExercise ue = userExerciseRepository.save(UserExercise.builder()
                .user(user).exercise(exercise).status(UserExercise.Status.ACTIVE)
                .cycleStartDate(LocalDate.now().minusWeeks(3))
                .currentWeek(4).currentTarget(4).orderIndex(0)
                .build());

        for (int week = 1; week <= 3; week++) {
            weeklyGoalRepository.save(WeeklyGoal.builder()
                    .userExercise(ue).cycleNumber(1).weekNumber(week).targetValue(4)
                    .successDays(2).result(WeeklyGoal.Result.FAIL).build());
        }
        weeklyGoalRepository.save(WeeklyGoal.builder()
                .userExercise(ue).cycleNumber(1).weekNumber(4).targetValue(4)
                .successDays(2).result(WeeklyGoal.Result.IN_PROGRESS).build());

        weeklyEvaluationService.finalizeWeek(ue, LocalDate.now());

        // 마스터가 아니므로 블로킹용 엔드포인트에는 안 뜬다.
        mockMvc.perform(get("/api/users/{userId}/cycle-summary", user.getId()))
                .andExpect(status().isNoContent());

        // 오늘의 미션 조회 시 배너 정보로 딸려오고, 동시에 자동으로 확인 처리된다.
        mockMvc.perform(get("/api/users/{userId}/today", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extensionNotice.outcome").value("EXTENDED"))
                .andExpect(jsonPath("$.extensionNotice.successWeeks").value(0));

        boolean stillUnacknowledged = cycleSummaryRepository
                .findFirstByUserAndAcknowledgedFalseAndOutcomeOrderByCycleEndDateDesc(user, CycleSummary.Outcome.EXTENDED)
                .isPresent();
        assertThat(stillUnacknowledged).isFalse();

        // 두 번째 조회부터는 이미 확인된 요약이라 더 이상 안 뜬다.
        mockMvc.perform(get("/api/users/{userId}/today", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extensionNotice").value(nullValue()));
    }
}
