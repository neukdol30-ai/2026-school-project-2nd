function renderMemo() {

    return `
            <textarea
                id="memoInput"
                placeholder="메모를 입력하세요"
            >${state.memoText}</textarea>

            <div class="memo-count">
                글자 수: ${state.memoText.length}
            </div>
        `;

}

