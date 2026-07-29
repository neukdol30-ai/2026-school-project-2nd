document.addEventListener("DOMContentLoaded", function () {

    /*
     * 한 페이지에 존재하는 모든 게시글·답변 폼을 가져온다.
     */
    const boardForms =
        document.querySelectorAll("[data-board-form]");

    if (boardForms.length === 0) {
        return;
    }

    /*
     * Toast UI 라이브러리 로드 확인
     */
    if (typeof toastui === "undefined"
        || typeof toastui.Editor === "undefined") {

        console.error(
            "Toast UI Editor 라이브러리가 로드되지 않았습니다."
        );

        return;
    }

    boardForms.forEach(function (boardForm) {

        /*
         * 각 폼 내부의 에디터와 content 입력창을 찾는다.
         */
        const editorElement =
            boardForm.querySelector("[data-board-editor]");

        const contentInput =
            boardForm.querySelector("[data-board-content]");

        if (!editorElement || !contentInput) {
            return;
        }

        let editor = null;

        /*
         * 에디터 생성 메서드
         *
         * 답변 수정 영역은 details 안에 숨겨져 있으므로
         * 열었을 때 에디터를 생성할 수 있도록 함수로 분리한다.
         */
        function createEditor() {

            // 이미 생성된 경우 다시 생성하지 않는다.
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
                     * 에디터 이미지 업로드
                     */
                    addImageBlobHook: async function (
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
                                blob.name || "게시글 이미지"
                            );

                        } catch (error) {

                            alert(
                                error.message
                            );
                        }
                    }
                }
            });

            /*
             * 기존 내용은 HTML로 저장되어 있으므로
             * setHTML()을 사용해 에디터에 불러온다.
             *
             * <p>123</p>가 화면에는 123으로 표시된다.
             */
            if (savedContent.trim() !== "") {

                editor.setHTML(
                    savedContent,
                    false
                );
            }

            return editor;
        }

        /*
         * 답변 수정 폼이 닫힌 details 안에 있으면
         * details를 열 때 에디터를 생성한다.
         *
         * 숨겨진 상태에서 에디터를 만들면
         * 너비가 잘못 계산되는 문제를 방지한다.
         */
        const detailsElement =
            boardForm.closest("details");

        if (detailsElement
            && !detailsElement.open) {

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
         * 폼 제출 처리
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

                const htmlContent =
                    currentEditor.getHTML();

                /*
                 * HTML 태그를 제외한 실제 글자 확인
                 */
                const temporaryElement =
                    document.createElement("div");

                temporaryElement.innerHTML =
                    htmlContent;

                const plainText =
                    (
                        temporaryElement.textContent
                        || temporaryElement.innerText
                        || ""
                    )
                        .replace(/\u00A0/g, " ")
                        .trim();

                /*
                 * 이미지 포함 여부
                 */
                const hasImage =
                    temporaryElement.querySelector("img")
                    !== null;

                if (!plainText && !hasImage) {

                    event.preventDefault();

                    const emptyMessage =
                        boardForm.dataset.editorEmptyMessage
                        || "내용을 입력해주세요.";

                    alert(emptyMessage);

                    currentEditor.focus();

                    return;
                }

                /*
                 * Toast UI에서 작성된 HTML을
                 * 서버 전송용 textarea에 저장
                 */
                contentInput.value =
                    htmlContent;
            }
        );
    });
});