function setMsg(el, text, kind) {
    el.textContent = text;
    el.className = 'check-msg' + (kind ? ' check-' + kind : '');
}

const emailInput = document.getElementById('email');
const sendBtn = document.getElementById('sendBtn');
const sendMsg = document.getElementById('sendMsg');

const codeGroup = document.getElementById('codeGroup');
const codeInput = document.getElementById('code');
const verifyBtn = document.getElementById('verifyBtn');
const verifyMsg = document.getElementById('verifyMsg');

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

sendBtn.addEventListener('click', function () {
    const email = emailInput.value.trim();

    if (!emailPattern.test(email)) {
        setMsg(sendMsg, 'example@domain.com 형식으로 입력하세요.', 'fail');
        return;
    }

    sendBtn.disabled = true;
    setMsg(sendMsg, '발송 중입니다...', 'hint');

    fetch('/member/find-id/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ email: email })
    })
        .then(res => res.text())
        .then(message => {
            sendBtn.disabled = false;

            if (message === '인증번호를 발송했습니다.') {
                setMsg(sendMsg, message + ' (3분 이내에 입력해 주세요)', 'ok');
                codeGroup.style.display = 'block';
                codeInput.focus();
            } else {
                setMsg(sendMsg, message, 'fail');
            }
        })
        .catch(err => {
            sendBtn.disabled = false;
            console.error('인증번호 발송 요청 실패', err);
            setMsg(sendMsg, '요청에 실패했습니다. 잠시 후 다시 시도하세요.', 'fail');
        });
});

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
        .then(res => res.json())
        .then(verified => {
            verifyBtn.disabled = false;

            if (verified) {
                setMsg(verifyMsg, '인증되었습니다. 결과 화면으로 이동합니다...', 'ok');
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