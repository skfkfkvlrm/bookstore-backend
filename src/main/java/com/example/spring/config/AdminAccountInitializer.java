package com.example.spring.config;

import com.example.spring.domain.model.Member;
import com.example.spring.domain.model.MembershipType;
import com.example.spring.domain.model.Role;
import com.example.spring.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 관리자 계정이 없으면 생성
        if (!memberRepository.existsByEmail("admin@bookstore.com")) {
            Member admin = Member.builder()
                    .email("admin@bookstore.com")
                    .name("시스템관리자")
                    .password(passwordEncoder.encode("admin123!"))
                    .role(Role.ADMIN)
                    .membershipType(MembershipType.PREMIUM)
                    .joinDate(LocalDateTime.now())
                    .build();
            memberRepository.save(admin);
            log.info("Registered Admin Account: admin@bookstore.com / admin123!");
        }

        // 테스트 일반 사용자 계정이 없으면 생성
        if (!memberRepository.existsByEmail("user@bookstore.com")) {
            Member user = Member.builder()
                    .email("user@bookstore.com")
                    .name("일반사용자")
                    .password(passwordEncoder.encode("user123!"))
                    .role(Role.USER)
                    .membershipType(MembershipType.REGULAR)
                    .joinDate(LocalDateTime.now())
                    .build();
            memberRepository.save(user);
            log.info("Registered User Account: user@bookstore.com / user123!");
        }
    }
}
