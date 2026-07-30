/*
 * 게시판 공통 헤더·푸터 출력
 */
document.addEventListener(
    "DOMContentLoaded",
    function () {

        // 공통 헤더 출력
        if (typeof updateGlobalBanner === "function") {
            updateGlobalBanner();
        }

        // 공통 푸터 출력
        const globalFooter =
            document.querySelector(
                "[data-global-footer]"
            );

        if (
            globalFooter
            && typeof renderGlobalFooter === "function"
        ) {
            globalFooter.innerHTML =
                renderGlobalFooter();
        }
    }
);


/*
 * 게시판 공통 헤더 이벤트
 */
document.addEventListener(
    "click",
    function (event) {

        /*
         * 서비스 메뉴 열기·닫기
         */
        const menuButton =
            event.target.closest(
                '[data-action="toggle-service-menu"]'
            );

        if (menuButton) {
            const serviceMenu =
                menuButton.closest(
                    "[data-global-service-menu]"
                );

            const servicePanel =
                document.querySelector(
                    "[data-global-service-panel]"
                );

            if (!servicePanel) {
                return;
            }

            const willOpen =
                servicePanel.hidden;

            servicePanel.hidden =
                !willOpen;

            menuButton.setAttribute(
                "aria-expanded",
                String(willOpen)
            );

            if (serviceMenu) {
                serviceMenu.classList.toggle(
                    "is-open",
                    willOpen
                );
            }

            return;
        }


        /*
         * 로그아웃 버튼
         */
        const logoutButton =
            event.target.closest(
                '[data-action="logout"]'
            );

        if (logoutButton) {
            window.location.href =
                "/member/logout";

            return;
        }


        /*
         * 바로가기 링크를 누르면 메뉴 닫기
         */
        const serviceLink =
            event.target.closest(
                "[data-service-menu-link]"
            );

        if (serviceLink) {
            closeGlobalServiceMenu();
            return;
        }


        /*
         * 메뉴 바깥을 누르면 닫기
         */
        const serviceMenu =
            document.querySelector(
                "[data-global-service-menu]"
            );

        if (
            serviceMenu
            && !serviceMenu.contains(event.target)
        ) {
            closeGlobalServiceMenu();
        }
    }
);


/*
 * 서비스 메뉴 닫기
 */
function closeGlobalServiceMenu() {
    const serviceMenu =
        document.querySelector(
            "[data-global-service-menu]"
        );

    const servicePanel =
        document.querySelector(
            "[data-global-service-panel]"
        );

    if (servicePanel) {
        servicePanel.hidden = true;
    }

    if (serviceMenu) {
        serviceMenu.classList.remove(
            "is-open"
        );

        const menuButton =
            serviceMenu.querySelector(
                '[data-action="toggle-service-menu"]'
            );

        if (menuButton) {
            menuButton.setAttribute(
                "aria-expanded",
                "false"
            );
        }
    }
}