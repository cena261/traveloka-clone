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

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountLockoutScheduler {

    private final UserRepository userRepository;

    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    @Transactional
    public void unlockExpiredAccounts() {
        log.debug("Starting automatic account lockout expiration check");

        try {
            OffsetDateTime now = OffsetDateTime.now();

            List<User> expiredLockouts = userRepository.findByAccountLockedTrue().stream()
                .filter(user -> user.getLockedUntil() != null && user.getLockedUntil().isBefore(now))
                .toList();

            if (expiredLockouts.isEmpty()) {
                log.debug("No expired account lockouts found");
                return;
            }

            log.info("Found {} account(s) with expired lockout period", expiredLockouts.size());

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

            userRepository.saveAll(expiredLockouts);

            log.info("Successfully unlocked {} account(s)", expiredLockouts.size());

        } catch (Exception e) {
            log.error("Error occurred during automatic account unlock process", e);
            throw e; // Re-throw to trigger transaction rollback
        }
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour on the hour
    public void logSchedulerStatus() {
        log.info("AccountLockoutScheduler is active and running");

        long lockedCount = userRepository.findByAccountLockedTrue().size();
        log.info("Current locked accounts: {}", lockedCount);
    }
}
