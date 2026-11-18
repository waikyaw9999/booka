package com.booka.config;

import com.booka.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoUserPasswordInitializer implements ApplicationRunner {

    static final List<String> DEMO_EMAILS = List.of(
            "admin@booka.com",
            "staff@booka.com",
            "harbour.admin@booka.com",
            "harbour.staff@booka.com",
            "inle.admin@booka.com",
            "inle.staff@booka.com",
            "bagan.admin@booka.com",
            "bagan.staff@booka.com",
            "mandalay.admin@booka.com",
            "mandalay.staff@booka.com"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String encoded = passwordEncoder.encode("password");
        for (String email : DEMO_EMAILS) {
            userRepository.findByEmail(email).ifPresent(user -> {
                user.setPasswordHash(encoded);
                userRepository.save(user);
            });
        }
        log.info("Demo user passwords synchronized");
    }
}
