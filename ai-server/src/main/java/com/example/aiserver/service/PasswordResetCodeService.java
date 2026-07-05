package com.example.aiserver.service;

import com.example.aiserver.common.BusinessException;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetCodeService {
    private static final int CODE_BOUND = 1_000_000;
    private static final int EXPIRE_MINUTES = 5;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, ResetCode> codes = new ConcurrentHashMap<>();

    public String createCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        String code = String.format("%06d", secureRandom.nextInt(CODE_BOUND));
        codes.put(normalizedEmail, new ResetCode(code, LocalDateTime.now().plusMinutes(EXPIRE_MINUTES)));
        return code;
    }

    public void verifyAndConsume(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        ResetCode resetCode = codes.get(normalizedEmail);
        if (resetCode == null) {
            throw new BusinessException("验证码不存在或已过期");
        }
        if (LocalDateTime.now().isAfter(resetCode.getExpireAt())) {
            codes.remove(normalizedEmail);
            throw new BusinessException("验证码已过期");
        }
        if (!resetCode.getCode().equals(code)) {
            throw new BusinessException("验证码不正确");
        }
        codes.remove(normalizedEmail);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    @Getter
    private static class ResetCode {
        private final String code;
        private final LocalDateTime expireAt;

        private ResetCode(String code, LocalDateTime expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
