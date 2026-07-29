(() => {
    "use strict";

    const backButton = document.querySelector("[data-error-back]");
    const reloadButton = document.querySelector("[data-error-reload]");

    if (backButton) {
        backButton.addEventListener("click", () => {
            const homeUrl = backButton.dataset.homeUrl || "/";

            try {
                const referrer = document.referrer;
                const hasSafePreviousPage = referrer
                    && new URL(referrer).origin === window.location.origin
                    && window.history.length > 1;

                if (hasSafePreviousPage) {
                    window.history.back();
                    return;
                }
            } catch (error) {
                console.debug("이전 페이지 주소를 확인하지 못했습니다.", error);
            }

            window.location.assign(homeUrl);
        });
    }

    if (reloadButton) {
        reloadButton.addEventListener("click", () => {
            window.location.reload();
        });
    }
})();
