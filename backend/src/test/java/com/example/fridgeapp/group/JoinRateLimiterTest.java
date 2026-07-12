package com.example.fridgeapp.group;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.fridgeapp.common.AppError;
import org.junit.jupiter.api.Test;

class JoinRateLimiterTest {

  @Test
  void allowsUpToFiveRequestsPerMinute() {
    JoinRateLimiter limiter = new JoinRateLimiter();
    String ip = "192.0.2.1";

    for (int i = 0; i < 5; i++) {
      assertThatCode(() -> limiter.assertAllowed(ip)).doesNotThrowAnyException();
    }

    assertThatThrownBy(() -> limiter.assertAllowed(ip))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.JOIN_RATE_LIMITED);
  }

  @Test
  void locksIpAfterTenConsecutiveFailures() {
    JoinRateLimiter limiter = new JoinRateLimiter();
    String ip = "192.0.2.2";

    for (int i = 0; i < 10; i++) {
      limiter.recordFailure(ip);
    }

    assertThatThrownBy(() -> limiter.assertAllowed(ip))
        .isInstanceOf(GroupException.class)
        .extracting("error")
        .isEqualTo(AppError.JOIN_RATE_LIMITED);
  }

  @Test
  void recordSuccessResetsFailureCount() {
    JoinRateLimiter limiter = new JoinRateLimiter();
    String ip = "192.0.2.3";

    for (int i = 0; i < 9; i++) {
      limiter.recordFailure(ip);
    }
    limiter.recordSuccess(ip);
    limiter.recordFailure(ip);

    assertThatCode(() -> limiter.assertAllowed(ip)).doesNotThrowAnyException();
  }

  @Test
  void differentIpsAreTrackedIndependently() {
    JoinRateLimiter limiter = new JoinRateLimiter();

    for (int i = 0; i < 5; i++) {
      limiter.assertAllowed("192.0.2.10");
    }

    assertThatCode(() -> limiter.assertAllowed("192.0.2.11")).doesNotThrowAnyException();
  }
}
