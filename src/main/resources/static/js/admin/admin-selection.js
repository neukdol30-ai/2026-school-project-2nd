/*
 * admin-selection.js
 * 회원/상담 목록의 전체선택, 선택해제, 선택 개수 표시 담당.
 */
(function (window, document) {
    const Admin = window.SecondProAdmin = window.SecondProAdmin || {};

    function initSelectionControls() {
        bindSelectionGroup({
            rowSelector: ".member-row-checkbox",
            headerId: "memberSelectAllCheckbox",
            selectAllId: "selectAllMembers",
            clearId: "clearMemberSelection",
            countId: "selectedMemberCount",
            label: "명"
        });

        bindSelectionGroup({
            rowSelector: ".chat-row-checkbox",
            headerId: "chatSelectAllCheckbox",
            selectAllId: "selectAllChats",
            clearId: "clearChatSelection",
            countId: "selectedChatCount",
            label: "건"
        });
    }

    function bindSelectionGroup(options) {
        const headerCheckbox = document.getElementById(options.headerId);
        const selectAllButton = document.getElementById(options.selectAllId);
        const clearButton = document.getElementById(options.clearId);

        if (headerCheckbox) {
            headerCheckbox.addEventListener("change", function () {
                getSelectableCheckboxes(options.rowSelector).forEach(function (checkbox) {
                    checkbox.checked = headerCheckbox.checked;
                });
                updateSelectedCount(options);
            });
        }

        if (selectAllButton) {
            selectAllButton.addEventListener("click", function () {
                const selectableCheckboxes = getSelectableCheckboxes(options.rowSelector);
                selectableCheckboxes.forEach(function (checkbox) {
                    checkbox.checked = true;
                });
                if (headerCheckbox) {
                    headerCheckbox.checked = selectableCheckboxes.length > 0;
                }
                updateSelectedCount(options);
            });
        }

        if (clearButton) {
            clearButton.addEventListener("click", function () {
                getSelectableCheckboxes(options.rowSelector).forEach(function (checkbox) {
                    checkbox.checked = false;
                });
                if (headerCheckbox) {
                    headerCheckbox.checked = false;
                }
                updateSelectedCount(options);
            });
        }

        document.addEventListener("change", function (event) {
            if (!event.target.matches(options.rowSelector)) {
                return;
            }

            const selectableCheckboxes = getSelectableCheckboxes(options.rowSelector);

            if (headerCheckbox) {
                headerCheckbox.checked = selectableCheckboxes.length > 0 && selectableCheckboxes.every(function (item) {
                    return item.checked;
                });
            }

            updateSelectedCount(options);
        });

        updateSelectedCount(options);
    }

    function getSelectableCheckboxes(selector) {
        return Array.from(document.querySelectorAll(selector)).filter(function (checkbox) {
            return !checkbox.disabled;
        });
    }

    function updateSelectedCount(options) {
        const selectedCount = document.getElementById(options.countId);

        if (!selectedCount) {
            return;
        }

        const count = getSelectableCheckboxes(options.rowSelector).filter(function (checkbox) {
            return checkbox.checked;
        }).length;

        selectedCount.textContent = "선택 " + count + options.label;
    }

    function resetChatSelection() {
        const headerCheckbox = document.getElementById("chatSelectAllCheckbox");
        if (headerCheckbox) {
            headerCheckbox.checked = false;
        }
        updateSelectedCount({
            rowSelector: ".chat-row-checkbox",
            countId: "selectedChatCount",
            label: "건"
        });
    }

    Admin.Selection = {
        init: initSelectionControls,
        updateSelectedCount: updateSelectedCount,
        resetChatSelection: resetChatSelection
    };
})(window, document);
