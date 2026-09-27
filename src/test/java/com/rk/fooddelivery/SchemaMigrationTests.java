package com.rk.fooddelivery;

import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
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

    @Test
    void paymentReasonIsOptionalAndStored() {
        UUID payment = id("""
            INSERT INTO payments (order_id, payment_method, amount)
            VALUES (?, 'UPI', 100) RETURNING id
            """, order);
        assertNull(db.queryForObject("SELECT status_reason FROM payments WHERE id = ?", String.class, payment));
        db.update("UPDATE payments SET status = 'Failed', status_reason = 'Declined' WHERE id = ?", payment);
        assertEquals("Declined", db.queryForObject("SELECT status_reason FROM payments WHERE id = ?", String.class, payment));
    }

    @Test
    void customerCanOnlyHaveOneCart() {
        cart();
        rejects("23505", "INSERT INTO carts (customer_id, restaurant_id) VALUES (?, ?)", customer, restaurant);
    }

    @Test
    void deletingCartRemovesItemsAndCreationHasDefaultTimestamp() {
        UUID cart = cart();
        cartItem(cart);
        assertNotNull(db.queryForObject("SELECT created_at FROM carts WHERE id = ?", java.sql.Timestamp.class, cart));
        db.update("DELETE FROM carts WHERE id = ?", cart);
        assertEquals(0, db.queryForObject("SELECT count(*) FROM cart_items WHERE cart_id = ?", Integer.class, cart));
    }

    @Test
    void cartCannotContainDuplicateMenuItems() {
        UUID cart = cart();
        cartItem(cart);
        rejects("23505", "INSERT INTO cart_items (cart_id, restaurant_id, menu_item_id, quantity) VALUES (?, ?, ?, 1)",
            cart, restaurant, item);
    }

    @Test
    void cartQuantityMustBePositive() {
        UUID cart = cart();
        rejects("23514", "INSERT INTO cart_items (cart_id, restaurant_id, menu_item_id, quantity) VALUES (?, ?, ?, 0)",
            cart, restaurant, item);
    }

    @Test
    void cartItemMustMatchMenuRestaurant() {
        UUID other = otherRestaurant();
        UUID cart = id("INSERT INTO carts (customer_id, restaurant_id) VALUES (?, ?) RETURNING id", customer, other);
        rejects("23503", "INSERT INTO cart_items (cart_id, restaurant_id, menu_item_id, quantity) VALUES (?, ?, ?, 1)",
            cart, other, item);
    }

    @Test
    void cartItemCannotLieAboutCartRestaurant() {
        UUID other = otherRestaurant();
        UUID cart = id("INSERT INTO carts (customer_id, restaurant_id) VALUES (?, ?) RETURNING id", customer, other);
        rejects("23503", "INSERT INTO cart_items (cart_id, restaurant_id, menu_item_id, quantity) VALUES (?, ?, ?, 1)",
            cart, restaurant, item);
    }

    @Test
    void nonEmptyCartCannotSwitchRestaurants() {
        UUID cart = cart();
        cartItem(cart);
        UUID other = otherRestaurant();
        rejects("23503", "UPDATE carts SET restaurant_id = ? WHERE id = ?", other, cart);
    }

    @Test
    void cartItemUpdatesRefreshTimestamp() {
        UUID cart = cart();
        UUID line = cartItem(cart);
        db.update("UPDATE cart_items SET quantity = 2, updated_at = '2000-01-01' WHERE id = ?", line);
        assertEquals(Boolean.TRUE, db.queryForObject(
            "SELECT quantity = 2 AND updated_at > '2000-01-02'::timestamptz FROM cart_items WHERE id = ?", Boolean.class, line));
    }

    @Test
    void userLocationIsOptionalAndStoresWgs84Point() {
        assertNull(db.queryForObject("SELECT ST_AsText(location::geometry) FROM users WHERE id = ?", String.class, customer));
        db.update("UPDATE users SET location = ST_GeogFromText('POINT(77 13)') WHERE id = ?", customer);
        assertEquals("POINT(77 13)", db.queryForObject(
            "SELECT ST_AsText(location::geometry) FROM users WHERE id = ?", String.class, customer));
        assertEquals(4326, db.queryForObject(
            "SELECT ST_SRID(location::geometry) FROM users WHERE id = ?", Integer.class, customer));
    }

    @Test
    void cookingDurationSupportsUnknownZeroAndPositiveSeconds() {
        assertNull(db.queryForObject("SELECT cook_duration_seconds FROM menu_items WHERE id = ?", Integer.class, item));
        db.update("UPDATE menu_items SET cook_duration_seconds = 0 WHERE id = ?", item);
        assertEquals(0, db.queryForObject("SELECT cook_duration_seconds FROM menu_items WHERE id = ?", Integer.class, item));
        db.update("UPDATE menu_items SET cook_duration_seconds = 900 WHERE id = ?", item);
        assertEquals(900, db.queryForObject("SELECT cook_duration_seconds FROM menu_items WHERE id = ?", Integer.class, item));
    }

    @Test
    void cookingDurationCannotBeNegative() {
        rejects("23514", "UPDATE menu_items SET cook_duration_seconds = -1 WHERE id = ?", item);
    }

    private UUID cart() {
        return id("INSERT INTO carts (customer_id, restaurant_id) VALUES (?, ?) RETURNING id", customer, restaurant);
    }

    private UUID cartItem(UUID cart) {
        return id("INSERT INTO cart_items (cart_id, restaurant_id, menu_item_id, quantity) VALUES (?, ?, ?, 1) RETURNING id",
            cart, restaurant, item);
    }

    private UUID otherRestaurant() {
        return id("""
            INSERT INTO restaurants (name, owner_id, cost_for_two, diet_type, address_line_1, city_id, location)
            SELECT 'Other Kitchen', owner_id, 200, 'Veg', address_line_1, city_id, location
            FROM restaurants WHERE id = ? RETURNING id
            """, restaurant);
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
