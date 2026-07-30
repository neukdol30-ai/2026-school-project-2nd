/*
 * 게시글 작성 페이지 문구 변경
 *
 * QUESTION:
 * 문의글 작성
 *
 * NOTICE:
 * 게시글 작성
 */
document.addEventListener(
    "DOMContentLoaded",
    function () {

        const categorySelect =
            document.querySelector(
                "#category"
            );

        const pageTitle =
            document.querySelector(
                "[data-board-write-title]"
            );

        const pageDescription =
            document.querySelector(
                "[data-board-write-description]"
            );

        const submitButton =
            document.querySelector(
                "[data-board-write-submit]"
            );

        const listLink =
            document.querySelector(
                "[data-board-write-list-link]"
            );

        const cancelLink =
            document.querySelector(
                "[data-board-write-cancel-link]"
            );


        /*
         * 카테고리에 따라 문구와 링크 변경
         */
        function updateWritePage(
            category
        ) {

            const isNotice =
                category === "NOTICE";

            /*
             * 브라우저 탭 제목
             */
            document.title =
                isNotice
                    ? "게시글 작성"
                    : "문의글 작성";


            /*
             * 화면 상단 제목
             */
            if (pageTitle) {
                pageTitle.textContent =
                    isNotice
                        ? "게시글 작성"
                        : "문의글 작성";
            }


            /*
             * 안내 문구
             */
            if (pageDescription) {
                pageDescription.textContent =
                    isNotice
                        ? "새로운 공지사항을 작성해주세요."
                        : "새로운 문의글을 작성해주세요.";
            }


            /*
             * 등록 버튼
             */
            if (submitButton) {
                submitButton.textContent =
                    isNotice
                        ? "게시글 등록"
                        : "문의글 등록";
            }


            /*
             * 상단 목록 링크
             */
            if (listLink) {
                listLink.textContent =
                    isNotice
                        ? "공지사항 목록으로"
                        : "문의 목록으로";

                listLink.setAttribute(
                    "href",
                    isNotice
                        ? "/board/notice"
                        : "/board/question"
                );
            }


            /*
             * 취소 링크
             */
            if (cancelLink) {
                cancelLink.setAttribute(
                    "href",
                    isNotice
                        ? "/board/notice"
                        : "/board/question"
                );
            }
        }


        /*
         * 최초 화면
         *
         * 비회원은 category 선택창이 없으므로
         * 자동으로 QUESTION을 사용한다.
         */
        const initialCategory =
            categorySelect
                ? categorySelect.value
                : "QUESTION";

        updateWritePage(
            initialCategory
        );


        /*
         * 관리자가 카테고리를 바꿀 때
         */
        if (categorySelect) {
            categorySelect.addEventListener(
                "change",
                function () {
                    updateWritePage(
                        categorySelect.value
                    );
                }
            );
        }
    }
);