package com.rk.fooddelivery;

import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SchemaMigrationTests {
    @Autowired JdbcTemplate db;

    UUID customer;
    UUID restaurant;
    UUID category;
    UUID item;
    UUID order;

    @BeforeEach
    void fixtures() {
        customer = id("""
            INSERT INTO users (name, email, phone_number, role)
            VALUES ('Customer', 'schema@example.test', '+919876543210', 'customer') RETURNING id
            """);
        UUID owner = id("""
            INSERT INTO users (name, email, phone_number, role)
            VALUES ('Owner', 'owner@example.test', '+919876543211', 'restaurant_owner') RETURNING id
            """);
        UUID city = id("""
            INSERT INTO cities (name, state, country, currency)
            VALUES ('Schema City', 'State', 'India', 'INR') RETURNING id
            """);
        restaurant = id("""
            INSERT INTO restaurants (name, owner_id, cost_for_two, diet_type, address_line_1, city_id, location)
            VALUES ('Kitchen', ?, 200, 'Veg', 'Street', ?, ST_GeogFromText('POINT(77 13)')) RETURNING id
            """, owner, city);
        category = id("""
            INSERT INTO menu_categories (restaurant_id, name) VALUES (?, 'Main') RETURNING id
            """, restaurant);
        item = id("""
            INSERT INTO menu_items (restaurant_id, category_id, name, diet_type, price, available_quantity)
            VALUES (?, ?, 'Meal', 'Veg', 100, 2) RETURNING id
            """, restaurant, category);
        order = id("""
            INSERT INTO orders (customer_id, restaurant_id, sub_total_amount, total_amount,
                address_line_1, city, state, country, location)
            VALUES (?, ?, 100, 100, 'Street', 'Schema City', 'State', 'India',
                ST_GeogFromText('POINT(77 13)')) RETURNING id
            """, customer, restaurant);
    }

    @Test
    void customerEmailIsUniqueIgnoringCaseWithinRole() {
        rejects("23505", """
            INSERT INTO users (name, email, phone_number, role)
            VALUES ('Duplicate', 'SCHEMA@example.test', '+919876543212', 'customer')
            """);
    }

    @Test
    void sameContactCanHaveAnotherRole() {
        assertNotNull(id("""
            INSERT INTO users (name, email, phone_number, role)
            VALUES ('Partner', 'schema@example.test', '+919876543210', 'delivery_partner') RETURNING id
            """));
    }

    @Test
    void menuCannotUseCategoryFromAnotherRestaurant() {
        UUID other = id("""
            INSERT INTO restaurants (name, owner_id, cost_for_two, diet_type, address_line_1, city_id, location)
            SELECT 'Other Kitchen', owner_id, 200, 'Veg', address_line_1, city_id, location
            FROM restaurants WHERE id = ? RETURNING id
            """, restaurant);
        rejects("23503", "UPDATE menu_items SET restaurant_id = ? WHERE id = ?", other, item);
    }

    @Test
    void stockCannotBecomeNegative() {
        rejects("23514", "UPDATE menu_items SET available_quantity = -1 WHERE id = ?", item);
    }

    @Test
    void orderTotalMustMatchItsComponents() {
        rejects("23514", "UPDATE orders SET total_amount = 101 WHERE id = ?", order);
    }

    @Test
    void orderLineMustMatchQuantityAndPrice() {
        rejects("23514", """
            INSERT INTO order_items (order_id, menu_item_id, quantity, unit_price, sub_total)
            VALUES (?, ?, 2, 100, 100)
            """, order, item);
    }

    @Test
    void failedPaymentCanBeRetriedButOnlyOneCanSucceed() {
        db.update("""
            INSERT INTO payments (order_id, status, payment_method, amount)
            VALUES (?, 'Failed', 'UPI', 100), (?, 'Success', 'UPI', 100)
            """, order, order);
        rejects("23505", """
            INSERT INTO payments (order_id, status, payment_method, amount)
            VALUES (?, 'Success', 'Card', 100)
            """, order);
    }

    @Test
    void percentageDiscountCannotExceedOneHundred() {
        rejects("23514", """
            INSERT INTO coupons (code, name, type, discount_value)
            VALUES ('INVALID', 'Invalid discount', 'percentage', 101)
            """);
    }

    @Test
    void overnightOpeningHoursAreSupported() {
        assertNotNull(id("""
            INSERT INTO restaurant_timings (restaurant_id, day, start_time, end_time, is_open)
            VALUES (?, 'Monday', '20:00', '02:00', true) RETURNING id
            """, restaurant));
    }

    @Test
    void updatesRefreshAuditTimestamp() {
        db.update("UPDATE users SET updated_at = '2000-01-01', name = 'Updated' WHERE id = ?", customer);
        assertEquals(Boolean.TRUE, db.queryForObject(
            "SELECT updated_at > '2000-01-02'::timestamptz FROM users WHERE id = ?", Boolean.class, customer));
    }

    @Test
    void orderStoresOptionalRestaurantAndDeliveryPartnerRatings() {
        assertEquals(1, db.update("""
            UPDATE orders SET order_status = 'Delivered', order_rating = 5,
                order_rating_review = 'Great meal', delivery_partner_rating = 4 WHERE id = ?
            """, order));
        assertEquals(4, db.queryForObject("SELECT delivery_partner_rating FROM orders WHERE id = ?",
            Integer.class, order));
    }

    @Test
    void ratingsMustBeBetweenOneAndFive() {
        rejects("23514", "UPDATE orders SET order_rating = 6 WHERE id = ?", order);
    }

    @Test
    void deliveryPartnerRatingMustBeBetweenOneAndFive() {
        rejects("23514", "UPDATE orders SET delivery_partner_rating = 0 WHERE id = ?", order);
    }

    private UUID id(String sql, Object... args) {
        return db.queryForObject(sql, UUID.class, args);
    }

    private void rejects(String sqlState, String sql, Object... args) {
        var error = assertThrows(DataIntegrityViolationException.class, () -> db.update(sql, args));
        var cause = assertInstanceOf(SQLException.class, error.getMostSpecificCause());
        assertEquals(sqlState, cause.getSQLState());
    }
}
