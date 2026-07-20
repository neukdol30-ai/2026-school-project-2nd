// 공통 메시지 표시 함수
function setMsg(el, text, kind) {
    el.textContent = text;
    el.className = 'check-msg' + (kind ? ' check-' + kind : '');
}

// 눈 아이콘 SVG(signup.js에서 복사)
// EYE_OFF : 눈에 빗금이 쳐진 모양 = "지금 비밀번호가 가려져 있음"
const EYE_OFF = `
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none"
             stroke="currentColor" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round">
            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"></path>
            <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"></path>
            <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"></path>
            <line x1="1" y1="1" x2="23" y2="23"></line>
        </svg>`;

// EYE_ON : 빗금 없는 뜬 눈 모양 = "지금 비밀번호가 보이는 중"
const EYE_ON = `
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none"
             stroke="currentColor" stroke-width="2"
             stroke-linecap="round" stroke-linejoin="round">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
            <circle cx="12" cy="12" r="3"></circle>
        </svg>`;

// 눈 아이콘 토글 기능 연결
document.querySelectorAll('.eye-btn').forEach(function (btn) {

    btn.innerHTML = EYE_OFF; // 처음엔 가려진 상태. 빗금 눈

    btn.addEventListener('click', function () {

        const targetId = btn.getAttribute('data-target');
        const input = document.getElementById(targetId); // 그 이름표를 가진 입력칸을 찾음

        if (input.type === 'password') { // 지금 가려져 있으면
            input.type = 'text'; // 글자 보이게 바꾸고
            btn.innerHTML = EYE_ON; // 아이콘도 뜬 눈으로
            btn.setAttribute('aria-label', '비밀번호 숨기기'); // 스크린 리더 설명도 갱신
        } else { // 지금 보이는 상태면
            input.type = 'password'; // 다시 가리고
            btn.innerHTML = EYE_OFF;
            btn.setAttribute('aria-label', '비밀번호 표시');
        }
    });
});

// 화면 요소 미리 찾아두기
const pw = document.getElementById('newPassword'); // 새 비밀번호 칸
const pwc = document.getElementById('newPasswordCheck'); // 비밀번호 확인 칸
const pwCheckMsg = document.getElementById('pwCheckMsg'); // 일치 여부 메시지 자리
const pwRules = document.getElementById('pwRules'); // 조건 체크리스트 <ul>
const resetForm = document.getElementById('resetForm'); // 폼 전체
const submitBtn = document.getElementById('submitBtn'); // 제출 버튼

const ruleLen = pwRules.querySelector('[data-rule="len"]');
const ruleUpper = pwRules.querySelector('[data-rule="upper"]');
const ruleLower = pwRules.querySelector('[data-rule="lower"]');
const ruleDigit = pwRules.querySelector('[data-rule="digit"]');

// 비밀번호 조건 실시간 체크
// 한 줄(li)의 통과/미통과 상태를 그려주는 함수.
// 매개변수 li : 대상이 되는 <li> 요소 / ok : true(통과) 또는 false(미통과)
function paintRule(li, ok) {
    if (ok) {
        li.classList.add('rule-ok'); // CSS가 ○ → ✓, 회색 → 초록으로 바꿔준다
    } else {
        li.classList.remove('rule-ok'); // 그 클래스만 떼어낸다
    }
}

// 4개 조건을 한 번에 검사 + 전부 통과했는지 true/false로 돌려주는 함수.
function checkPwRules() {
    const v = pw.value; // 현재 새 비밀번호 칸에 적힌 값

    const okLen = v.length >= 8 && v.length <= 20;

    // .test(값) = 정규식 메서드. 패턴이 값 안에 있으면 true, 없으면 false
    const okUpper = /[A-Z]/.test(v); // 영문 대문자 A~Z 중 아무거나 하나
    const okLower = /[a-z]/.test(v); // 영문 소문자 a~z 중 아무거나 하나
    const okDigit = /[0-9]/.test(v); // 숫자 0~9 중 아무거나 하나

    // 1단계 : 각 줄을 결과에 맞게 칠함.
    paintRule(ruleLen, okLen);
    paintRule(ruleUpper, okUpper);
    paintRule(ruleLower, okLower);
    paintRule(ruleDigit, okDigit);

    // 2단계 : 4개가 전부 참일 때만 true를 돌려줌.
    return okLen && okUpper && okLower && okDigit;
}

// 두 칸이 같은지 실시간 비교. 같으면 true를 돌려줌.
function checkPwMatch() {
    if (pwc.value === '') { // 확인 칸 아직 비어 있으면
        setMsg(pwCheckMsg, '', null); // 메시지 안 띄움.
        return false; // 아직 "일치"는 아님 -> false
    }
    if (pw.value === pwc.value) {
        setMsg(pwCheckMsg, '비밀번호가 일치합니다.', 'ok');
        return true;
    }
    setMsg(pwCheckMsg, '비밀번호가 일치하지 않습니다.', 'fail');
    return false;
}

// 이벤트 연결
pw.addEventListener('input', function () {
    checkPwRules(); // 조건 체크리스트 갱신
    checkPwMatch(); // 일치 여부도 함께 갱신 (새 비밀번호를 고치면 이미 맞춰놨던 일치 상태가 깨지기 때문에)
});

pwc.addEventListener('input', checkPwMatch);

// 화면 처음 뜰 때 한 번 실행. -> 브라우저가 이전 입력값을 복원 못 하게
checkPwRules();

// 제출 직전 최종 검사
resetForm.addEventListener('submit', function (event) {

    const rulesOk = checkPwRules(); // 4개 조건 전부 통과했는가
    const matchOk = checkPwMatch(); // 두 칸이 같은가

    if (!rulesOk) {
        event.preventDefault();
        setMsg(pwCheckMsg, '비밀번호 조건을 모두 충족해야 합니다.', 'fail');
        pw.focus(); // 커서를 새 비밀번호 칸으로 옮겨 바로 고칠 수 있게 함.
        return;
    }

    if (!matchOk) {
        event.preventDefault();
        setMsg(pwCheckMsg, '비밀번호가 일치하지 않습니다.', 'fail');
        pwc.focus();
        return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = '변경 중입니다...';

});



























