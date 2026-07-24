/*
 * kakao-map.js
 * 역할: 카카오맵 REST API 테스트 화면의 폼 제출과 결과 렌더링을 담당합니다.
 */
(() => {
    const API = {
        address: "/api/kakao-map/address",
        coord: "/api/kakao-map/coord-to-address",
        place: "/api/kakao-map/places",
        staticMap: "/api/kakao-map/static"
    };

    let currentStaticMapUrl = null;

    document.addEventListener("DOMContentLoaded", () => {
        bindAddressForm();
        bindCoordForm();
        bindPlaceForm();
        bindStaticMapForm();
    });

    function bindAddressForm() {
        const form = document.querySelector('[data-kakao-form="address"]');
        const result = document.querySelector('[data-result="address"]');

        if (!form || !result) {
            return;
        }

        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            const query = form.querySelector('[name="query"]')?.value?.trim();
            await submitForm(form, result, () => requestJson(API.address, { query }), renderAddressResults);
        });
    }

    function bindCoordForm() {
        const form = document.querySelector('[data-kakao-form="coord"]');
        const result = document.querySelector('[data-result="coord"]');

        if (!form || !result) {
            return;
        }

        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            const x = form.querySelector('[name="x"]')?.value?.trim();
            const y = form.querySelector('[name="y"]')?.value?.trim();
            await submitForm(form, result, () => requestJson(API.coord, { x, y }), renderCoordResults);
        });
    }

    function bindPlaceForm() {
        const form = document.querySelector('[data-kakao-form="place"]');
        const result = document.querySelector('[data-result="place"]');

        if (!form || !result) {
            return;
        }

        form.addEventListener("submit", async (event) => {
            event.preventDefault();

            const payload = {
                query: form.querySelector('[name="query"]')?.value?.trim(),
                x: form.querySelector('[name="x"]')?.value?.trim(),
                y: form.querySelector('[name="y"]')?.value?.trim(),
                radius: form.querySelector('[name="radius"]')?.value?.trim(),
                sort: form.querySelector('[name="sort"]')?.value || "accuracy"
            };

            await submitForm(form, result, () => requestJson(API.place, payload), renderPlaceResults);
        });
    }

    function bindStaticMapForm() {
        const form = document.querySelector('[data-kakao-form="staticMap"]');
        const result = document.querySelector('[data-result="staticMap"]');

        if (!form || !result) {
            return;
        }

        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            await submitStaticMapForm();
        });
    }

    async function submitStaticMapForm() {
        const form = document.querySelector('[data-kakao-form="staticMap"]');
        const result = document.querySelector('[data-result="staticMap"]');
        const image = document.querySelector("[data-static-map-image]");

        if (!form || !result || !image) {
            return;
        }

        const payload = {
            x: form.querySelector('[name="x"]')?.value?.trim(),
            y: form.querySelector('[name="y"]')?.value?.trim(),
            width: form.querySelector('[name="width"]')?.value?.trim(),
            height: form.querySelector('[name="height"]')?.value?.trim(),
            level: form.querySelector('[name="level"]')?.value?.trim(),
            format: form.querySelector('[name="format"]')?.value || "png"
        };

        setLoading(form, true);
        result.innerHTML = messageTemplate("정적 지도 이미지를 불러오는 중입니다...", "");
        image.hidden = true;

        try {
            const objectUrl = await requestImageBlob(API.staticMap, payload);

            if (currentStaticMapUrl) {
                URL.revokeObjectURL(currentStaticMapUrl);
            }

            currentStaticMapUrl = objectUrl;
            image.src = objectUrl;
            image.hidden = false;
            result.innerHTML = messageTemplate("정적 지도 조회가 완료되었습니다.", "success");
        } catch (error) {
            result.innerHTML = messageTemplate(error.message || "정적 지도 조회 중 오류가 발생했습니다.", "error");
        } finally {
            setLoading(form, false);
        }
    }

    async function submitForm(form, resultBox, requestCallback, renderCallback) {
        setLoading(form, true);
        resultBox.innerHTML = messageTemplate("요청 중입니다...", "");

        try {
            const response = await requestCallback();
            renderCallback(resultBox, response);
        } catch (error) {
            resultBox.innerHTML = messageTemplate(error.message || "요청 처리 중 오류가 발생했습니다.", "error");
        } finally {
            setLoading(form, false);
        }
    }

    async function requestJson(url, params) {
        const requestUrl = buildRequestUrl(url, params);
        const response = await fetch(requestUrl, {
            method: "GET",
            headers: {
                "Accept": "application/json"
            }
        });

        if (!response.ok) {
            throw new Error(`서버 요청 실패: HTTP ${response.status}`);
        }

        return response.json();
    }

    async function requestImageBlob(url, params) {
        const requestUrl = buildRequestUrl(url, params);
        const response = await fetch(requestUrl, {
            method: "GET",
            headers: {
                "Accept": "image/png,image/jpeg,text/plain"
            }
        });

        const contentType = response.headers.get("content-type") || "";

        if (!response.ok || !contentType.startsWith("image/")) {
            const message = await response.text();
            throw new Error(message || `정적 지도 요청 실패: HTTP ${response.status}`);
        }

        const blob = await response.blob();
        return URL.createObjectURL(blob);
    }

    function buildRequestUrl(url, params) {
        const queryString = new URLSearchParams();

        Object.entries(params)
            .filter(([, value]) => value !== undefined && value !== null && String(value).trim() !== "")
            .forEach(([key, value]) => queryString.append(key, value));

        return queryString.toString() ? `${url}?${queryString}` : url;
    }

    function renderAddressResults(resultBox, response) {
        if (!response.success) {
            resultBox.innerHTML = messageTemplate(response.message, "error") + rawJsonTemplate(response.data);
            return;
        }

        const documents = response.data?.documents || [];

        if (documents.length === 0) {
            resultBox.innerHTML = messageTemplate("검색 결과가 없습니다.", "");
            return;
        }

        const items = documents.map((item) => {
            const roadAddress = item.road_address?.address_name || "도로명 주소 없음";
            const address = item.address?.address_name || item.address_name || "주소 없음";
            return `
                <li class="result-item">
                    <strong>${escapeHtml(address)}</strong>
                    <span>${escapeHtml(roadAddress)}</span>
                    <small>x: ${escapeHtml(item.x)} / y: ${escapeHtml(item.y)}</small>
                    <div class="result-actions">
                        <button type="button" data-copy-coord="${escapeAttribute(item.x)},${escapeAttribute(item.y)}">좌표 복사</button>
                        <button type="button" data-static-map="${escapeAttribute(item.x)},${escapeAttribute(item.y)}">정적 지도 보기</button>
                    </div>
                </li>
            `;
        }).join("");

        resultBox.innerHTML = messageTemplate(response.message, "success") + `<ul class="result-list">${items}</ul>`;
        bindResultActionButtons(resultBox);
    }

    function renderCoordResults(resultBox, response) {
        if (!response.success) {
            resultBox.innerHTML = messageTemplate(response.message, "error") + rawJsonTemplate(response.data);
            return;
        }

        const documents = response.data?.documents || [];

        if (documents.length === 0) {
            resultBox.innerHTML = messageTemplate("좌표에 해당하는 주소 결과가 없습니다.", "");
            return;
        }

        const items = documents.map((item) => {
            const address = item.address?.address_name || "주소 없음";
            const roadAddress = item.road_address?.address_name || "도로명 주소 없음";
            const region = [item.address?.region_1depth_name, item.address?.region_2depth_name, item.address?.region_3depth_name]
                .filter(Boolean)
                .join(" ");

            return `
                <li class="result-item">
                    <strong>${escapeHtml(address)}</strong>
                    <span>${escapeHtml(roadAddress)}</span>
                    <small>${escapeHtml(region || "행정구역 정보 없음")}</small>
                </li>
            `;
        }).join("");

        resultBox.innerHTML = messageTemplate(response.message, "success") + `<ul class="result-list">${items}</ul>`;
    }

    function renderPlaceResults(resultBox, response) {
        if (!response.success) {
            resultBox.innerHTML = messageTemplate(response.message, "error") + rawJsonTemplate(response.data);
            return;
        }

        const documents = response.data?.documents || [];

        if (documents.length === 0) {
            resultBox.innerHTML = messageTemplate("장소 검색 결과가 없습니다.", "");
            return;
        }

        const items = documents.map((item) => {
            const address = item.road_address_name || item.address_name || "주소 없음";
            const distance = item.distance ? `${Number(item.distance).toLocaleString()}m` : "거리 정보 없음";
            return `
                <li class="result-item">
                    <strong>${escapeHtml(item.place_name || "장소명 없음")}</strong>
                    <span>${escapeHtml(address)}</span>
                    <small>${escapeHtml(item.category_name || "카테고리 없음")} · ${escapeHtml(distance)}</small>
                    <small>x: ${escapeHtml(item.x)} / y: ${escapeHtml(item.y)}</small>
                    <div class="result-actions">
                        <button type="button" data-copy-coord="${escapeAttribute(item.x)},${escapeAttribute(item.y)}">좌표 복사</button>
                        <button type="button" data-static-map="${escapeAttribute(item.x)},${escapeAttribute(item.y)}">정적 지도 보기</button>
                        ${item.place_url ? `<a href="${escapeAttribute(item.place_url)}" target="_blank" rel="noopener noreferrer">카카오맵 보기</a>` : ""}
                    </div>
                </li>
            `;
        }).join("");

        resultBox.innerHTML = messageTemplate(response.message, "success") + `<ul class="result-list">${items}</ul>`;
        bindResultActionButtons(resultBox);
    }

    function bindResultActionButtons(scope) {
        bindCopyButtons(scope);
        bindStaticMapButtons(scope);
    }

    function bindCopyButtons(scope) {
        scope.querySelectorAll("[data-copy-coord]").forEach((button) => {
            button.addEventListener("click", async () => {
                const value = button.dataset.copyCoord || "";

                try {
                    await navigator.clipboard.writeText(value);
                    button.textContent = "복사 완료";
                    setTimeout(() => button.textContent = "좌표 복사", 1200);
                } catch (_) {
                    button.textContent = value;
                }
            });
        });
    }

    function bindStaticMapButtons(scope) {
        scope.querySelectorAll("[data-static-map]").forEach((button) => {
            button.addEventListener("click", async () => {
                const [x, y] = String(button.dataset.staticMap || "").split(",");
                const form = document.querySelector('[data-kakao-form="staticMap"]');

                if (!form || !x || !y) {
                    return;
                }

                form.querySelector('[name="x"]').value = x;
                form.querySelector('[name="y"]').value = y;
                await submitStaticMapForm();

                document.querySelector(".static-map-card")?.scrollIntoView({
                    behavior: "smooth",
                    block: "start"
                });
            });
        });
    }

    function setLoading(form, loading) {
        form.querySelectorAll("button, input, select").forEach((element) => {
            element.disabled = loading;
        });
    }

    function messageTemplate(message, type) {
        const className = type ? `result-message ${type}` : "result-message";
        return `<div class="${className}">${escapeHtml(message || "")}</div>`;
    }

    function rawJsonTemplate(data) {
        if (!data) {
            return "";
        }

        return `<pre class="raw-json">${escapeHtml(JSON.stringify(data, null, 2))}</pre>`;
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function escapeAttribute(value) {
        return escapeHtml(value).replaceAll("`", "&#096;");
    }
})();
