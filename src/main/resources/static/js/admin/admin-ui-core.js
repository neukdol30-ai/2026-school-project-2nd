/*
 * admin-ui-core.js
 * 관리자 콘솔 공통 유틸 모음.
 * 화면별 스크립트에서 반복되는 DOM/문자열/POST form 처리를 한 곳에서 관리합니다.
 */
(function (window, document) {
    const Admin = window.SecondProAdmin = window.SecondProAdmin || {};

    Admin.ready = function (callback) {
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", callback);
            return;
        }
        callback();
    };

    Admin.setText = function (id, value) {
        const target = document.getElementById(id);
        if (target) {
            target.textContent = value;
        }
    };

    Admin.escapeHtml = function (value) {
        if (value == null) {
            return "";
        }

        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    };

    Admin.formatDateTime = function (value) {
        if (!value) {
            return "-";
        }

        let date;

        if (Array.isArray(value)) {
            date = new Date(
                Number(value[0]),
                Number(value[1]) - 1,
                Number(value[2]),
                Number(value[3] || 0),
                Number(value[4] || 0),
                Number(value[5] || 0)
            );
        } else {
            date = new Date(value);
        }

        if (Number.isNaN(date.getTime())) {
            return String(value);
        }

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        const hour = String(date.getHours()).padStart(2, "0");
        const minute = String(date.getMinutes()).padStart(2, "0");

        return year + "-" + month + "-" + day + " " + hour + ":" + minute;
    };

    Admin.getCheckedValues = function (selector) {
        return Array.from(document.querySelectorAll(selector + ":checked"))
            .filter(function (checkbox) {
                return !checkbox.disabled;
            })
            .map(function (checkbox) {
                return checkbox.value;
            })
            .filter(Boolean);
    };

    Admin.submitPostForm = function (action, fieldName, values) {
        const fields = {};

        if (fieldName && Array.isArray(values)) {
            fields[fieldName] = values;
        }

        Admin.submitPostFormFields(action, fields);
    };

    Admin.submitPostFormFields = function (action, fields) {
        const form = document.createElement("form");
        form.method = "post";
        form.action = action;
        form.style.display = "none";

        Object.entries(fields || {}).forEach(function ([name, value]) {
            const values = Array.isArray(value) ? value : [value];

            values.forEach(function (item) {
                const input = document.createElement("input");
                input.type = "hidden";
                input.name = name;
                input.value = item;
                form.appendChild(input);
            });
        });

        document.body.appendChild(form);
        form.submit();
    };
})(window, document);
