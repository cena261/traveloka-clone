package com.cena.traveloka.iam.scheduler;

import com.cena.traveloka.common.enums.Status;
import com.cena.traveloka.iam.entity.User;
import com.cena.traveloka.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled job for automatic account lockout expiration.
 * <p>
 * This scheduler runs every 5 minutes to check for users whose account lockout
 * period has expired and automatically unlocks them.
 * </p>
 *
 * <p>Business Rules:</p>
 * <ul>
 *   <li>Runs every 5 minutes (300,000 milliseconds)</li>
 *   <li>Checks users with accountLocked = true</li>
 *   <li>Unlocks accounts where lockedUntil timestamp is in the past</li>
 *   <li>Resets failedLoginAttempts to 0 upon unlock</li>
 *   <li>Sets accountLocked to false</li>
 * </ul>
 *
 * @author Traveloka IAM Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountLockoutScheduler {

    private final UserRepository userRepository;

    /**
     * Scheduled task to unlock expired account lockouts.
     * <p>
     * This method runs every 5 minutes (fixedRate = 300000ms) to automatically
     * unlock user accounts whose lockout period has expired.
     * </p>
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Query all locked users where lockedUntil is before current time</li>
     *   <li>For each user:
     *     <ul>
     *       <li>Set accountLocked to false</li>
     *       <li>Clear lockedUntil timestamp</li>
     *       <li>Reset failedLoginAttempts to 0</li>
     *     </ul>
     *   </li>
     *   <li>Batch update all unlocked users</li>
     *   <li>Log the number of accounts unlocked</li>
     * </ol>
     *
     * <p>Scheduling Configuration:</p>
     * <ul>
     *   <li>Fixed Rate: 300,000 milliseconds (5 minutes)</li>
     *   <li>Initial Delay: 60,000 milliseconds (1 minute after startup)</li>
     * </ul>
     *
     * <p>Transaction Management:</p>
     * The method is transactional to ensure all updates are atomic. If any error
     * occurs during the unlock process, all changes are rolled back.
     *
     * @throws org.springframework.dao.DataAccessException if database operation fails
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    @Transactional
    public void unlockExpiredAccounts() {
        log.debug("Starting automatic account lockout expiration check");

        try {
            OffsetDateTime now = OffsetDateTime.now();

            // Find all locked users whose lockout period has expired
            // Note: Using accountLocked field instead of status
            List<User> expiredLockouts = userRepository.findByAccountLockedTrue().stream()
                .filter(user -> user.getLockedUntil() != null && user.getLockedUntil().isBefore(now))
                .toList();

            if (expiredLockouts.isEmpty()) {
                log.debug("No expired account lockouts found");
                return;
            }

            log.info("Found {} account(s) with expired lockout period", expiredLockouts.size());

            // Unlock each account
            for (User user : expiredLockouts) {
                log.info("Unlocking account for user: {} (ID: {}), locked until: {}",
                    user.getEmail(),
                    user.getId(),
                    user.getLockedUntil()
                );

                user.setAccountLocked(false);
                user.setLockedUntil(null);
                user.setFailedLoginAttempts(0);
                user.setLockReason(null);
            }

            // Batch save all unlocked users
            userRepository.saveAll(expiredLockouts);

            log.info("Successfully unlocked {} account(s)", expiredLockouts.size());

        } catch (Exception e) {
            log.error("Error occurred during automatic account unlock process", e);
            throw e; // Re-throw to trigger transaction rollback
        }
    }

    /**
     * Scheduled task to log scheduler health status.
     * <p>
     * This method runs every hour to confirm the scheduler is active and functioning.
     * Useful for monitoring and debugging purposes.
     * </p>
     *
     * @since 1.0.0
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour on the hour
    public void logSchedulerStatus() {
        log.info("AccountLockoutScheduler is active and running");

        // Count currently locked accounts
        long lockedCount = userRepository.findByAccountLockedTrue().size();
        log.info("Current locked accounts: {}", lockedCount);
    }
}
