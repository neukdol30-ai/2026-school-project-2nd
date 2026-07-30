/*
 * 게시판 공통 헤더·푸터와 환경설정
 */
document.addEventListener(
    "DOMContentLoaded",
    function () {

        /*
         * 공통 상단 헤더 출력
         */
        if (
            typeof updateGlobalBanner
            === "function"
        ) {
            updateGlobalBanner();
        }

        /*
         * 공통 하단 푸터 출력
         */
        const globalFooter =
            document.querySelector(
                "[data-global-footer]"
            );

        if (
            globalFooter
            && typeof renderGlobalFooter
            === "function"
        ) {
            globalFooter.innerHTML =
                renderGlobalFooter();
        }

        /*
         * 게시판 전용 환경설정 생성
         */
        createBoardSettingsDrawer();
    }
);


/*
 * 게시판 전용 환경설정 생성
 *
 * 메인 페이지의 위젯 관리는 제외하고
 * 라이트·다크 모드만 표시한다.
 */
function createBoardSettingsDrawer() {

    /*
     * 이미 생성되었다면 다시 만들지 않는다.
     */
    const existingLayer =
        document.querySelector(
            "[data-board-settings-layer]"
        );

    if (existingLayer) {
        return;
    }

    const settingsLayer =
        document.createElement("div");

    settingsLayer.className =
        "settings-layer board-settings-layer";

    settingsLayer.setAttribute(
        "data-board-settings-layer",
        ""
    );

    settingsLayer.setAttribute(
        "aria-hidden",
        "true"
    );

    settingsLayer.innerHTML = `
        <!-- 설정창 뒤쪽 배경 -->
        <button
            type="button"
            class="settings-backdrop"
            data-board-settings-close
            aria-label="환경설정 닫기"
        ></button>

        <!-- 오른쪽 설정 서랍 -->
        <aside
            class="settings-drawer board-settings-drawer"
            aria-label="환경설정"
        >
            <div class="settings-drawer-header">

                <h2>
                    환경설정
                </h2>

                <button
                    type="button"
                    class="settings-close-button"
                    data-board-settings-close
                    aria-label="환경설정 닫기"
                >
                    ×
                </button>

            </div>

            <div data-control-box>

                <section
                    class="theme-setting-box
                           board-theme-setting-box"
                >

                    <h2>
                        화면 설정
                    </h2>

                    <p class="theme-setting-label">
                        화면 스타일
                    </p>

                    <div
                        class="theme-option-list"
                        role="group"
                        aria-label="화면 스타일 선택"
                    >

                        <!-- 라이트 모드 -->
                        <button
                            type="button"
                            class="theme-option-button"
                            data-action="set-theme"
                            data-value="light"
                            data-theme-option="light"
                            aria-pressed="false"
                        >
                            <span
                                class="theme-option-icon"
                                aria-hidden="true"
                            >
                                ☀
                            </span>

                            <span class="theme-option-text">
                                <strong>
                                    라이트 모드
                                </strong>

                                <span>
                                    밝은 화면
                                </span>
                            </span>

                            <span
                                class="theme-option-check"
                                aria-hidden="true"
                            >
                                ✓
                            </span>
                        </button>


                        <!-- 다크 모드 -->
                        <button
                            type="button"
                            class="theme-option-button"
                            data-action="set-theme"
                            data-value="dark"
                            data-theme-option="dark"
                            aria-pressed="false"
                        >
                            <span
                                class="theme-option-icon"
                                aria-hidden="true"
                            >
                                ☾
                            </span>

                            <span class="theme-option-text">
                                <strong>
                                    다크 모드
                                </strong>

                                <span>
                                    어두운 화면
                                </span>
                            </span>

                            <span
                                class="theme-option-check"
                                aria-hidden="true"
                            >
                                ✓
                            </span>
                        </button>

                    </div>

                </section>

            </div>

        </aside>
    `;

    document.body.appendChild(
        settingsLayer
    );

    /*
     * 현재 저장된 테마와 버튼 상태 맞추기
     */
    refreshBoardThemeButtons();
}


/*
 * 저장된 테마와 설정 버튼 선택 상태 맞추기
 */
function refreshBoardThemeButtons() {

    if (
        !window.PortalTheme
        || typeof window.PortalTheme
            .refreshButtons !== "function"
    ) {
        return;
    }

    const currentTheme =
        document.documentElement
            .dataset.theme
        || window.PortalTheme.get();

    window.PortalTheme.refreshButtons(
        currentTheme
    );
}


/*
 * 게시판 환경설정 열기
 */
function openBoardSettingsDrawer() {

    /*
     * 혹시 생성되지 않았다면 생성
     */
    createBoardSettingsDrawer();

    const settingsLayer =
        document.querySelector(
            "[data-board-settings-layer]"
        );

    if (!settingsLayer) {
        return;
    }

    /*
     * 햄버거 메뉴가 열려 있다면 닫기
     */
    closeGlobalServiceMenu();

    settingsLayer.classList.add(
        "is-open"
    );

    settingsLayer.setAttribute(
        "aria-hidden",
        "false"
    );

    document.body.classList.add(
        "board-settings-open"
    );

    refreshBoardThemeButtons();
}


/*
 * 게시판 환경설정 닫기
 */
function closeBoardSettingsDrawer() {

    const settingsLayer =
        document.querySelector(
            "[data-board-settings-layer]"
        );

    if (!settingsLayer) {
        return;
    }

    settingsLayer.classList.remove(
        "is-open"
    );

    settingsLayer.setAttribute(
        "aria-hidden",
        "true"
    );

    document.body.classList.remove(
        "board-settings-open"
    );
}


/*
 * 게시판 공통 클릭 이벤트
 */
document.addEventListener(
    "click",
    function (event) {

        /*
         * 상단 환경설정 버튼
         */
        const settingsButton =
            event.target.closest(
                '[data-action="toggle-edit"]'
            );

        if (settingsButton) {
            event.preventDefault();

            openBoardSettingsDrawer();
            return;
        }


        /*
         * 환경설정 닫기 버튼 또는 배경
         */
        const settingsCloseButton =
            event.target.closest(
                "[data-board-settings-close]"
            );

        if (settingsCloseButton) {
            event.preventDefault();

            closeBoardSettingsDrawer();
            return;
        }


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
         * 로그아웃
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
         * 서비스 바로가기 링크
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
         * 서비스 메뉴 바깥 클릭
         */
        const serviceMenu =
            document.querySelector(
                "[data-global-service-menu]"
            );

        if (
            serviceMenu
            && !serviceMenu.contains(
                event.target
            )
        ) {
            closeGlobalServiceMenu();
        }
    }
);


/*
 * ESC 키로 환경설정과 서비스 메뉴 닫기
 */
document.addEventListener(
    "keydown",
    function (event) {

        if (event.key !== "Escape") {
            return;
        }

        closeBoardSettingsDrawer();
        closeGlobalServiceMenu();
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