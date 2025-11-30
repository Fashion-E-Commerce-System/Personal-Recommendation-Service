//package com.ecommerce.backend.batch;
//
//import com.ecommerce.backend.document.Order;
//import com.ecommerce.backend.service.RecommendationService;
//import com.ecommerce.backend.service.provider.OrderProvider;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.core.StepContribution;
//import org.springframework.batch.core.scope.context.ChunkContext;
//import org.springframework.batch.core.step.tasklet.Tasklet;
//import org.springframework.batch.repeat.RepeatStatus;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.Date;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class GenerateRecommendationsTasklet implements Tasklet {
//
//    private final RecommendationService recommendationService;
//
//    @Qualifier("mongoOrderProvider")
//    private final OrderProvider orderProvider;
//
//    @Override
//    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
//        List<Order> orders = orderProvider.getOrders(LocalDateTime.now());
//
//        if (orders.isEmpty())
//            return RepeatStatus.FINISHED;
//
//        LocalDateTime maxDate = orders.stream()
//                .map(Order::getDate)
//                .max(LocalDateTime::compareTo)
//                .orElse(LocalDateTime.now());
//
//        LocalDateTime testWeekStart = maxDate.minusWeeks(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
//        LocalDateTime trainingDataEnd = testWeekStart;
//        LocalDateTime trainingDataStart = trainingDataEnd.minusWeeks(4);
//
//        List<Order> testOrders = orders.stream()
//                .filter(t -> !t.getDate().isBefore(testWeekStart))
//                .collect(Collectors.toList());
//
//        List<Order> trainingOrders = orders.stream()
//                .filter(t -> !t.getDate().isBefore(trainingDataStart) && t.getDate().isBefore(trainingDataEnd))
//                .collect(Collectors.toList());
//
//        if (trainingOrders.isEmpty() || testOrders.isEmpty())
//            return RepeatStatus.FINISHED;
//
//        double map12 = recommendationService.generateAndSaveRecommendations(trainingOrders, testOrders);
//        contribution.getStepExecution().getJobExecution().getExecutionContext().putDouble("MAP @12_score", map12);
//        return RepeatStatus.FINISHED;
//    }
//}
package com.ecommerce.backend.batch;

import com.ecommerce.backend.document.Order;
import com.ecommerce.backend.service.RecommendationService;
import com.ecommerce.backend.service.provider.OrderProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateRecommendationsTasklet implements Tasklet {

    private final RecommendationService recommendationService;

    @Qualifier("mongoOrderProvider")
    private final OrderProvider orderProvider;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<Order> orders = orderProvider.getOrders(LocalDateTime.now());

        if (orders.isEmpty())
            return RepeatStatus.FINISHED;

        LocalDateTime maxDate = orders.stream()
                .map(Order::getDate)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        LocalDateTime testWeekStart = maxDate.minusWeeks(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime trainingDataEnd = testWeekStart;
        LocalDateTime trainingDataStart = trainingDataEnd.minusWeeks(4);

        List<Order> testOrders = orders.stream()
                .filter(t -> !t.getDate().isBefore(testWeekStart))
                .collect(Collectors.toList());

        List<Order> trainingOrders = orders.stream()
                .filter(t -> !t.getDate().isBefore(trainingDataStart) && t.getDate().isBefore(trainingDataEnd))
                .collect(Collectors.toList());

        if (trainingOrders.isEmpty() || testOrders.isEmpty())
            return RepeatStatus.FINISHED;

        double map12 = recommendationService.generateAndSaveRecommendations(trainingOrders, testOrders);
        contribution.getStepExecution().getJobExecution().getExecutionContext().putDouble("MAP @12_score", map12);
        return RepeatStatus.FINISHED;
    }
}