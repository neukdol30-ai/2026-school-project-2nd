const MEMO_MAX_LENGTH = 300;

function getMemoStorageMemberId() {
    const currentUser = state.currentUser || {};

    const memberId = String(
        currentUser.username
        || currentUser.memberId
        || currentUser.memberNo
        || currentUser.no
        || currentUser.id
        || "guest"
    ).trim();

    return memberId || "guest";
}

function getMemoStorageKey() {
    return "dashboardMemoList:"
        + getMemoStorageMemberId();
}

function getMemoList() {
    try {
        const savedMemoList =
            localStorage.getItem(
                getMemoStorageKey()
            );

        if (!savedMemoList) {
            return [];
        }

        const parsedMemoList =
            JSON.parse(savedMemoList);

        return Array.isArray(parsedMemoList)
            ? parsedMemoList
            : [];

    } catch (error) {
        console.warn(
            "메모 조회 실패:",
            error
        );

        return [];
    }
}

function saveMemoList(memoList) {
    try {
        localStorage.setItem(
            getMemoStorageKey(),
            JSON.stringify(memoList)
        );

        return true;

    } catch (error) {
        console.warn(
            "메모 저장 실패:",
            error
        );

        alert("메모를 저장하지 못했습니다.");

        return false;
    }
}

function escapeMemoHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function formatMemoDateTime(dateText) {
    const date = new Date(dateText);

    if (Number.isNaN(date.getTime())) {
        return "";
    }

    return date.toLocaleString(
        "ko-KR",
        {
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        }
    );
}

function renderMemoList() {
    const memoList = getMemoList();

    if (memoList.length === 0) {
        return `
            <p class="memo-empty">
                저장된 메모가 없습니다.
            </p>
        `;
    }

    return memoList.map((memo) => {
        const memoText =
            escapeMemoHtml(
                memo.text
                ?? memo.content
                ?? ""
            );

        const createdAt =
            formatMemoDateTime(
                memo.createdAt
            );

        return `
            <article
                class="memo-item"
                data-memo-id="${memo.id}"
            >
                <p class="memo-item-content">${memoText}</p>

                <div class="memo-item-bottom">
                    <span class="memo-created-at">${createdAt}</span>

                    <button
                        type="button"
                        class="memo-delete-button"
                        data-action="delete-memo"
                        data-memo-id="${memo.id}"
                    >
                        삭제
                    </button>
                </div>
            </article>
        `;
    }).join("");
}

function renderMemo() {
    const draftText =
        String(state.memoText || "")
            .slice(0, MEMO_MAX_LENGTH);

    return `
        <div class="memo-write-box">
            <textarea
                id="memoInput"
                class="memo-input"
                maxlength="${MEMO_MAX_LENGTH}"
                placeholder="메모를 입력하세요."
            >${escapeMemoHtml(draftText)}</textarea>

            <div class="memo-write-bottom">
                <span
                    id="memoCount"
                    class="memo-count"
                >
                    ${draftText.length}
                    / ${MEMO_MAX_LENGTH}
                </span>

                <button
                    type="button"
                    class="memo-save-button"
                    data-action="save-memo"
                >
                    저장
                </button>
            </div>
        </div>

        <div
            id="memoList"
            class="memo-list"
        >
            ${renderMemoList()}
        </div>
    `;
}

function updateMemoCount(value) {
    const memoCount =
        document.querySelector("#memoCount");

    if (!memoCount) {
        return;
    }

    const text = String(value || "")
        .slice(0, MEMO_MAX_LENGTH);

    memoCount.textContent =
        text.length
        + " / "
        + MEMO_MAX_LENGTH;
}

function refreshMemoWidget() {
    const memoContent =
        document.querySelector(
            '[data-widget-content="8"]'
        )
        || document.querySelector(
            '[data-widget-id="8"] .widget-content'
        );

    if (!memoContent) {
        return;
    }

    memoContent.innerHTML =
        renderMemo();

    if (
        typeof scheduleDashboardLayout
        === "function"
    ) {
        scheduleDashboardLayout({
            animate: false
        });
    }
}

function addMemo() {
    const memoInput =
        document.querySelector("#memoInput");

    if (!memoInput) {
        return;
    }

    const memoText =
        memoInput.value.trim();

    if (!memoText) {
        alert("메모 내용을 입력해주세요.");

        memoInput.focus();

        return;
    }

    const memoList =
        getMemoList();

    memoList.unshift({
        id: Date.now(),
        text: memoText,
        createdAt:
            new Date().toISOString()
    });

    if (!saveMemoList(memoList)) {
        return;
    }

    state.memoText = "";

    refreshMemoWidget();
}

function deleteMemo(memoId) {
    if (!memoId) {
        return;
    }

    const deleteConfirmed =
        confirm(
            "이 메모를 삭제하시겠습니까?"
        );

    if (!deleteConfirmed) {
        return;
    }

    const memoList =
        getMemoList()
            .filter((memo) => {
                return String(memo.id)
                    !== String(memoId);
            });

    if (!saveMemoList(memoList)) {
        return;
    }

    refreshMemoWidget();
}
