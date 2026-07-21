(function () {
    "use strict";

    const PASSWORD_AUTOFILL_COOKIE_NAME = "SECONDPRO_PASSWORD_AUTOFILL";

    document.addEventListener("DOMContentLoaded", function () {
        applyPasswordAutocompleteSetting();
    });

    function applyPasswordAutocompleteSetting() {
        const passwordAutofillAllowed = getCookie(PASSWORD_AUTOFILL_COOKIE_NAME) === "Y";
        const forms = document.querySelectorAll("[data-password-autocomplete-form]");

        forms.forEach(function (form) {
            form.setAttribute("autocomplete", passwordAutofillAllowed ? "on" : "off");

            const inputs = form.querySelectorAll("[data-autocomplete-allowed]");

            inputs.forEach(function (input) {
                const allowedValue = input.getAttribute("data-autocomplete-allowed");

                if (passwordAutofillAllowed && allowedValue) {
                    input.setAttribute("autocomplete", allowedValue);
                } else {
                    input.setAttribute("autocomplete", "off");
                }
            });
        });
    }

    function getCookie(name) {
        const target = encodeURIComponent(name) + "=";
        const cookies = document.cookie ? document.cookie.split(";") : [];

        for (const rawCookie of cookies) {
            const cookie = rawCookie.trim();
            if (cookie.indexOf(target) === 0) {
                return decodeURIComponent(cookie.substring(target.length));
            }
        }

        return "";
    }
})();
