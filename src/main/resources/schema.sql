-- Fill the Pockets MySQL Database Schema
-- Database: fill_the_pockets

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(150) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(30) NOT NULL,
    points_balance INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS employee_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    employee_code VARCHAR(30) NOT NULL UNIQUE,
    designation VARCHAR(100) NOT NULL,
    panchayat_name VARCHAR(100) NOT NULL,
    is_approved BOOLEAN NOT NULL DEFAULT TRUE,
    is_demo_account BOOLEAN NOT NULL DEFAULT FALSE,
    approved_at DATETIME,
    CONSTRAINT fk_employee_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS road_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_number VARCHAR(40) NOT NULL UNIQUE,
    villager_id BIGINT NOT NULL,
    road_name VARCHAR(150) NOT NULL,
    village_area VARCHAR(100) NOT NULL,
    description TEXT,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    image_path VARCHAR(255) NOT NULL,
    image_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    repair_status VARCHAR(30) NOT NULL,
    points_awarded INT NOT NULL DEFAULT 0,
    rejection_reason TEXT,
    points_transaction_id BIGINT,
    verified_by_employee_id BIGINT,
    verified_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_road_coords (latitude, longitude),
    CONSTRAINT fk_report_villager FOREIGN KEY (villager_id) REFERENCES users(id),
    CONSTRAINT fk_report_verified_employee FOREIGN KEY (verified_by_employee_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS ai_verification_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL UNIQUE,
    category VARCHAR(40) NOT NULL,
    confidence_score DOUBLE NOT NULL,
    top_labels VARCHAR(255),
    explanation TEXT,
    inference_time_ms BIGINT,
    verified_at DATETIME NOT NULL,
    CONSTRAINT fk_ai_report FOREIGN KEY (report_id) REFERENCES road_reports(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS points_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    report_id BIGINT,
    points INT NOT NULL,
    type VARCHAR(30) NOT NULL,
    description VARCHAR(255) NOT NULL,
    balance_after INT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_points_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS reward_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(255) NOT NULL,
    category VARCHAR(60) NOT NULL,
    points_required INT NOT NULL,
    coupon_code_prefix VARCHAR(20) NOT NULL,
    icon VARCHAR(60),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS reward_redemptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    redemption_code VARCHAR(40) NOT NULL UNIQUE,
    points_deducted INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    redeemed_at DATETIME NOT NULL,
    valid_until DATETIME NOT NULL,
    CONSTRAINT fk_redemption_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_redemption_coupon FOREIGN KEY (coupon_id) REFERENCES reward_coupons(id)
);

CREATE TABLE IF NOT EXISTS repair_status_updates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    previous_status VARCHAR(30) NOT NULL,
    new_status VARCHAR(30) NOT NULL,
    notes TEXT,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_audit_report FOREIGN KEY (report_id) REFERENCES road_reports(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_employee FOREIGN KEY (employee_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS otp_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(120) NOT NULL,
    otp_code VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    INDEX idx_otp_email (email, used)
);
