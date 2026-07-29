/*
 * 지도 페이지 공통 배너 연결
 * - 메인·캘린더와 같은 햄버거 메뉴와 화면 설정을 제공합니다.
 * - 기존 mypage-modal.js가 사용하는 전역 상태와 갱신 함수를 유지합니다.
 */

var state =
    window.mapPageState
    || {
        currentUser: null,
        myPage: null
    };

window.mapPageState =
    state;


/*
 * mypage-modal.js의 공통 초기화 조건을 만족시키기 위한
 * 지도 페이지용 호환 함수입니다.
 */
function renderAuthWidget() {

    return "";
}


/*
 * mypage-modal.js가 로그인 정보를 갱신한 뒤
 * 지도 배너도 다시 그릴 수 있도록 연결합니다.
 */
function updateAuthWidget() {

    updateGlobalBanner();
}


/*
 * 지도 화면을 처음 열 때 실제 로그인 상태를 먼저 확인합니다.
 */
async function loadMapLoginState() {

    try {

        const response =
            await fetch(
                "/mypage/me",
                {
                    method: "GET",
                    credentials: "same-origin",
                    cache: "no-store",
                    headers: {
                        "Accept": "application/json",
                        "X-Requested-With": "XMLHttpRequest"
                    }
                }
            );


        if (!response.ok) {

            state.currentUser = null;
            state.myPage = null;

            return;
        }


        const data =
            await response.json();


        if (
            data?.loggedIn === true
            && data.profile
        ) {

            state.currentUser =
                data.profile;

            state.myPage =
                data;

            return;
        }


        state.currentUser = null;
        state.myPage = null;

    } catch (error) {

        /*
         * mypage-modal.js가 먼저 로그인 정보를 넣었다면
         * 해당 정보는 지우지 않습니다.
         */
        if (!state.currentUser) {

            state.currentUser = null;
            state.myPage = null;
        }


        console.warn(
            "[지도 로그인 상태 확인 실패]",
            error
        );
    }
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


/*
 * 상단 계정 메뉴와 햄버거 사용자 영역을 함께 갱신합니다.
 */
function updateGlobalBanner() {

    const userMenu =
        document.querySelector(
            "[data-map-global-user-menu]"
        );

    const serviceUserArea =
        document.querySelector(
            "[data-map-service-user]"
        );


    if (!userMenu) {
        return;
    }


    if (!state.currentUser) {

        userMenu.innerHTML = `
            <a href="/member/login">
                로그인
            </a>
        `;


        if (serviceUserArea) {

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
        }


        return;
    }


    const displayName =
        getMapBannerDisplayName(
            state.currentUser
        );

    const avatarText =
        displayName.charAt(0)
        || "회";


    userMenu.innerHTML = `
        <button type="button"
                data-mypage-open>
            마이페이지
        </button>
    `;


    if (!serviceUserArea) {
        return;
    }


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

                    <span>
                        마이페이지로 이동
                    </span>
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


        if (
            action
            === "toggle-service-menu"
        ) {

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


        if (
            action
            === "open-settings"
        ) {

            setMapSettingsOpen(
                true
            );

            return;
        }


        if (
            action
            === "close-settings"
        ) {

            setMapSettingsOpen(
                false
            );

            return;
        }
    }


    /*
     * 마이페이지 버튼을 누를 때 햄버거 메뉴만 닫고,
     * 클릭 이벤트는 mypage-modal.js가 계속 처리하도록 둡니다.
     */
    if (
        event.target.closest(
            "[data-mypage-open]"
        )
    ) {

        setMapServiceMenuOpen(
            false
        );

        return;
    }


    if (
        event.target.closest(
            "[data-map-service-link]"
        )
    ) {

        setMapServiceMenuOpen(
            false
        );

        return;
    }


    const menu =
        document.querySelector(
            "[data-map-service-menu]"
        );


    if (
        menu?.classList.contains(
            "is-open"
        )
        && !event.target.closest(
            "[data-map-service-menu]"
        )
    ) {

        setMapServiceMenuOpen(
            false
        );
    }
}


function handleMapPageKeydown(event) {

    if (
        event.key
        !== "Escape"
    ) {
        return;
    }


    setMapServiceMenuOpen(
        false
    );

    setMapSettingsOpen(
        false
    );
}


async function initializeMapPageLayout() {

    await loadMapLoginState();

    updateGlobalBanner();
}


document.addEventListener(
    "click",
    handleMapPageClick
);

document.addEventListener(
    "keydown",
    handleMapPageKeydown
);


if (
    document.readyState
    === "loading"
) {

    document.addEventListener(
        "DOMContentLoaded",
        initializeMapPageLayout
    );

} else {

    initializeMapPageLayout();
}
