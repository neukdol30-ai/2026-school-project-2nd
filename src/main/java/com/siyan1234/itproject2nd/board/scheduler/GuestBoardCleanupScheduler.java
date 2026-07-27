package com.siyan1234.itproject2nd.board.scheduler;

import com.siyan1234.itproject2nd.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GuestBoardCleanupScheduler {

    private final BoardService boardService;

    /*
     * 답변 완료 후 30일이 지난
     * 비회원 문의글 자동 삭제
     *
     * 매일 새벽 3시에 실행
     *
     * cron 순서:
     * 초 분 시 일 월 요일
     */
    @Scheduled(
            cron = "0 0 3 * * *",
            zone = "Asia/Seoul"
    )
    public void deleteExpiredGuestBoards() {

        try {

            int deletedCount =
                    boardService.deleteExpiredGuestBoards();

            log.info(
                    "만료된 비회원 문의글 자동 삭제 완료: {}건",
                    deletedCount
            );

        } catch (Exception e) {

            log.error(
                    "비회원 문의글 자동 삭제 중 오류가 발생했습니다.",
                    e
            );
        }
    }
}
