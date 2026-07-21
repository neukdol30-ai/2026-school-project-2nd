// setMsg : 안내 문구(el)에 텍스트(text)와 상태(kind : ok/fail/hint)를 표시하는 공통 함수
function setMsg(el, text, kind) {
    el.textContent = text; // el(span)의 화면 표시 글자를 text로 변경
    el.className = 'check-msg' + (kind ? ' check-' + kind : '');
}

const emailInput = document.getElementById('email'); // 이메일 입력칸(find-id.html의 <input id="email">)
const sendBtn = document.getElementById('sendBtn'); // 인증번호 발송 버튼
const sendMsg = document.getElementById('sendMsg'); // 발송 결과 메시지가 표시될 자리

const codeGroup = document.getElementById('codeGroup'); // 인증번호 입력 영역 전체(처음엔 숨김)
const codeInput = document.getElementById('code'); // 인증번호 입력칸
const verifyBtn = document.getElementById('verifyBtn'); // 인증 확인 버튼
const verifyMsg = document.getElementById('verifyMsg'); // 인증 결과 메시지가 표시될 자리
const changeEmailLink = document.getElementById('changeEmailLink'); // "다시 입력하기" 링크

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/; // 이메일 형식 검사용 정규식.

// 재발송 쿨타임 상태 관리
const COOLDOWN_SECONDS = 60; // 서버 MailService.COOLDOWN_TTL(60초)과 반드시 같은 값이어야 함.
let cooldownIntervalId = null; // setInterval의 ID를 저장해뒀다가 나중에 clearInterval로 멈추는데 사용

// 새로고침 복원 sessionStorage
const PENDING_KEY = 'findIdPending';

// startCooldown : 발송 버튼을 seconds초 동안 잠그고, 남은 초를 버튼 글자에 표시하는 함수
function startCooldown(seconds) {
    let remaining = seconds; //
    sendBtn.disabled = true; // 버튼을 눌러도 반응 없게 잠금
    sendBtn.textContent = '재전송 (' + remaining + '초)';

    // setInterval(함수, 1000) : 1000밀리초(1초)마다 함수를 반복 실행하도록 예약하는 브라우저 API
    cooldownIntervalId = setInterval(function () {
        remaining -= 1;
        if (remaining <= 0) { // 카운트 다 됐으면
            stopCooldown(); // 잠금 풀고 반복 멈춤
            return; // 아래 코드(버튼 글자 갱신) 실행 안 하고 함수 종료
        }
        sendBtn.textContent = '재전송 (' + remaining + '초)'; // 매초 버튼 글자 갱신
    }, 1000);
}

// stopCooldown : 진행 중인 카운트다운 멈추고 버튼 원래대로 되돌리는 함수
function stopCooldown() {
    if (cooldownIntervalId !== null) { // 돌고 있는 타이머가 있을 때만
        clearInterval(cooldownIntervalId); // setInterval 예약을 취소
        cooldownIntervalId = null; // 다 썼으니 표시 값 비움
    }
    sendBtn.disabled = false;
    sendBtn.textContent = '인증번호 재전송'; // 이후엔 "재전송"이라는 표현으로 통일(이미 한 번 보냈으니)
}

// 이메일 + "쿨타임이 끝나는 정확한 시간(ms)"을 저장한다
function savePendingState(email, seconds) {
    const state = {
        email: email,
        expiresAt: Date.now() + seconds * 1000 // 지금 시각(ms) + 남은 초 -> 쿨타임 종료 시각
    };
    sessionStorage.setItem(PENDING_KEY, JSON.stringify(state)); // 객체는 그대로 못 넣으므로 문자열 변환
}

// 더 이상 복원 필요 없을 때(다시 입력하기, 인증 성공) tkrwp
function clearPendingState() {
    sessionStorage.removeItem(PENDING_KEY);
}

// 페이지가 열릴 때(새로고침 포함) 한 번 실행
function restorePendingState() {
    const raw = sessionStorage.getItem(PENDING_KEY);
    if (!raw) return; // 저장된 게 없으면 할 일 없음

    const state = JSON.parse(raw);
    const remainingMs = state.expiresAt - Date.now();

    if (remainingMs <= 0) { // 쿨타임이 이미 끝난 뒤 새로고침한 경우
        clearPendingState(); // 낡은 값이므로 삭제
        return; // 화면은 기본 상태 그대로
    }

    // 여기 도달 = 쿨타임 아직 안 끝남 = 서버의 정답 코드도 3분 TTL 안에 살아있을 가능성 높음
    emailInput.value = state.email;
    emailInput.readOnly = true;
    codeGroup.style.display = 'block';
    changeEmailLink.style.display = 'inline';

    startCooldown(Math.ceil(remainingMs / 1000)); // 남은 ms를 초 단위로 올림 처리해 카운트다운 재개
}

// 1단계 : 인증번호 발송 버튼
sendBtn.addEventListener('click', function () {
    const email = emailInput.value.trim(); // 입력칸의 현재 값을 꺼내고, 앞 뒤 공백 제거

    if (!emailPattern.test(email)) {
        setMsg(sendMsg, 'example@domain.com 형식으로 입력하세요.', 'fail');
        return;
    }

    sendBtn.disabled = true; // 응답 오기 전까지 버튼 연타 방지
    setMsg(sendMsg, '발송 중입니다...', 'hint');

    fetch('/member/find-id/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ email: email })
    })
        .then(res => res.text()) // 서버가 String을 반환 -> .text()로 받아야 함.
        .then(message => { // message : 서버가 보낸 응답 문자열
            if (message === '인증번호를 발송했습니다.') {
                setMsg(sendMsg, message + ' (3분 이내에 입력해 주세요)', 'ok');
                codeGroup.style.display = 'block';
                codeInput.focus();

                emailInput.readOnly = true; // 발송 성공 직후 이메일 칸 잠금.
                changeEmailLink.style.display = 'inline'; // 잠긴 대신 "다시 입력하기" 경로 열어두기

                startCooldown(COOLDOWN_SECONDS); // 60초 재발송 쿨타임 시작
                savePendingState(email, COOLDOWN_SECONDS);
            } else {
                // 서버가 쿨타임 초과로 거절한 경우("N초 후 다시 시도해 주세요.")인지 정규식으로 판별
                const cooldownMatch = message.match(/^(\d+)초 후 다시 시도해 주세요\.$/);

                if (cooldownMatch) {
                    // 서버가 준 숫자는 화면 문구에 쓰지 않고, 버튼 카운트다운에만 넘김.
                    setMsg(sendMsg, '잠시 후 다시 시도해 주세요.', 'hint');
                    startCooldown(Number(cooldownMatch[1])); // 문자열 "37"을 숫자 37로 변환해 버튼 카운트다운 재구성

                    emailInput.readOnly = true;
                    changeEmailLink.style.display = 'inline'; // 잠긴 대신 "다시 입력하기" 경로를 함께 열어줌

                    savePendingState(email, Number(cooldownMatch[1]));
                } else {
                    // 쿨타임이 아닌 진짜 실패(가입 안 된 이메일 등)는 서버 문구를 그대로 빨간색으로 보여줌
                    setMsg(sendMsg, message, 'fail');
                    sendBtn.disabled = false; // 바로 다시 시도할 수 있게 버튼 해제
                }
            }
        })
        .catch(err => {
            sendBtn.disabled = false;
            console.error('인증번호 발송 요청 실패', err); // F12 콘솔용(개발자)
            setMsg(sendMsg, '요청에 실패했습니다. 잠시 후 다시 시도하세요.', 'fail');
        });
});

// 2단계 : 인증번호 확인 버튼 클릭 시 동작
verifyBtn.addEventListener('click', function () {
    const email = emailInput.value.trim();
    const code = codeInput.value.trim();

    if (code.length !== 6) {
        setMsg(verifyMsg, '6자리 숫자를 입력하세요.', 'fail');
        return;
    }

    verifyBtn.disabled = true;
    setMsg(verifyMsg, '확인 중입니다...', 'hint');

    fetch('/member/find-id/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ email: email, code: code })
    })
        .then(res => res.json()) // 서버 boolean 반환. json()으로 받아야 함.
        .then(verified => { // verified : true(성공) 또는 false(실패)
            verifyBtn.disabled = false;

            if (verified) {
                setMsg(verifyMsg, '인증되었습니다. 결과 화면으로 이동합니다...', 'ok');
                codeInput.readOnly = true; // 인증 성공 직후 인증번호 칸도 잠금
                clearPendingState(); // 인증 끝났으니 복원 상태도 삭제
                window.location.href = '/member/find-id/result';
            } else {
                setMsg(verifyMsg, '인증번호가 올바르지 않거나 만료되었습니다.', 'fail');
            }
        })
        .catch(err => {
            verifyBtn.disabled = false;
            console.error('인증번호 확인 요청 실패', err);
            setMsg(verifyMsg, '요청에 실패했습니다. 잠시 후 다시 시도하세요.', 'fail');
        });
});

// "이메일을 잘못 입력하셨나요? 다시 입력하기" 링크 클릭 시 동작
changeEmailLink.addEventListener('click', function (e) {
    e.preventDefault(); // <a href="#"> 클릭하면 원래 페이지 맨 위로 이동하는데, 그 기본 동작을 막음

    emailInput.readOnly = false; // 잠갔던 이메일 칸을 다시 수정 가능하게

    codeGroup.style.display = 'none'; // 인증번호 입력 영역을 다시 숨김
    codeInput.value = ''; // 이전에 입력했던 인증번호 값 비움(다른 이메일로 새로 받을 것이니까 의미 없는 값)

    setMsg(sendMsg, '', null); // 발송 메시지 초기화
    setMsg(verifyMsg, '', null); // 인증 메시지 초기화

    stopCooldown(); // 진행 중이던 쿨타임 타이머 멈춤 -> 버튼 원상 복구
    clearPendingState(); // 다시 입력하기로 리셋했으니 저장값도 삭제

    changeEmailLink.style.display = 'none'; // 이 링크 자체도 다시 숨김

    emailInput.focus(); // 사용자가 바로 새 이메일 입력할 수 있게 커서 이동
});

// 페이지가 열릴 때(새로고침 포함) 딱 한 번 실행 -> 저장된 상태가 있으면 복원
restorePendingState();