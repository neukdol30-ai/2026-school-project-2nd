const SEND_SUCCESS_MESSAGE = '인증번호를 발송했습니다.';

// 공통 메시지 표시 함수
function setMsg(el, text, kind) {
    el.textContent = text;
    el.className = 'check-msg' + (kind ? ' check-' + kind : '');
}

// 화면 요소 미리 찾아두기
const memberIdInput = document.getElementById('memberId'); // 아이디 입력칸
const memberIdMsg = document.getElementById('memberIdMsg'); // 아이디 안내 메시지 자리

const emailInput = document.getElementById('email'); // 이메일 입력칸
const sendBtn = document.getElementById('sendBtn'); // 인증번호 발송 버튼
const sendMsg = document.getElementById('sendMsg'); // 발송 결과 메시지 자리
const changeEmailLink = document.getElementById('changeEmailLink'); // 다시 입력하기 링크

const codeGroup = document.getElementById('codeGroup'); // 인증번호 그룹 전체(숨김/표시 대상)
const codeInput = document.getElementById('code'); // 인증번호 입력칸
const verifyBtn = document.getElementById('verifyBtn'); // 인증 확인 버튼
const verifyMsg = document.getElementById('verifyMsg'); // 인증 결과 메시지 자리

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// 재발송 쿨타임 상태 관리 (find-id.js와 동일한 방식)
const COOLDOWN_SECONDS = 60; // 서버 MailService.COOLDOWN_TTL(60)과 반드시 같은 값
let cooldownIntervalId = null;

// find-id.js와 다른 이름 사용 (같은 브라우저 저장 공간 페이지별로 구분해서 사용)
const PENDING_KEY = 'findPasswordPending';

function startCooldown(seconds) {
    let remaining = seconds;
    sendBtn.disabled = true;
    sendBtn.textContent = '재전송 (' + remaining + '초)';

    cooldownIntervalId = setInterval(function () {
        remaining -= 1;
        if (remaining <= 0) {
            stopCooldown();
            return;
        }
        sendBtn.textContent = '재전송 (' + remaining + '초)';
    }, 1000);
}

function stopCooldown() {
    if (cooldownIntervalId !== null) {
        clearInterval(cooldownIntervalId);
        cooldownIntervalId = null;
    }
    sendBtn.disabled = false;
    sendBtn.textContent = '인증번호 재전송';
}

// 아이디까지 함께 저장 (find-id.js와 다르게 잠기는 칸이 2개)
function savePendingState(memberId, email, seconds) {
    const state = {
        memberId: memberId,
        email: email,
        expiresAt: Date.now() + seconds * 1000
    };
    sessionStorage.setItem(PENDING_KEY, JSON.stringify(state));
}

function clearPendingState() {
    sessionStorage.removeItem(PENDING_KEY);
}

function restorePendingState() {
    const raw = sessionStorage.getItem(PENDING_KEY);
    if (!raw) return;

    const state = JSON.parse(raw);
    const remainingMs = state.expiresAt - Date.now();

    if (remainingMs <= 0) {
        clearPendingState();
        return;
    }

    memberIdInput.value = state.memberId;
    memberIdInput.readOnly = true;
    emailInput.value = state.email;
    emailInput.readOnly = true;
    codeGroup.style.display = 'block';
    changeEmailLink.style.display = 'inline';

    startCooldown(Math.ceil(remainingMs / 1000));
}

// 아이디 칸 실시간 안내
memberIdInput.addEventListener('input', function () {
    const value = memberIdInput.value.trim();
    if (value.length === 0) {
        setMsg(memberIdMsg, '', null);
        return;
    }

    // 4~20자는 SignupDto의 @Size(min=4, max=20) 기준과 맞춘 값
    if (value.length < 4 || value.length > 20) {
        setMsg(memberIdMsg, '아이디는 4~20자로 입력하세요.', 'fail');
    } else {
        setMsg(memberIdMsg, '', null);
    }
});

// 인증번호 발송 버튼 (POST / member/find-password/send)
sendBtn.addEventListener('click', function () {
    const memberId = memberIdInput.value.trim();
    const email = emailInput.value.trim();

    if (memberId.length < 4) {
        setMsg(sendMsg, '아이디를 입력해 주세요.', 'fail');
        return;
    }

    if (!emailPattern.test(email)) {
        setMsg(sendMsg, 'example@domain.com 형식으로 입력하세요.', 'fail');
        return;
    }

    sendBtn.disabled = true; // 연타 방지 (응답이 올 때까지 버튼 잠금)
    setMsg(sendMsg, '발송 중입니다...', 'hint');

    fetch('/member/find-password/send', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: new URLSearchParams({memberId: memberId, email: email})
    })
        .then(res => res.text())
        .then(message => { // message : 위에서 꺼낸 글자가 여기로 넘어온다.
            sendBtn.disabled = false;

            if (message === SEND_SUCCESS_MESSAGE) {
                setMsg(sendMsg, message + ' (3분 이내에 입력해 주세요)', 'ok'); // 3분 = MailService의 AUTH_CODE_TTL 값과 맞춘 안내.
                codeGroup.style.display = 'block'; // 숨겨뒀던 인증번호 그룹을 화면에 나타나게.
                codeInput.focus();

                memberIdInput.readOnly = true; // 발송 성공 직후 아이디, 이메일 칸 잠금(readOnly) - 인증 진행 중 값이 바뀌는 걸 막기 위함
                emailInput.readOnly = true; // 특히 이메일은 Redis 인증키 기준이라 바뀌면 인증 무조건 실패.
                changeEmailLink.style.display = 'inline'; // 잠긴 대신 "다시 입력하기" 경로 열어둠

                startCooldown(COOLDOWN_SECONDS); // 60초 재발송 쿨타임 시작
                savePendingState(memberId, email, COOLDOWN_SECONDS);
            } else {
                // 쿨타임 초과 응답인지 먼저 판별
                const cooldownMatch = message.match(/^(\d+)초 후 다시 시도해 주세요\.$/);

                if (cooldownMatch) {
                    // 숫자는 버튼 카운트다운에만 쓰고 문구에는 넣지 않음
                    setMsg(sendMsg, '잠시 후 다시 시도해 주세요.', 'hint');
                    startCooldown(Number(cooldownMatch[1]));

                    // 새로고침 등으로 잠금이 풀린 상태에서 쿨타임에 걸린 경우, 아아디와 이메일 칸을 다시 잠금
                    memberIdInput.readOnly = true;
                    emailInput.readOnly = true;
                    changeEmailLink.style.display = 'inline';

                    savePendingState(memberId, email, Number(cooldownMatch[1]));
                } else {
                    // 아이디, 이메일 불일치 등 진짜 실패는 서버 문구를 그대로 빨간색으로
                    setMsg(sendMsg, message, 'fail');
                    sendBtn.disabled = false;
                }
            }
        })
        .catch(err => {
            sendBtn.disabled = false; // 오류가 나도 버튼은 반드시 되살려야 함.
            console.error('인증번호 발송 요청 실패', err); // F12 콘솔에 원인 기록(개발자용)
            setMsg(sendMsg, '요청에 실패했습니다. 잠시 후 다시 시도하세요.', 'fail'); // 화면 안내(사용자용)
        });
});

// 인증 확인 버튼 (POST /member/find-password/verify)
verifyBtn.addEventListener('click', function () {
    const email = emailInput.value.trim();
    const code = codeInput.value.trim();

    if (code.length !== 6) {
        setMsg(verifyMsg, '6자리 숫자를 입력하세요.', 'fail');
        return;
    }

    verifyBtn.disabled = true;
    setMsg(verifyMsg, '확인 중입니다...', 'hint');

    fetch('/member/find-password/verify', {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: new URLSearchParams({email: email, code: code})
    })
        .then(res => res.json())
        .then(verified => { // verified : true(인증 성공) 또는 false(실패)
            verifyBtn.disabled = false;

            if (verified) {
                setMsg(verifyMsg, '인증되었습니다. 비밀번호 재설정 화면으로 이동합니다...', 'ok');
                // 인증 성공 직후 인증번호 칸 잠금(readOnly)
                codeInput.readOnly = true;
                clearPendingState();
                window.location.href = '/member/reset-password';
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

// "아이디/이메일을 잘못 입력했나요? 다시 입력하기" 링크 클릭 시 동작
changeEmailLink.addEventListener('click', function (e) {
    e.preventDefault();

    memberIdInput.readOnly = false; // 아이디 칸도 함께 잠금 해제
    emailInput.readOnly = false;

    codeGroup.style.display = 'none';
    codeInput.value = '';

    setMsg(sendMsg, '', null);
    setMsg(verifyMsg, '', null);

    stopCooldown();
    clearPendingState();

    changeEmailLink.style.display = 'none';

    memberIdInput.focus(); // 첫 번째로 고칠 가능성이 높은 아이디 칸으로 포커스 이동
});

// 페이지가 열릴 때(새로고침 포함) 복원 시도
restorePendingState();