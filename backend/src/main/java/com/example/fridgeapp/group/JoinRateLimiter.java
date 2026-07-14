package com.example.fridgeapp.group;

import com.example.fridgeapp.common.AppError;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

/**
 * POST /groups/join 用のインメモリ・レート制限/ロック機構（単一プロセス前提、MVPスコープ）。
 *
 * <p>IPごとに 1 分あたり {@value #MAX_REQUESTS_PER_WINDOW} 回までのリクエストを許可し、連続 {@value
 * #FAILURE_LOCK_THRESHOLD} 回失敗した IP を {@link #IP_LOCK_DURATION} の間ブロックする。 招待コード自体の失敗回数・ロックは {@link
 * InvitationCode} 側（DB永続化）で別途管理する。
 */
@Component
public class JoinRateLimiter {

  private static final int MAX_REQUESTS_PER_WINDOW = 5;
  private static final Duration RATE_WINDOW = Duration.ofMinutes(1);
  private static final int FAILURE_LOCK_THRESHOLD = 10;
  private static final Duration IP_LOCK_DURATION = Duration.ofHours(3);

  private final ConcurrentHashMap<String, Deque<Instant>> requestTimestamps =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, FailureState> failureStates = new ConcurrentHashMap<>();

  /**
   * この IP からの参加リクエストを受け付けてよいか検証し、呼び出しを 1 回分として記録する。
   *
   * @throws GroupException ロック中、またはレート制限を超えた場合（{@link AppError#JOIN_RATE_LIMITED}）
   */
  public void assertAllowed(String clientIp) {
    assertNotLocked(clientIp);
    assertNotRateLimited(clientIp);
  }

  /** 参加失敗を記録する。連続失敗が閾値に達した IP はロックする。 */
  public void recordFailure(String clientIp) {
    failureStates.compute(
        clientIp,
        (ip, existing) -> {
          FailureState state = existing == null ? new FailureState() : existing;
          state.consecutiveFailures++;
          if (state.consecutiveFailures >= FAILURE_LOCK_THRESHOLD) {
            state.lockedUntil = Instant.now().plus(IP_LOCK_DURATION);
          }
          return state;
        });
  }

  /** 参加成功として連続失敗のカウントをリセットする。 */
  public void recordSuccess(String clientIp) {
    failureStates.remove(clientIp);
  }

  private void assertNotLocked(String clientIp) {
    FailureState state = failureStates.get(clientIp);
    if (state != null && state.lockedUntil != null && Instant.now().isBefore(state.lockedUntil)) {
      throw new GroupException(AppError.JOIN_RATE_LIMITED);
    }
  }

  private void assertNotRateLimited(String clientIp) {
    Instant now = Instant.now();
    Deque<Instant> timestamps =
        requestTimestamps.computeIfAbsent(clientIp, ip -> new ConcurrentLinkedDeque<>());
    synchronized (timestamps) {
      while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(now.minus(RATE_WINDOW))) {
        timestamps.pollFirst();
      }
      if (timestamps.size() >= MAX_REQUESTS_PER_WINDOW) {
        throw new GroupException(AppError.JOIN_RATE_LIMITED);
      }
      timestamps.addLast(now);
    }
  }

  private static final class FailureState {
    private int consecutiveFailures;
    private Instant lockedUntil;
  }
}
