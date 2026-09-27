package com.rk.fooddelivery.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbc;

    public DatabaseUserDetailsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return jdbc.query("""
            SELECT u.id, c.username, c.password_hash, u.name, u.email, u.role, u.active
            FROM user_credentials c
            JOIN users u ON u.id = c.user_id
            WHERE lower(c.username) = lower(?)
            """, rs -> {
            if (!rs.next()) {
                throw new UsernameNotFoundException("Unknown username");
            }
            return new AuthenticatedUser(
                rs.getObject("id", java.util.UUID.class),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("name"),
                rs.getString("email"),
                Role.fromDatabase(rs.getString("role")),
                rs.getBoolean("active"));
        }, username);
    }
}
