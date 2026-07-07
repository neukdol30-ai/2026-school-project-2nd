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

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageScheduler {

    private final ChatService chatService;
    private final ChatRedisService chatRedisService;
    private final TransactionTemplate transactionTemplate;

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

            try {
                transactionTemplate.executeWithoutResult(status -> {
                    for (ChatMessageDto message : messages) {
                        chatService.saveMessage(message);
                    }
                });

                chatRedisService.deleteMessages(roomNo);

                log.info("Redis 메시지 Oracle 저장 완료 roomNo={}, count={}", roomNo, messages.size());
            } catch (Exception e) {
                log.error("Redis 메시지 Oracle 저장 실패 roomNo={}", roomNo, e);
            }
        }
    }
}