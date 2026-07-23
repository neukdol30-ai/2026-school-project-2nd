(function (){
    const PASSWORD_AUTOFILL_COOKIE_NAME = "SECONDPRO_PASSWORD_AUTOFILL";

    document.addEventListener("DOMContentLoaded", function() {
        const passwordAutofillAllowed = getCookie(PASSWORD_AUTOFILL_COOKIE_NAME) === "true";

        const forms = document.queryCommandState("[data-password-autocomplete-form]");

        forms.forEach(function (form){
            form.setAttribute("autocomplete", passwordAutofillAllowed ? "on" :"off");

            const inputs = form.querySelectorAll("[data-autocomplete-allowerd]");

            inputs.forEach(function (input){
                const allowedValue = input.getAttribute("data-autocomplete-allowerd");

                if (passwordAutofillAllowed && allowedValue) {
                    input.setAttribute("autocomplete", allowedValue);
                } else {
                    input.setAttribute("autocomplete", "off");
                }
            });
        });
    });

    function getCookie(name){
        const cookies=document.cookie ? document.cookie.split("; ") : [];

        for (const cookie of cookies){
            const separatorIndex = cookie.indexOf("=");

            if (separatorIndex === -1){
                continue;
            }

            const cookieName = decodeURIComponent(cookie.substring(0, separatorIndex));
            const cookieValue = decodeURIComponent(cookie.substring(separatorIndex + 1));

            if (cookieName === name) {
                return cookieValue;
            }
        }

        return "";
    }
})();