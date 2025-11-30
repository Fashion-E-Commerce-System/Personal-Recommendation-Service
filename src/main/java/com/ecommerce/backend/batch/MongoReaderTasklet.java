package com.ecommerce.backend.batch;

import com.ecommerce.backend.domain.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MongoReaderTasklet implements Tasklet {
    private final MongoTemplate mongoTemplate;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting to read Order documents from MongoDB.");

        List<Order> orderList = mongoTemplate.findAll(Order.class);

        if (orderList.size() > 1) {
            Order secondOrder = orderList.get(1);
            log.info("Successfully read Order documents. Order at index 1: {}", secondOrder);
        } else {
            log.warn("Order list size is less than 2, cannot print the item at index 1.");
        }
        log.info("Finished reading Order documents from MongoDB.");
        return RepeatStatus.FINISHED;
    }
}