document.addEventListener("DOMContentLoaded", function () {

    /*
     * 현재 페이지의 게시글 작성·수정 폼
     */
    const boardForms =
        document.querySelectorAll(
            "[data-board-form]"
        );

    if (boardForms.length === 0) {
        return;
    }

    /*
     * Toast UI 라이브러리 확인
     */
    if (
        typeof toastui === "undefined"
        || typeof toastui.Editor === "undefined"
    ) {
        console.error(
            "Toast UI Editor 라이브러리가 로드되지 않았습니다."
        );

        return;
    }

    boardForms.forEach(function (boardForm) {

        const editorElement =
            boardForm.querySelector(
                "[data-board-editor]"
            );

        const contentInput =
            boardForm.querySelector(
                "[data-board-content]"
            );

        const titleInput =
            boardForm.querySelector(
                "[data-board-title]"
            );

        const titleCountElement =
            boardForm.querySelector(
                "[data-board-title-count]"
            );

        const contentCountElement =
            boardForm.querySelector(
                "[data-board-content-count]"
            );

        const contentLimitTextElement =
            boardForm.querySelector(
                "[data-board-content-limit-text]"
            );

        const categorySelect =
            boardForm.querySelector(
                '[name="category"]'
            );

        if (!editorElement || !contentInput) {
            return;
        }

        /*
         * HTML의 data 속성에서 제한값 가져오기
         */
        const titleMaxLength =
            Number(
                boardForm.dataset.titleMaxLength
            ) || 200;

        /*
         * 카테고리별 실제 표시 글자 수 제한
        */
        const QUESTION_CONTENT_MAX_LENGTH =
            1000;

        const NOTICE_CONTENT_MAX_LENGTH =
            10000;

        /*
         * 현재 적용할 본문 제한
         */
        let contentMaxLength =
            Number(
                boardForm.dataset.contentMaxLength
            ) || QUESTION_CONTENT_MAX_LENGTH;

        /*
         * 태그와 이미지 주소를 포함한
         * HTML 전체 길이 제한
         */
        const contentHtmlMaxLength =
            Number(
                boardForm.dataset.contentHtmlMaxLength
            ) || 30000;

        let editor = null;

        /*
         * 글자 수 제한을 넘기기 전
         * 마지막 정상 HTML 내용
         */
        let lastValidHtml = "";

        /*
         * 직전 실제 글자 수
         */
        let previousTextLength = 0;

        /*
         * 내용 복구 중 change 이벤트 중복 실행 방지
         */
        let isRestoringContent = false;

        /*
         * HTML에서 화면에 보이는 글자 추출
         */
        function getPlainText(htmlContent) {

            const temporaryElement =
                document.createElement("div");

            temporaryElement.innerHTML =
                htmlContent || "";

            return (
                temporaryElement.textContent
                || temporaryElement.innerText
                || ""
            )
                .replace(/\u00A0/g, " ")
                .trim();
        }

        /*
         * 글자 수 표시 색상 변경
         */
        function updateCounterStyle(
            countElement,
            currentLength,
            maxLength
        ) {
            if (!countElement) {
                return;
            }

            const countArea =
                countElement.closest(
                    ".board-form-count"
                );

            if (!countArea) {
                return;
            }

            countArea.classList.toggle(
                "is-over",
                currentLength > maxLength
            );
        }

        /*
         * 제목 글자 수 표시
         */
        function updateTitleCount() {

            if (!titleInput
                || !titleCountElement) {
                return;
            }

            const currentLength =
                titleInput.value.length;

            titleCountElement.textContent =
                currentLength.toLocaleString(
                    "ko-KR"
                );

            updateCounterStyle(
                titleCountElement,
                currentLength,
                titleMaxLength
            );
        }

        /*
         * 본문 글자 수 표시
         */
        function updateContentCount(
            currentEditor
        ) {
            if (!currentEditor
                || !contentCountElement) {
                return;
            }

            const htmlContent =
                currentEditor.getHTML();

            const plainText =
                getPlainText(htmlContent);

            const currentLength =
                plainText.length;

            contentCountElement.textContent =
                currentLength.toLocaleString(
                    "ko-KR"
                );

            updateCounterStyle(
                contentCountElement,
                currentLength,
                contentMaxLength
            );
        }

        /*
        * 카테고리에 따라 본문 제한 변경
        *
        * 문의게시글: 1,000자
        * 공지사항: 10,000자
        */
        function updateContentLimitByCategory() {

            /*
             * 카테고리 입력 요소가 없는 화면은
             * HTML data 속성의 제한값을 그대로 사용
             */
            if (!categorySelect) {
                return;
            }

            if (
                categorySelect.value
                === "NOTICE"
            ) {
                contentMaxLength =
                    NOTICE_CONTENT_MAX_LENGTH;
            } else {
                contentMaxLength =
                    QUESTION_CONTENT_MAX_LENGTH;
            }

            /*
             * 현재 제한값을 폼 data 속성에도 반영
             */
            boardForm.dataset.contentMaxLength =
                String(contentMaxLength);

            /*
             * 화면의 / 1,000자 또는
             * / 10,000자 문구 변경
             */
            if (contentLimitTextElement) {

                contentLimitTextElement.textContent =
                    "/ "
                    + contentMaxLength
                        .toLocaleString("ko-KR")
                    + "자";
            }

            /*
             * 수정 화면처럼 기존 내용이 있는 경우
             * 변경된 제한 기준으로 다시 표시
             */
            if (editor) {
                updateContentCount(editor);
            }
        }




        /*
         * 제목 입력 이벤트
         */
        if (titleInput) {

            titleInput.addEventListener(
                "input",
                updateTitleCount
            );

            updateTitleCount();
        }

        /*
        * 문의게시글과 공지사항의
        * 본문 제한을 다르게 적용
        */
        if (categorySelect) {

            categorySelect.addEventListener(
                "change",
                updateContentLimitByCategory
            );

            /*
             * 신규 작성 화면과 기존 수정 화면에서
             * 현재 선택된 카테고리 기준으로 초기화
             */
            updateContentLimitByCategory();
        }

        /*
         * Toast UI Editor 생성
         */
        function createEditor() {

            if (editor !== null) {
                return editor;
            }

            const savedContent =
                contentInput.value || "";

            const editorHeight =
                editorElement.dataset.editorHeight
                || "560px";

            const placeholder =
                boardForm.dataset.editorPlaceholder
                || "내용을 입력해주세요.";

            editor = new toastui.Editor({

                el: editorElement,

                height: editorHeight,

                initialEditType: "wysiwyg",

                previewStyle: "vertical",

                language: "ko-KR",

                usageStatistics: false,

                placeholder: placeholder,

                hooks: {

                    /*
                     * 이미지 업로드
                     */
                    addImageBlobHook:
                        async function (
                            blob,
                            callback
                        ) {

                            const formData =
                                new FormData();

                            formData.append(
                                "image",
                                blob
                            );

                            try {

                                const response =
                                    await fetch(
                                        "/board/image/upload",
                                        {
                                            method: "POST",
                                            body: formData
                                        }
                                    );

                                const result =
                                    await response.json();

                                if (!response.ok) {
                                    throw new Error(
                                        result.message
                                        || "이미지 업로드에 실패했습니다."
                                    );
                                }

                                callback(
                                    result.imageUrl,
                                    blob.name
                                    || "게시글 이미지"
                                );

                            } catch (error) {

                                alert(
                                    error.message
                                    || "이미지 업로드 중 오류가 발생했습니다."
                                );
                            }
                        }
                }
            });

            /*
             * 수정 화면의 기존 내용 불러오기
             */
            if (savedContent.trim() !== "") {

                editor.setHTML(
                    savedContent,
                    false
                );
            }

            /*
            * 최초 내용을 정상 내용으로 저장
            */
            lastValidHtml =
                editor.getHTML();

            previousTextLength =
                getPlainText(
                    lastValidHtml
                ).length;

            /*
            * 에디터 내용이 바뀔 때마다
            * 글자 수 검사 및 표시 갱신
            */
            editor.on(
                "change",
                function () {

                    /*
                     * 제한 초과 내용을 복구하면서 발생한
                     * change 이벤트는 무시한다.
                     */
                    if (isRestoringContent) {
                        return;
                    }

                    const currentHtml =
                        editor.getHTML();

                    const currentPlainText =
                        getPlainText(
                            currentHtml
                        );

                    const currentTextLength =
                        currentPlainText.length;

                    /*
                     * 현재 제한 안에 있는 경우
                     * 정상 내용으로 저장한다.
                     */
                    if (
                        currentTextLength
                        <= contentMaxLength
                    ) {
                        lastValidHtml =
                            currentHtml;

                        previousTextLength =
                            currentTextLength;

                        updateContentCount(
                            editor
                        );

                        return;
                    }

                    /*
                     * 기존 데이터가 제한보다 긴 경우에는
                     * 사용자가 내용을 삭제하는 동작을 허용한다.
                     *
                     * 예:
                     * 기존 공지사항 5,000자를 문의글로 변경한 경우
                     * 1,000자까지 줄일 수 있어야 한다.
                     */
                    if (
                        previousTextLength
                        > contentMaxLength
                        && currentTextLength
                        < previousTextLength
                    ) {
                        lastValidHtml =
                            currentHtml;

                        previousTextLength =
                            currentTextLength;

                        updateContentCount(
                            editor
                        );

                        return;
                    }

                    /*
                     * 제한을 초과한 입력은
                     * 마지막 정상 내용으로 되돌린다.
                     */
                    isRestoringContent = true;

                    editor.setHTML(
                        lastValidHtml,
                        false
                    );

                    previousTextLength =
                        getPlainText(
                            lastValidHtml
                        ).length;

                    requestAnimationFrame(
                        function () {

                            isRestoringContent =
                                false;

                            updateContentCount(
                                editor
                            );

                            editor.focus();
                        }
                    );
                }
            );

            updateContentCount(editor);

            return editor;
        }

        /*
         * 숨겨진 details 안에서 사용하는 경우
         */
        const detailsElement =
            boardForm.closest("details");

        if (
            detailsElement
            && !detailsElement.open
        ) {
            detailsElement.addEventListener(
                "toggle",
                function () {

                    if (detailsElement.open) {
                        requestAnimationFrame(
                            createEditor
                        );
                    }
                }
            );

        } else {
            createEditor();
        }

        /*
         * 등록 또는 수정 버튼 클릭
         */
        boardForm.addEventListener(
            "submit",
            function (event) {

                /*
                 * 이미 한 번 제출된 폼이면
                 * 추가 제출을 차단한다.
                 */
                if (
                    boardForm.dataset.submitting
                    === "true"
                ) {
                    event.preventDefault();
                    return;
                }

                const currentEditor =
                    createEditor();

                if (!currentEditor) {
                    event.preventDefault();
                    return;
                }

                /*
                 * 제목 200자 검사
                 */
                if (
                    titleInput
                    && titleInput.value.length
                    > titleMaxLength
                ) {
                    event.preventDefault();

                    alert(
                        "제목은 "
                        + titleMaxLength
                        + "자 이하로 입력해주세요."
                    );

                    titleInput.focus();
                    return;
                }

                const htmlContent =
                    currentEditor.getHTML();

                const plainText =
                    getPlainText(htmlContent);

                const temporaryElement =
                    document.createElement("div");

                temporaryElement.innerHTML =
                    htmlContent;

                const hasImage =
                    temporaryElement.querySelector(
                        "img"
                    ) !== null;

                /*
                 * 빈 본문 검사
                 */
                if (!plainText && !hasImage) {

                    event.preventDefault();

                    alert(
                        "내용을 입력해주세요."
                    );

                    currentEditor.focus();
                    return;
                }

                /*
                 * 카테고리별 본문 글자 수 검사
                 */
                if (
                    plainText.length
                    > contentMaxLength
                ) {
                    event.preventDefault();

                    alert(
                        "본문은 "
                        + contentMaxLength.toLocaleString(
                            "ko-KR"
                        )
                        + "자 이하로 입력해주세요."
                    );

                    currentEditor.focus();
                    return;
                }

                /*
                 * HTML 전체 길이 검사
                 */
                if (
                    htmlContent.length
                    > contentHtmlMaxLength
                ) {
                    event.preventDefault();

                    alert(
                        "본문에 너무 많은 서식이 포함되어 있습니다."
                    );

                    currentEditor.focus();
                    return;
                }

                /*
                 * Toast UI HTML을 서버 전송값에 저장
                 */
                contentInput.value =
                    htmlContent;


                /*
                 * 모든 입력 검사를 통과한 뒤
                 * 중복 제출 방지를 시작한다.
                 */
                boardForm.dataset.submitting =
                    "true";

                const submitButton =
                    boardForm.querySelector(
                        'button[type="submit"]'
                    );

                if (submitButton) {

                    /*
                     * 등록 버튼을 비활성화하여
                     * 연속 클릭을 막는다.
                     */
                    submitButton.disabled = true;

                    /*
                     * 글쓰기와 수정 화면에 따라
                     * 버튼 문구를 다르게 표시한다.
                     */
                    if (
                        boardForm.id
                        === "board-update-form"
                    ) {
                        submitButton.textContent =
                            "수정 중...";
                    } else {
                        submitButton.textContent =
                            "등록 중...";
                    }
                }
            }
        );
    });
});