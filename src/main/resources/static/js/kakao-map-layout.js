/*
 * 지도 페이지 공통 배너 연결
 * - 메인·캘린더와 같은 햄버거 메뉴와 화면 설정을 제공합니다.
 * - 기존 mypage-modal.js가 사용하는 전역 상태와 갱신 함수를 유지합니다.
 */

var state = window.mapPageState || {
    currentUser: null,
    myPage: null
};

window.mapPageState = state;

const MAP_THEME_STORAGE_KEY = "portalTheme";
const MAP_THEME_VALUES = new Set([
    "light",
    "dark"
]);

function renderAuthWidget() {
    return "";
}

function escapeMapBannerHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function getMapBannerDisplayName(user) {
    if (!user) {
        return "회원";
    }

    return String(
        user.displayName
        || user.nickname
        || user.name
        || user.memberId
        || user.username
        || "회원"
    ).trim();
}

function updateGlobalBanner() {
    const userMenu =
        document.querySelector(
            "[data-map-global-user-menu]"
        );

    const serviceUserArea =
        document.querySelector(
            "[data-map-service-user]"
        );

    if (!userMenu || !serviceUserArea) {
        return;
    }

    if (!state.currentUser) {
        userMenu.innerHTML = `
            <a href="/member/login">
                로그인
            </a>
        `;

        serviceUserArea.innerHTML = `
            <a class="global-service-login-link"
               href="/member/login">
                <strong>로그인하세요</strong>
                <span aria-hidden="true">›</span>
            </a>

            <p class="global-service-login-desc">
                로그인하고 여러 서비스를 편리하게 이용하세요.
            </p>
        `;

        return;
    }

    const displayName =
        getMapBannerDisplayName(
            state.currentUser
        );

    const avatarText =
        displayName.charAt(0) || "회";

    userMenu.innerHTML = `
        <button type="button"
                data-mypage-open>
            마이페이지
        </button>
    `;

    serviceUserArea.innerHTML = `
        <div class="global-service-profile">

            <button class="global-service-profile-button"
                    type="button"
                    data-mypage-open>
                <span class="global-service-avatar"
                      aria-hidden="true">
                    ${escapeMapBannerHtml(avatarText)}
                </span>

                <span class="global-service-profile-text">
                    <strong>
                        ${escapeMapBannerHtml(displayName)}님
                    </strong>
                    <span>마이페이지로 이동</span>
                </span>

                <span class="global-service-profile-arrow"
                      aria-hidden="true">
                    ›
                </span>
            </button>

            <form class="global-service-logout-form"
                  action="/member/logout"
                  method="post">
                <button class="global-service-logout-button"
                        type="submit">
                    로그아웃
                </button>
            </form>

        </div>
    `;
}

function normalizeMapTheme(theme) {
    return MAP_THEME_VALUES.has(theme)
        ? theme
        : "light";
}

function getStoredMapTheme() {
    try {
        return normalizeMapTheme(
            localStorage.getItem(
                MAP_THEME_STORAGE_KEY
            )
        );
    } catch (error) {
        console.warn(
            "[지도 테마 불러오기 실패]",
            error
        );

        return "light";
    }
}

function updateMapThemeButtons(theme) {
    document
        .querySelectorAll(
            "[data-map-theme-option]"
        )
        .forEach((button) => {
            const isSelected =
                button.dataset.mapThemeOption
                === theme;

            button.classList.toggle(
                "is-selected",
                isSelected
            );

            button.setAttribute(
                "aria-pressed",
                String(isSelected)
            );
        });
}

function applyMapTheme(theme) {
    const nextTheme =
        normalizeMapTheme(theme);

    document.documentElement.dataset.theme =
        nextTheme;

    document.documentElement.style.colorScheme =
        nextTheme;

    updateMapThemeButtons(
        nextTheme
    );
}

function setMapTheme(theme) {
    const nextTheme =
        normalizeMapTheme(theme);

    applyMapTheme(
        nextTheme
    );

    try {
        localStorage.setItem(
            MAP_THEME_STORAGE_KEY,
            nextTheme
        );
    } catch (error) {
        console.warn(
            "[지도 테마 저장 실패]",
            error
        );
    }
}

function setMapServiceMenuOpen(isOpen) {
    const menu =
        document.querySelector(
            "[data-map-service-menu]"
        );

    if (!menu) {
        return;
    }

    const button =
        menu.querySelector(
            '[data-map-page-action="toggle-service-menu"]'
        );

    const panel =
        menu.querySelector(
            "[data-map-service-panel]"
        );

    if (!button || !panel) {
        return;
    }

    menu.classList.toggle(
        "is-open",
        isOpen
    );

    button.setAttribute(
        "aria-expanded",
        String(isOpen)
    );

    button.setAttribute(
        "aria-label",
        isOpen
            ? "서비스 메뉴 닫기"
            : "서비스 메뉴 열기"
    );

    panel.hidden =
        !isOpen;
}

function setMapSettingsOpen(isOpen) {
    const layer =
        document.querySelector(
            "[data-map-settings-layer]"
        );

    if (!layer) {
        return;
    }

    layer.classList.toggle(
        "is-open",
        isOpen
    );

    layer.setAttribute(
        "aria-hidden",
        String(!isOpen)
    );

    document.body.classList.toggle(
        "map-settings-open",
        isOpen
    );

    if (isOpen) {
        setMapServiceMenuOpen(
            false
        );
    }
}

function handleMapPageClick(event) {
    const actionButton =
        event.target.closest(
            "[data-map-page-action]"
        );

    if (actionButton) {
        const action =
            actionButton.dataset.mapPageAction;

        if (action === "toggle-service-menu") {
            const menu =
                actionButton.closest(
                    "[data-map-service-menu]"
                );

            setMapServiceMenuOpen(
                !menu?.classList.contains(
                    "is-open"
                )
            );

            return;
        }

        if (action === "open-settings") {
            setMapSettingsOpen(true);
            return;
        }

        if (action === "close-settings") {
            setMapSettingsOpen(false);
            return;
        }

        if (action === "set-theme") {
            setMapTheme(
                actionButton.dataset.value
            );

            return;
        }
    }

    if (
        event.target.closest(
            "[data-map-service-link]"
        )
    ) {
        setMapServiceMenuOpen(false);
        return;
    }

    const menu =
        document.querySelector(
            "[data-map-service-menu]"
        );

    if (
        menu?.classList.contains("is-open")
        && !event.target.closest(
            "[data-map-service-menu]"
        )
    ) {
        setMapServiceMenuOpen(false);
    }
}

function handleMapPageKeydown(event) {
    if (event.key !== "Escape") {
        return;
    }

    setMapServiceMenuOpen(false);
    setMapSettingsOpen(false);
}

document.addEventListener(
    "click",
    handleMapPageClick
);

document.addEventListener(
    "keydown",
    handleMapPageKeydown
);

window.addEventListener(
    "storage",
    function (event) {
        if (
            event.key
            !== MAP_THEME_STORAGE_KEY
        ) {
            return;
        }

        applyMapTheme(
            event.newValue
        );
    }
);

document.addEventListener(
    "DOMContentLoaded",
    function () {
        applyMapTheme(
            getStoredMapTheme()
        );

        updateGlobalBanner();
    }
);
