package com.rk.fooddelivery.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    private static final String CLEAR_DATABASE = """
        TRUNCATE TABLE order_items, payments, orders, cart_items, carts, menu_items,
        menu_categories, restaurant_cuisines, restaurant_timings, restaurants, cuisines,
        coupons, cities, users RESTART IDENTITY CASCADE
        """;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        String database = jdbc.queryForObject("SELECT current_database()", String.class);
        if (database == null || !database.endsWith("_assignment_test")) {
            throw new IllegalStateException("Refusing to clear a non-assignment test database: " + database);
        }
        jdbc.execute(CLEAR_DATABASE);
    }

    protected void committed(Consumer<JdbcTemplate> fixture) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> fixture.accept(jdbc));
    }
}
