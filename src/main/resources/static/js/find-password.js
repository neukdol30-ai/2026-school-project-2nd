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

const codeGroup = document.getElementById('codeGroup'); // 인증번호 그룹 전체(숨김/표시 대상)
const codeInput = document.getElementById('code'); // 인증번호 입력칸
const verifyBtn = document.getElementById('verifyBtn'); // 인증 확인 버튼
const verifyMsg = document.getElementById('verifyMsg'); // 인증 결과 메시지 자리

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

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
            } else {
                // 성공 문구가 아니면 서버가 보낸 실패 사유를 그대로 보여준다.
                setMsg(sendMsg, message, 'fail');
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