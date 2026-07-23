(function () {
    function findInput(selector) {
        if (!selector) {
            return null;
        }
        return document.querySelector(selector);
    }

    function openAddressSearch(button) {
        if (!window.daum || !window.daum.Postcode) {
            alert("주소 검색 서비스를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
            return;
        }

        const postcodeInput = findInput(button.dataset.postcodeTarget);
        const addressInput = findInput(button.dataset.addressTarget);
        const detailInput = findInput(button.dataset.detailTarget);

        new window.daum.Postcode({
            oncomplete: function (data) {
                let address = data.roadAddress || data.jibunAddress || "";
                let extraAddress = "";

                if (data.userSelectedType === "R") {
                    if (data.bname && /[동|로|가]$/g.test(data.bname)) {
                        extraAddress += data.bname;
                    }
                    if (data.buildingName && data.apartment === "Y") {
                        extraAddress += extraAddress ? ", " + data.buildingName : data.buildingName;
                    }
                    if (extraAddress) {
                        address += " (" + extraAddress + ")";
                    }
                }

                if (postcodeInput) {
                    postcodeInput.value = data.zonecode || "";
                }
                if (addressInput) {
                    addressInput.value = address;
                }
                if (detailInput) {
                    detailInput.focus();
                }
            }
        }).open();
    }

    document.addEventListener("click", function (event) {
        const button = event.target.closest("[data-address-search]");
        if (!button) {
            return;
        }

        event.preventDefault();
        openAddressSearch(button);
    });
})();