package com.siyan1234.itproject2nd.chat.scheduler;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Redis에 임시 저장된 채팅 메시지를 Oracle DB로 저장하는 Scheduler
 *
 * WebSocket으로 받은 메시지는 Redis에 먼저 저장된다.
 * 이 Scheduler가 일정 주기마다 Redis 메시지를 Oracle chat_message 테이블로 옮긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageScheduler {

    private final ChatService chatService;
    private final ChatRedisService chatRedisService;
    private final TransactionTemplate transactionTemplate;

    /**
     * Redis 메시지 Oracle 저장 작업
     *
     * 현재는 테스트 확인이 쉽도록 60초마다 실행한다.
     * 운영 또는 발표용으로는 cron 설정으로 변경할 수 있다.
     */
    //@Scheduled(cron = "0 30 23 * * *", zone = "Asia/Seoul")
    @Scheduled(fixedDelay = 60000)
    public void saveRedisMessagesToOracle() {
        List<ChatRoomDto> rooms = chatService.findAllRooms();

        for (ChatRoomDto room : rooms) {
            Integer roomNo = room.getRoomNo();

            List<ChatMessageDto> messages = chatRedisService.findMessages(roomNo);

            if (messages.isEmpty()) {
                continue;
            }

            int savedCount = messages.size();

            try {
                transactionTemplate.executeWithoutResult(status -> {
                    for (ChatMessageDto message : messages) {
                        chatService.saveMessage(message);
                    }
                });

                // 저장 완료한 메시지 개수만큼만 Redis에서 제거
                // Scheduler 저장 중 새로 들어온 메시지가 삭제되는 것을 방지한다.
                chatRedisService.deleteSavedMessages(roomNo, savedCount);

                log.info("Redis 메시지 Oracle 저장 완료 roomNo={}, count={}", roomNo, savedCount);
            } catch (Exception e) {
                log.error("Redis 메시지 Oracle 저장 실패 roomNo={}", roomNo, e);
            }
        }
    }
}