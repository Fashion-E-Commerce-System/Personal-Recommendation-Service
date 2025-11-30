package com.ecommerce.backend.batch;

import com.ecommerce.backend.document.Order;
import com.ecommerce.backend.document.OrderItem;
import com.ecommerce.backend.document.Recommendation;
import com.ecommerce.backend.service.provider.OrderProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Disabled
public class GenerateRecommendationsTaskletTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Qualifier("generateRecommendationsJob")
    @Autowired
    private Job job;

    @MockBean
    @Qualifier("mongoOrderProvider")
    private OrderProvider orderProvider;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final String user1 = "user1";
    private final String user2 = "user2";

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection("recommendations");
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection("recommendations");
    }

    @Test
    void testGenerateRecommendationsJob() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<Order> orders = List.of(
                createOrder(user1, now.minusWeeks(2), "101", "201"),
                createOrder(user1, now.minusWeeks(3), "102"),
                createOrder(user2, now.minusDays(3), "301", "401")
        );
        when(orderProvider.getOrders(any(LocalDateTime.class))).thenReturn(orders);

        // when
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("uuid", UUID.randomUUID().toString()) // Unique parameter for each run
                .toJobParameters();
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);

        // then
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
        Double map12 = jobExecution.getExecutionContext().getDouble("MAP @12_score");
        assertNotNull(map12);
        assertTrue(map12 >= 0.0);

        List<Recommendation> recommendations = mongoTemplate.findAll(Recommendation.class);
        assertFalse(recommendations.isEmpty());

        long user1Recs = recommendations.stream().filter(r -> r.getUsername().equals(user1)).count();
        assertTrue(user1Recs > 0);

        long user2Recs = recommendations.stream().filter(r -> r.getUsername().equals(user2)).count();
        assertTrue(user2Recs > 0);
    }

    private Order createOrder(String username, LocalDateTime date, String... productIds) {
        List<OrderItem> items = new java.util.ArrayList<>();
        for (String productId : productIds) {
            items.add(new OrderItem(productId, 2));
        }
        return new Order(UUID.randomUUID().toString(), username, items, date);
    }
}
