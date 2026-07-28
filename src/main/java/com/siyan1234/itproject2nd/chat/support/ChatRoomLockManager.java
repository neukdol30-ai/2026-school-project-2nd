package com.siyan1234.itproject2nd.chat.support;

import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 같은 상담방에서 동시에 실행되는 메시지 저장, 읽음 처리, Redis flush를 직렬화합니다.
 *
 * 상담방별 Lock 객체를 계속 생성하면 메모리가 증가할 수 있으므로
 * 고정된 개수의 stripe Lock을 상담방 번호에 따라 재사용합니다.
 */
@Component
public class ChatRoomLockManager {

    private static final int LOCK_STRIPE_COUNT = 64;

    private final Object[] locks = new Object[LOCK_STRIPE_COUNT];

    public ChatRoomLockManager() {
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new Object();
        }
    }

    public void execute(Integer roomNo, Runnable action) {
        synchronized (lockFor(roomNo)) {
            action.run();
        }
    }

    public <T> T execute(Integer roomNo, Supplier<T> action) {
        synchronized (lockFor(roomNo)) {
            return action.get();
        }
    }

    private Object lockFor(Integer roomNo) {
        int hash = roomNo == null ? 0 : roomNo.hashCode();
        int index = Math.floorMod(hash, locks.length);
        return locks[index];
    }
}
