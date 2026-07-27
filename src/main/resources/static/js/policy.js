// 이용약관과 개인정보 전문 화면에서 공통으로 사용하는 "이전 화면으로" 버튼

const backButton = document.querySelector('.back-button');

// 새 탭 약관 화면 닫거나, 창을 닫을 수 없는 경우 이전 방문 화면으로 이동하는 함수
function returnToPreviousScreen() {

    window.close();

    setTimeout(function () {

        if (!window.closed) {

            history.back();
        }
    }, 100);
}

if (backButton !== null) {

    backButton.addEventListener('click', returnToPreviousScreen);
}