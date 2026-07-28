package com.siyan1234.itproject2nd.chat.scheduler;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import com.siyan1234.itproject2nd.chat.support.ChatRoomLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Redis에 임시 저장된 채팅 메시지를 Oracle DB로 저장하는 Scheduler입니다.
 *
 * 전체 상담방을 매번 DB에서 조회하지 않고 Redis의 chat:pending-rooms Set에
 * 등록된 상담방만 처리하여 상담방 수가 늘어났을 때의 불필요한 순회를 줄입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageScheduler {

    private final ChatService chatService;
    private final ChatRedisService chatRedisService;
    private final ChatRoomLockManager roomLockManager;

    /** 현재는 테스트 확인이 쉽도록 마지막 실행 완료 후 60초마다 수행합니다. */
    //@Scheduled(cron = "0 30 23 * * *", zone = "Asia/Seoul")
    @Scheduled(fixedDelay = 60000)
    public void saveRedisMessagesToOracle() {
        Set<Integer> pendingRoomNos = chatRedisService.findPendingRoomNos();

        if (pendingRoomNos.isEmpty()) {
            return;
        }

        for (Integer roomNo : pendingRoomNos) {
            try {
                int savedCount = roomLockManager.execute(roomNo, () -> flushRoom(roomNo));

                if (savedCount > 0) {
                    log.info("Redis 메시지 Oracle 일괄 저장 완료 roomNo={}, count={}", roomNo, savedCount);
                }
            } catch (Exception e) {
                // 실패한 상담방은 pending Set과 Redis List에 그대로 남겨 다음 실행에서 재시도합니다.
                log.error("Redis 메시지 Oracle 일괄 저장 실패 roomNo={}", roomNo, e);
            }
        }
    }

    private int flushRoom(Integer roomNo) {
        List<ChatMessageDto> messages = chatRedisService.findMessages(roomNo);

        if (messages.isEmpty()) {
            chatRedisService.cleanupPendingRoomIfEmpty(roomNo);
            return 0;
        }

        int savedCount = chatService.saveMessages(messages);
        if (savedCount != messages.size()) {
            throw new IllegalStateException("Redis 조회 개수와 Oracle 저장 개수가 일치하지 않습니다.");
        }

        chatRedisService.deleteSavedMessages(roomNo, messages);
        return savedCount;
    }
}
