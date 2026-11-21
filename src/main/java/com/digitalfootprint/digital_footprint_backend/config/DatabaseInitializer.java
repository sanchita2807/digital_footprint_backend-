package com.digitalfootprint.digital_footprint_backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Database initializer that runs on application startup.
 * Adds the is_active and is_admin columns if they don't exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("🔍 Checking database schema...");
            
            // Check and add is_active column
            addIsActiveColumn();
            
            // Check and add is_admin column
            addIsAdminColumn();

            // Drop legacy role column if it exists
            dropRoleColumn();
            
            log.info("✅ Database schema check complete!");
            
        } catch (Exception e) {
            log.error("❌ Error initializing database: {}", e.getMessage(), e);
            // Don't fail startup - log error and continue
        }
    }
    
    private void addIsActiveColumn() {
        try {
            String checkColumnQuery = 
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'users' AND column_name = 'is_active'";
            
            Integer count = jdbcTemplate.queryForObject(checkColumnQuery, Integer.class);
            
            if (count == null || count == 0) {
                log.info("Adding is_active column to users table...");
                
                // Add the column with default true (active)
                jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true"
                );
                
                // Add index for performance
                jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active)"
                );
                
                log.info("✅ Successfully added is_active column (default: true)");
            } else {
                log.info("✅ is_active column already exists");
            }
        } catch (Exception e) {
            log.error("❌ Error adding is_active column: {}", e.getMessage());
        }
    }
    
    private void addIsAdminColumn() {
        try {
            String checkColumnQuery = 
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'users' AND column_name = 'is_admin'";
            
            Integer count = jdbcTemplate.queryForObject(checkColumnQuery, Integer.class);
            
            if (count == null || count == 0) {
                log.info("Adding is_admin column to users table...");
                
                // Add the column
                jdbcTemplate.execute(
                    "ALTER TABLE users ADD COLUMN is_admin INTEGER NOT NULL DEFAULT 0"
                );
                
                // Add index for performance
                jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_users_is_admin ON users(is_admin)"
                );
                
                // Update existing users to ensure they have isAdmin = 0
                jdbcTemplate.execute(
                    "UPDATE users SET is_admin = 0 WHERE is_admin IS NULL"
                );
                
                log.info("✅ Successfully added is_admin column (0=User, 1=Admin)");
                log.info("✅ All existing users set to isAdmin = 0 (regular users)");
                log.info("ℹ️  To create admin: UPDATE users SET is_admin = 1 WHERE phone_number = 'YOUR_PHONE'");
            } else {
                log.info("✅ is_admin column already exists");
            }
        } catch (Exception e) {
            log.error("❌ Error adding is_admin column: {}", e.getMessage());
        }
    }

    private void dropRoleColumn() {
        try {
            String checkColumnQuery = 
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'users' AND column_name = 'role'";
            
            Integer count = jdbcTemplate.queryForObject(checkColumnQuery, Integer.class);
            
            if (count != null && count > 0) {
                log.info("Dropping legacy 'role' column from users table...");
                jdbcTemplate.execute("ALTER TABLE users DROP COLUMN IF EXISTS role");
                log.info("✅ Successfully dropped legacy 'role' column from database");
            } else {
                log.info("✅ Legacy 'role' column already removed from database");
            }
        } catch (Exception e) {
            log.warn("⚠️ Note on dropping 'role' column: {}", e.getMessage());
        }
    }
}
