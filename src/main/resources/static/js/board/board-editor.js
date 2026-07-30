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

        const contentMaxLength =
            Number(
                boardForm.dataset.contentMaxLength
            ) || 10000;

        const contentHtmlMaxLength =
            Number(
                boardForm.dataset.contentHtmlMaxLength
            ) || 30000;

        let editor = null;

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
             * 에디터 내용이 바뀔 때마다
             * 본문 글자 수 갱신
             */
            editor.on(
                "change",
                function () {
                    updateContentCount(
                        editor
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
                 * 화면에 보이는 본문 10,000자 검사
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
            }
        );
    });
});