package com.ecommerce.backend.service.provider;

import com.ecommerce.backend.document.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Qualifier("mongoTransactionProvider")
@RequiredArgsConstructor
public class MongoOrderProvider implements OrderProvider {

    private final MongoTemplate mongoTemplate;
    private static final String TRANSACTIONS_COLLECTION = "orders";

    @Override
    public List<Order> getOrders(LocalDateTime date) {
        LocalDateTime fiveWeeksAgo = date.minusWeeks(5);
        Query query = new Query().addCriteria(Criteria.where("date").gte(fiveWeeksAgo));
        return mongoTemplate.find(query, Order.class, TRANSACTIONS_COLLECTION);
    }
}