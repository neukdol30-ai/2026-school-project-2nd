/*
 * 공통 화면 테마
 * 메인, 캘린더, 지도에서 같은 portalTheme 값을 사용합니다.
 */
(function () {
    "use strict";

    const STORAGE_KEY = "portalTheme";
    const LEGACY_CALENDAR_KEY = "calendar-theme";
    const ALLOWED_THEMES = new Set([
        "light",
        "dark"
    ]);

    function normalize(theme) {
        return ALLOWED_THEMES.has(theme)
            ? theme
            : "light";
    }

    function getStoredTheme() {
        try {
            const storedTheme =
                localStorage.getItem(
                    STORAGE_KEY
                );

            if (ALLOWED_THEMES.has(storedTheme)) {
                return storedTheme;
            }

            const legacyTheme =
                localStorage.getItem(
                    LEGACY_CALENDAR_KEY
                );

            if (ALLOWED_THEMES.has(legacyTheme)) {
                localStorage.setItem(
                    STORAGE_KEY,
                    legacyTheme
                );

                return legacyTheme;
            }
        } catch (error) {
            console.warn(
                "[공통 테마 불러오기 실패]",
                error
            );
        }

        return "light";
    }

    function getButtonTheme(button) {
        return button.dataset.themeOption
            || button.dataset.mapThemeOption
            || button.dataset.value
            || "";
    }

    function updateButtons(theme) {
        document
            .querySelectorAll(
                "[data-theme-option], "
                + "[data-map-theme-option]"
            )
            .forEach((button) => {
                const isSelected =
                    getButtonTheme(button)
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

    function syncPageState(theme) {
        if (
            window.state
            && typeof window.state === "object"
        ) {
            window.state.theme = theme;
        }
    }

    function apply(theme) {
        const nextTheme =
            normalize(theme);

        document.documentElement.dataset.theme =
            nextTheme;

        document.documentElement.style.colorScheme =
            nextTheme;

        syncPageState(nextTheme);
        updateButtons(nextTheme);

        window.dispatchEvent(
            new CustomEvent(
                "portal-theme-changed",
                {
                    detail: {
                        theme: nextTheme
                    }
                }
            )
        );

        return nextTheme;
    }

    function set(theme) {
        const nextTheme =
            apply(theme);

        try {
            localStorage.setItem(
                STORAGE_KEY,
                nextTheme
            );

            localStorage.setItem(
                LEGACY_CALENDAR_KEY,
                nextTheme
            );
        } catch (error) {
            console.warn(
                "[공통 테마 저장 실패]",
                error
            );
        }

        return nextTheme;
    }

    function initialize() {
        apply(
            getStoredTheme()
        );
    }

    function findThemeButton(target) {
        return target.closest(
            '[data-action="set-theme"], '
            + '[data-calendar-action="set-theme"], '
            + '[data-map-page-action="set-theme"]'
        );
    }

    document.addEventListener(
        "click",
        function (event) {
            const button =
                findThemeButton(
                    event.target
                );

            if (!button) {
                return;
            }

            set(
                button.dataset.value
                || getButtonTheme(button)
            );
        }
    );

    window.addEventListener(
        "storage",
        function (event) {
            if (event.key !== STORAGE_KEY) {
                return;
            }

            apply(event.newValue);
        }
    );

    window.addEventListener(
        "portal-theme-refresh",
        function () {
            updateButtons(
                normalize(
                    document.documentElement
                        .dataset.theme
                )
            );
        }
    );

    window.PortalTheme = Object.freeze({
        get: getStoredTheme,
        apply: apply,
        set: set,
        initialize: initialize,
        refreshButtons: updateButtons
    });

    /*
     * 메인 화면 기존 코드가 setPortalTheme()을 호출해도
     * 공통 테마 파일을 사용하도록 호환 함수를 공개합니다.
     */
    window.setPortalTheme = set;
    window.getStoredPortalTheme = getStoredTheme;
    window.applyPortalTheme = apply;
    window.initializePortalTheme = initialize;

    if (document.readyState === "loading") {
        document.addEventListener(
            "DOMContentLoaded",
            initialize,
            {
                once: true
            }
        );
    } else {
        initialize();
    }
})();
