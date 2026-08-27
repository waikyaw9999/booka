package com.booka.config;

import com.booka.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoUserPasswordInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String encoded = passwordEncoder.encode("password");
        userRepository.findByEmail("admin@booka.com").ifPresent(user -> {
            user.setPasswordHash(encoded);
            userRepository.save(user);
        });
        userRepository.findByEmail("staff@booka.com").ifPresent(user -> {
            user.setPasswordHash(encoded);
            userRepository.save(user);
        });
        log.info("Demo user passwords synchronized");
    }
}
