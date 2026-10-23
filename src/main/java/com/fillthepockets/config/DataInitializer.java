package com.fillthepockets.config;

import com.fillthepockets.entity.*;
import com.fillthepockets.repository.EmployeeProfileRepository;
import com.fillthepockets.repository.RewardCouponRepository;
import com.fillthepockets.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private RewardCouponRepository rewardCouponRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.demo.default-password:Demo@Emp123}")
    private String defaultEmployeePassword;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.full-name:Administrator}")
    private String adminFullName;

    @Override
    public void run(String... args) {
        seedConfiguredAdmin();
        seedSampleEmployees();
        seedSampleRewardCoupons();
    }

    private void seedConfiguredAdmin() {
        String email = adminEmail.trim().toLowerCase();
        String password = adminPassword;

        if (email.isEmpty() && password.isBlank()) {
            return;
        }
        if (email.isEmpty() || password.isBlank()) {
            throw new IllegalStateException(
                    "Both app.admin.email and app.admin.password must be configured to create an administrator.");
        }

        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            if (user.getRole() != Role.ADMIN) {
                throw new IllegalStateException(
                        "Configured administrator email belongs to a non-administrator account.");
            }
        }, () -> {
            User admin = new User(
                    email,
                    passwordEncoder.encode(password),
                    adminFullName.trim(),
                    null,
                    Role.ADMIN
            );
            userRepository.save(admin);
            log.info("Provisioned the configured administrator account.");
        });
    }

    private void seedSampleEmployees() {
        String[][] demoEmployees = {
            {"EMP001", "Arun Kumar", "arun.kumar@fillthepockets.demo", "Executive Officer", "Sundarapuram Gram Panchayat", "+91 98765 43210"},
            {"EMP002", "Anjali Mohan", "anjali.mohan@fillthepockets.demo", "Assistant Engineer (Civil)", "Sundarapuram Gram Panchayat", "+91 98765 43211"},
            {"EMP003", "Rahul Joseph", "rahul.joseph@fillthepockets.demo", "Road & Infrastructure Overseer", "Sundarapuram Gram Panchayat", "+91 98765 43212"},
            {"EMP004", "Meera Thomas", "meera.thomas@fillthepockets.demo", "Panchayat Secretary", "Sundarapuram Gram Panchayat", "+91 98765 43213"},
            {"EMP005", "Nikhil Raj", "nikhil.raj@fillthepockets.demo", "Field Works Inspector", "Sundarapuram Gram Panchayat", "+91 98765 43214"}
        };

        for (String[] empData : demoEmployees) {
            String code = empData[0];
            String name = empData[1];
            String email = empData[2];
            String designation = empData[3];
            String panchayat = empData[4];
            String phone = empData[5];

            if (!userRepository.existsByEmail(email)) {
                User user = new User(email, passwordEncoder.encode(defaultEmployeePassword), name, phone, Role.PANCHAYAT_EMPLOYEE);
                user = userRepository.save(user);

                EmployeeProfile profile = new EmployeeProfile(user, code, designation, panchayat, true, true);
                employeeProfileRepository.save(profile);

                log.info("Seeded Demo Employee: {} ({}) - {}", name, code, email);
            }
        }
    }

    private void seedSampleRewardCoupons() {
        if (rewardCouponRepository.count() == 0) {
            List<RewardCoupon> coupons = List.of(
                new RewardCoupon(
                    "Village Cooperative Grocery Voucher",
                    "Redeem Rs. 100 off on daily essentials and groceries at Sundarapuram Village Cooperative Store.",
                    "Grocery Voucher",
                    40,
                    "COOP100",
                    "shopping_bag"
                ),
                new RewardCoupon(
                    "Agricultural Supplies Subsidy Coupon",
                    "Get 15% discount on seeds, fertilizers, and farming tools at the Grama Seva Kendra.",
                    "Farming & Agri",
                    60,
                    "AGRI15",
                    "agriculture"
                ),
                new RewardCoupon(
                    "Solar Emergency Lamp Subsidy",
                    "Citizen reward for 50% discount on portable solar lantern for rural household resilience.",
                    "Green Energy",
                    100,
                    "SOLAR50",
                    "lightbulb"
                ),
                new RewardCoupon(
                    "Panchayat Community Hall Booking Credit",
                    "Rs. 250 fee waiver coupon for booking the village community hall or sports facility.",
                    "Public Amenity",
                    140,
                    "HALL250",
                    "holiday_village"
                )
            );
            rewardCouponRepository.saveAll(coupons);
            log.info("Seeded 4 sample demo reward coupons for college demonstration.");
        }
    }
}
