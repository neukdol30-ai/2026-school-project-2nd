document.addEventListener("DOMContentLoaded", function () {
    const editorElement =
        document.querySelector("[data-board-editor]");

    const contentInput =
        document.querySelector("[data-board-content]");

    const boardForm =
        document.querySelector("[data-board-form]");

    /*
     * 게시글 작성·수정 페이지가 아니라면
     * 에디터를 생성하지 않는다.
     */
    if (!editorElement || !contentInput || !boardForm) {
        return;
    }

    const savedContent = contentInput.value || "";

    const editor = new toastui.Editor({
        el: editorElement,
        height: "560px",
        initialEditType: "wysiwyg",
        previewStyle: "vertical",
        language: "ko-KR",
        initialValue: savedContent,

        hooks: {
            addImageBlobHook: async function (
                blob,
                callback
            ) {
                const formData = new FormData();

                formData.append("image", blob);

                try {
                    const response = await fetch(
                        "/board/image/upload",
                        {
                            method: "POST",
                            body: formData
                        }
                    );

                    const result = await response.json();

                    if (!response.ok) {
                        throw new Error(
                            result.message ||
                            "이미지 업로드에 실패했습니다."
                        );
                    }

                    callback(
                        result.imageUrl,
                        blob.name || "게시글 이미지"
                    );

                } catch (error) {
                    alert(error.message);
                }
            }
        }
    });

    boardForm.addEventListener(
        "submit",
        function (event) {
            const htmlContent = editor.getHTML();

            const plainText = htmlContent
                .replace(/<[^>]*>/g, "")
                .replace(/&nbsp;/gi, "")
                .trim();

            const hasImage =
                /<img\b[^>]*>/i.test(htmlContent);

            if (!plainText && !hasImage) {
                event.preventDefault();
                alert("게시글 내용을 입력해주세요.");
                editor.focus();
                return;
            }

            contentInput.value = htmlContent;
        }
    );
});