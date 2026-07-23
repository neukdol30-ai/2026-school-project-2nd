// 메모 최대 글자 수
const MEMO_MAX_LENGTH = 300;


// 회원별 메모 저장 키
function getMemoStorageKey() {
    const memberId = state.currentUser?.username || "guest";

    return "dashboardMemoList:" + memberId;
}


// 저장된 메모 목록 조회
function getMemoList() {
    try {
        const savedMemoList =
            localStorage.getItem(getMemoStorageKey());

        return savedMemoList
            ? JSON.parse(savedMemoList)
            : [];

    } catch (error) {
        console.warn("메모 조회 실패:", error);

        return [];
    }
}


// 메모 목록 저장
function saveMemoList(memoList) {
    try {
        localStorage.setItem(
            getMemoStorageKey(),
            JSON.stringify(memoList)
        );

    } catch (error) {
        console.warn("메모 저장 실패:", error);
    }
}


// 메모 위젯 화면
function renderMemo() {
    const memoList = getMemoList();

    return `
        <div class="memo-form">

            <textarea
                id="memoInput"
                class="memo-input"
                maxlength="${MEMO_MAX_LENGTH}"
                placeholder="메모를 입력하세요."
            ></textarea>

            <div class="memo-form-bottom">

                <span
                    id="memoCount"
                    class="memo-count"
                >
                    0 / ${MEMO_MAX_LENGTH}
                </span>

                <button
                    type="button"
                    id="memoSaveButton"
                    class="memo-save-button"
                >
                    저장
                </button>

            </div>

        </div>

        <div class="memo-list">

            ${
        memoList.length === 0
            ? `
                        <p class="memo-empty">
                            저장된 메모가 없습니다.
                        </p>
                    `
            : memoList.map((memo) => `
                        <div
                            class="memo-item"
                            data-memo-id="${memo.id}"
                        >
                            <p class="memo-text">
                                ${escapeHtml(memo.text)}
                            </p>

                            <button
                                type="button"
                                class="memo-delete-button"
                                data-memo-id="${memo.id}"
                                aria-label="메모 삭제"
                            >
                                ×
                            </button>
                        </div>
                    `).join("")
    }

        </div>
    `;
}


// 메모 위젯 부분만 다시 출력
function refreshMemo() {
    const memoContent =
        document.querySelector(
            '[data-widget-id="8"] .widget-content'
        );

    if (!memoContent) {
        return;
    }

    memoContent.innerHTML = renderMemo();

    bindMemoEvents();
}


// 메모 추가
function addMemo() {
    const memoInput =
        document.querySelector("#memoInput");

    if (!memoInput) {
        return;
    }

    const memoText = memoInput.value.trim();

    if (!memoText) {
        alert("메모 내용을 입력해주세요.");

        memoInput.focus();

        return;
    }

    const memoList = getMemoList();

    // 최신 메모를 맨 위에 추가
    memoList.unshift({
        id: Date.now(),
        text: memoText,
        createdAt: new Date().toISOString()
    });

    saveMemoList(memoList);

    refreshMemo();
}


// 메모 삭제
function deleteMemo(memoId) {
    const memoList = getMemoList()
        .filter((memo) =>
            String(memo.id) !== String(memoId)
        );

    saveMemoList(memoList);

    refreshMemo();
}


// 메모 이벤트 연결
function bindMemoEvents() {
    const memoInput =
        document.querySelector("#memoInput");

    const memoCount =
        document.querySelector("#memoCount");

    const memoSaveButton =
        document.querySelector("#memoSaveButton");


    // 입력할 때 전체 화면을 다시 그리지 않고 글자 수만 변경
    if (memoInput) {
        memoInput.addEventListener("input", () => {
            if (memoCount) {
                memoCount.textContent =
                    memoInput.value.length
                    + " / "
                    + MEMO_MAX_LENGTH;
            }
        });
    }


    // 메모 저장
    if (memoSaveButton) {
        memoSaveButton.addEventListener(
            "click",
            addMemo
        );
    }


    // 메모 삭제
    document
        .querySelectorAll(".memo-delete-button")
        .forEach((deleteButton) => {
            deleteButton.addEventListener(
                "click",
                () => {
                    deleteMemo(
                        deleteButton.dataset.memoId
                    );
                }
            );
        });
}