// 공통
// el : 메시지 넣을 span 요소, text : 보여줄 문구, kind : 'ok'(초록) / 'fail'(빨강) / 'hint'(회색) 중 하나
function setMsg(el, text, kind) {
    el.textContent = text; // 문구를 화면에 표시
    // className을 통째로 갈아끼운다.
    el.className = 'check-msg' + (kind ? ' check-' + kind : '');
}

//
// 1) 눈 아이콘 SVG
//
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


document.querySelectorAll('.eye-btn').forEach(function (btn) {

    btn.innerHTML = EYE_OFF;

    btn.addEventListener('click', function () {

        const targetId = btn.getAttribute('data-target');
        const input = document.getElementById(targetId);

        if (input.type === 'password') {
            input.type = 'text';
            btn.innerHTML = EYE_ON;
            btn.setAttribute('aria-label', '비밀번호 숨기기');
        } else {
            input.type = 'password';
            btn.innerHTML = EYE_OFF;
            btn.setAttribute('aria-label', '비밀번호 표시');
        }
    });
});

//
// 2) 비밀번호 조건 실시간 체크리스트
//
const pw = document.getElementById('password');
const pwc = document.getElementById('passwordCheck');
const pwCheck = document.getElementById('pwCheck'); // 두 칸 일치 여부 메시지 자리
const pwRules = document.getElementById('pwRules'); // <ul> 체크리스트 전체

// 체크리스트 각 줄(li)을 data-rule 값으로 찾아 미리 변수에 담아둔다.
// querySelector : 조건에 맞는 첫 번째 요소 하나만 찾음
const ruleLen = pwRules.querySelector('[data-rule="len"]');
const ruleUpper = pwRules.querySelector('[data-rule="upper"]');
const ruleLower = pwRules.querySelector('[data-rule="lower"]');
const ruleDigit = pwRules.querySelector('[data-rule="digit"]');

// 한 줄(li)의 통과/미통과 상태를 그려주는 함수
// ok가 true면 rule-ok 클래스를 붙여 초록 체크, false면 떼어내 회색 o
function paintRule(li, ok) {
    if (ok) {
        li.classList.add('rule-ok'); // classList.add : 클래스 추가(기존 클래스는 그대로 유지)
    } else {
        li.classList.remove('rule-ok'); // classList.remove : 그 클래스만 떼어냄
    }
}

function checkPwRules() {
    const v = pw.value; // 현재 비밀번호 칸에 적힌 값

    // 정규식(regex) : "이런 모양의 글자가 들어 있냐"를 검사하는 패턴 문법
    // /[A-Z]/ -> 영문 대문자 A~Z 중 아무거나 하나
    // /[a-z]/ -> 영문 소문자 a~z 중 아무거나 하나
    // /[0-9]/ -> 숫자 0~9 중 아무거나 하나
    // .test(값) -> 그 패턴이 값 안에 있으면 true, 없으면 false
    const okLen = v.length >= 8 && v.length <= 20; // 8자 이상 그리고 20자 이하
    const okUpper = /[A-Z]/.test(v);
    const okLower = /[a-z]/.test(v);
    const okDigit = /[0-9]/.test(v);

    paintRule(ruleLen, okLen); // 각 줄을 결과에 맞게 칠함.
    paintRule(ruleUpper, okUpper);
    paintRule(ruleLower, okLower);
    paintRule(ruleDigit, okDigit);
}

// 두 비밀번호 칸이 같은지 실시간 비교
function checkPwMatch() {
    if (pwc.value === '') { // 확인 칸이 비어 있으면 메시지 안 띄움
        setMsg(pwCheck, '', null);
        return;
    }
    if (pw.value === pwc.value) {
        setMsg(pwCheck, '비밀번호가 일치합니다.', 'ok');
    } else {
        setMsg(pwCheck, '비밀번호가 일치하지 않습니다.', 'fail');
    }
}

// input 이벤트 : 글자를 치거나 지울 때마다 매번 실행 (= 실시간)
pw.addEventListener('input', function () {
    checkPwRules(); // 조건 체크리스트 갱신
    checkPwMatch(); // 일치 여부도 같이 갱신 (비번을 고치면 일치 상태가 깨질 수 있으니까)
});
pwc.addEventListener('input', checkPwMatch);

checkPwRules(); // 화면이 처음 뜰 때 한 번 실행 (서버 검증 실패로 값이 채워진 채 돌아온 경우 대비)

//
// 3) 아이디 실시간 중복 체크
//
const memberIdInput = document.getElementById('memberId'); // 아이디 입력칸
const memberIdCheck = document.getElementById('memberIdCheck'); // 결과 메시지 자리

// input : 값을 고치는 즉시 이전 결과 메시지를 지움.(이게 없으면 "사용 가능합니다" 초록 메시지가 남은 채로 아이디만 바뀌는 착시 생김)
memberIdInput.addEventListener('input', function () {
    setMsg(memberIdCheck, '', null);
});

// blur : 입력칸에서 포커스가 "빠져나가는 순간"(다른 칸으로 넘어갈 때) 실행
memberIdInput.addEventListener('blur', function () {
    const value = memberIdInput.value.trim(); // 앞, 뒤 공백 제거한 입력값
    if (value.length < 4) { // 4자 미만이면 서버에 묻지 않음
        setMsg(memberIdCheck, value.length === 0 ? '' : '아이디는 4~20자로 입력하세요.', 'fail');
        return;
    }

    fetch('/member/exists?memberId=' + encodeURIComponent(value)) // 서버에 GET 요청
        .then(res => res.json()) // 서버가 보낸 true/false를 값으로 변환
        .then(isDuplicate => { // isDuplicate : true면 중복, false면 사용 가능
            if (isDuplicate) {
                setMsg(memberIdCheck, '이미 사용 중인 아이디입니다.', 'fail');
            } else {
                setMsg(memberIdCheck, '사용 가능한 아이디입니다.', 'ok');
            }
        })
        // catch : 위 과정 중 어디서든 에러가 나면 여기로(이게 없으면 에러가 콘솔에만 찍히고 화면은 아무 반응 없음)
        .catch(err => {
            console.error('아이디 중복 확인 실패', err);
            setMsg(memberIdCheck, '중복 확인에 실패했습니다. 잠시 후 다시 시도하세요.', 'fail');
        });
});

//
// 4) 닉네임 실시간 중복 체크
//
const nicknameInput = document.getElementById('nickname');
const nicknameCheck = document.getElementById('nicknameCheck');

nicknameInput.addEventListener('input', function () {
    setMsg(nicknameCheck, '', null); // 값을 고치면 이전 결과 메시지 제거
});

nicknameInput.addEventListener('blur', function () {
    const value = nicknameInput.value.trim();

    if (value.length === 0) {  // 아예 비었으면 아무 메시지도 안 띄움
        setMsg(nicknameCheck, '', null);
        return;
    }
    if (value.length < 2 || value.length > 10) { // 길이 조건 미달이면 서버에 물어보지 않음
        setMsg(nicknameCheck, '닉네임은 2~10자로 입력하세요.', 'fail');
        return;
    }

    // encodeURIComponent : 한글이나 공백을 URL에 안전한 형태로 바꿈
    // (F12 화면에서 nickname=%ED%85%8C... 로 보였던 게 이 변환 결과)
    fetch('/member/exists-nickname?nickname=' + encodeURIComponent(value))
        .then(res => res.json())
        .then(isDuplicate => {
            if (isDuplicate) {
                setMsg(nicknameCheck, '이미 사용 중인 닉네임입니다.', 'fail');
            } else {
                setMsg(nicknameCheck, '사용 가능한 닉네임입니다.', 'ok');
            }
        })
        .catch(err => {
            console.error('닉네임 중복 확인 실패', err);
            setMsg(nicknameCheck, '중복 확인에 실패했습니다. 잠시 후 다시 시도하세요.', 'fail');
        });
});

//
// 5) 이름 실시간 길이 안내 (2~20자)
//
const nameInput = document.getElementById('name');
const nameHint = document.getElementById('nameHint');

nameInput.addEventListener('input', function () {
    const v = nameInput.value.trim();
    if (v.length === 0) {
        setMsg(nameHint, '', null); // 비었으면 메시지 없음
    } else if (v.length < 2 || v.length > 20) {
        setMsg(nameHint, '이름은 2~20자로 입력하세요.', 'fail');
    } else {
        setMsg(nameHint, '사용 가능합니다.', 'ok');
    }
});

//
// 6) 이메일 실시간 형식 안내
//
const emailInput = document.getElementById('email');
const emailHint = document.getElementById('emailHint');

// 정규식 해설 : ^[^\s@]+@[^\s@]+\.[^\s@]+$
//  ^        : 문자열의 시작
//  [^\s@]+  : 공백(\s)도 아니고 @도 아닌 글자가 1개 이상
//  @        : @ 기호 하나
//  [^\s@]+  : 다시 공백·@ 아닌 글자 1개 이상 (도메인 앞부분)
//  \.       : 점(.) 하나  ※ 그냥 . 은 "아무 글자"라는 뜻이라 \ 를 붙여 진짜 점으로 만듦
//  [^\s@]+  : 점 뒤 글자 1개 이상 (com, net 등)
//  $        : 문자열의 끝
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

emailInput.addEventListener('input', function () {
    const v = emailInput.value.trim();
    if (v.length === 0) {
        setMsg(emailHint, '', null);
    } else if (emailPattern.test(v)) {
        setMsg(emailHint, '올바른 이메일 형식입니다.', 'ok');
    } else {
        setMsg(emailHint, 'example@domain.com 형식으로 입력하세요.', 'fail');
    }
});

//
// 7) 전화번호 : 자동 하이픈 + 형식 안내
//
const phoneInput = document.getElementById('phone');
const phoneHint = document.getElementById('phoneHint');

phoneInput.addEventListener('input', function () {
    // replace(/[^0-9]/g, '') : 숫자가 아닌 글자를 전부 지운다.
    //   /g 는 global(전역) 플래그. 붙이지 않으면 "처음 하나만" 바꾼다.
    let digits = phoneInput.value.replace(/[^0-9]/g, '');

    if (digits.length > 11) { // 휴대폰 번호는 숫자 11자리(01012345678)까지만
        digits = digits.substring(0, 11); // substring(0,11) : 0번째부터 11개만 잘라냄
    }

    let formatted; // 하이픈을 넣어 완성한 문자열
    if (digits.length < 4) {
        formatted = digits; // 010
    } else if (digits.length < 8) {
        formatted = digits.substring(0, 3) + '-' + digits.substring(3); // 010-1234
    } else {
        formatted = digits.substring(0, 3) + '-'
            + digits.substring(3, 7) + '-'
            + digits.substring(7); // 010-1234-5678
    }

    phoneInput.value = formatted; // 사용자가 친 값을 하이픈 붙은 값으로 즉시 교체

    if (digits.length === 0) {
        setMsg(phoneHint, '', null);
    } else if (digits.length === 11) {
        setMsg(phoneHint, '올바른 형식입니다.', 'ok');
    } else {
        setMsg(phoneHint, '010-0000-0000 형식으로 입력하세요.', 'fail');
    }
});

//
// 8) 생년월일 : 숫자 8자리 -> 자동 하이픈 + 실제 존재하는 날짜인지(윤년 포함) + 미래 날짜 / 만 14세 미만 검증
//
function getDaysInMonth(year, month) {
    return new Date(year, month, 0).getDate();
}

const birthDateInput = document.getElementById('birthDate');
const birthDateHint = document.getElementById('birthDateHint');

birthDateInput.addEventListener('input', function () {
    // 1 : 숫자만 남기기 (전화번호 칸과 같은 방식)
    let digits = birthDateInput.value.replace(/[^0-9]/g, '');

    if (digits.length > 8) { // YYYYMMDD = 8자리까지만
        digits = digits.substring(0, 8);
    }

    // 2 : 하이픈 넣어 화면에 보여줄 문자열 조립 (서버 저장 형식과 동일 : yyyy-MM-dd)
    let formatted;
    if (digits.length < 5) {
        formatted = digits; // 2000
    } else if (digits.length < 7) {
        formatted = digits.substring(0, 4) + '-' + digits.substring(4); // 2000-03
    } else {
        formatted = digits.substring(0, 4) + '-'
            + digits.substring(4, 6) + '-'
            + digits.substring(6); // 2000-03-06
    }
    birthDateInput.value = formatted;

    // 3 : 8자리가 다 채워지기 전에는 검증하지 않고 대기 (입력 중에 미리 오류 뜨면 방해)
    if (digits.length < 8) {
        setMsg(birthDateHint, '', null);
        return;
    }

    // 4 : 연/월/일을 각각 숫자로 분리
    const year = parseInt(digits.substring(0, 4), 10);
    const month = parseInt(digits.substring(4, 6), 10);
    const day = parseInt(digits.substring(6, 8), 10);

    // 5 : 월 자체가 1~12 범위인지 먼저 확인
    if (month < 1 || month > 12) {
        setMsg(birthDateHint, '존재하지 않는 월입니다.', 'fail');
        return;
    }

    // 6 : 핵심 - 그 연/월에 실제로 존재하는 일(day)인지 확인 (2월 윤년 포함)
    const maxDay = getDaysInMonth(year, month);
    if (day < 1 || day > maxDay) {
        setMsg(birthDateHint, '존재하지 않는 날짜입니다. (이 달은 ' + maxDay + '일까지 있습니다)', 'fail');
        return;
    }

    // 7 : 여기까지 통과했으면 달력에 실제로 있는 날짜 확정. 이제 미래 날짜 / 만 14세 검증.
    const selected = new Date(year, month - 1, day); // JS Date의 month는 0~11이라 -1 보정
    const today = new Date();
    today.setHours(0,0,0,0);

    if (selected > today) { // 미래 날짜 차단
        setMsg(birthDateHint, '생년월일은 오늘 이전 날짜여야 합니다.', 'fail');
        return;
    }

    const fourteenYearsAgo = new Date(today);
    fourteenYearsAgo.setFullYear(today.getFullYear() -14);

    if (selected > fourteenYearsAgo) { // 만 14세 미만 차단
        setMsg(birthDateHint, '만 14세 미만은 가입할 수 없습니다.', 'fail');
    } else {
        setMsg(birthDateHint, '', null);
    }
});

//
// 9) 우편번호 검색 (카카오 / 다음 우편번호 서비스)
//
const postcodeSearchBtn = document.getElementById('postcodeSearchBtn');

function execDaumPostcode() {
    new daum.Postcode({
        oncomplete: function (data) {
            let addr;

            if (data.userSelectedType === 'R') {
                addr = data.roadAddress; // 도로명 주소
            } else {
                addr = data.jibunAddress; // 지번 주소
            }

            // 팝업에서 받은 값을 화면의 readonly 입력칸에 채우기
            document.getElementById('postcode').value = data.zonecode; // 우편번호 5자리
            document.getElementById('address').value = addr;

            document.getElementById('detailAddress').focus(); // 상세주소 칸으로 커서 자동 이동 (다음 입력 유도)
        }
    }).open(); // open() : 만들어둔 검색 창 객체를 실제로 화면에 팝업으로 띄우는 메서드
}

// 버튼 클릭 시 위에서 만든 함수를 실행
postcodeSearchBtn.addEventListener('click', execDaumPostcode);