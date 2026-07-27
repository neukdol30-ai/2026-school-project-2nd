/*
 * map/kakao 전용 글로벌 배너 연결
 * - 메인페이지와 같은 계정 메뉴를 지도 화면에 표시
 * - 기존 mypage-modal.js가 사용하는 전역 상태/갱신 함수를 제공
 */
var state = window.mapPageState || {
    currentUser: null,
    myPage: null
};

window.mapPageState = state;

/**
 * mypage-modal.js의 공통 초기화 조건을 만족시키기 위한 지도 페이지용 호환 함수입니다.
 * 지도 화면에는 메인 대시보드 계정 위젯이 없으므로 빈 문자열만 반환합니다.
 */
function renderAuthWidget() {
    return "";
}

function updateGlobalBanner() {
    var target = document.querySelector("[data-map-global-user-menu]");

    if (!target) {
        return;
    }

    if (!state.currentUser) {
        target.innerHTML = '<a href="/member/login">로그인</a>';
        return;
    }

    target.innerHTML = [
        '<button type="button" data-mypage-open>마이페이지</button>',
        '<form action="/member/logout" method="post">',
        '    <button type="submit">로그아웃</button>',
        '</form>'
    ].join("");
}

document.addEventListener("DOMContentLoaded", updateGlobalBanner);
