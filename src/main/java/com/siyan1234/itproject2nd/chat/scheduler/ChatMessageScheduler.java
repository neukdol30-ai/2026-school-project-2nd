package com.siyan1234.itproject2nd.chat.scheduler;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatMessageScheduler {

    private final ChatService chatService;
    private final ChatRedisService chatRedisService;

    @Scheduled(fixedDelay = 60000)
    //@Scheduled(cron = "0 30 23 * * *", zone = "Asia/Seoul")
    public void saveRedisMessagesToOracle(){
        List<ChatRoomDto> rooms = chatService.findAllRooms();

        for (ChatRoomDto room : rooms){
            List<ChatMessageDto> messages =
                    chatRedisService.popAllMessages(room.getRoomNo());
            for (ChatMessageDto message : messages){
                chatService.saveMessage(message);
            }
        }
    }
}
