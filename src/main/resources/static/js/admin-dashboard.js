/*
 * admin-dashboard.js
 * 사용 위치: templates/admin/dashboard.html
 * 역할:
 * 1. Swiper 운영 알림 롤링 배너 초기화
 * 2. 최근 7일 상담 추이 막대 높이 계산
 * 3. 카테고리별 상담 통계 progress bar 계산
 */
(function () {
    document.addEventListener("DOMContentLoaded", function () {
        initAdminNoticeSwiper();
        renderDailyChatTrend();
        renderCategoryProgress();
    });

    function initAdminNoticeSwiper() {
        if (typeof Swiper === "undefined") {
            console.warn("Swiper가 로드되지 않아 운영 알림 롤링을 건너뜁니다.");
            return;
        }

        new Swiper(".adminNoticeSwiper", {
            loop: true,
            speed: 650,
            autoplay: {
                delay: 3600,
                disableOnInteraction: false
            },
            pagination: {
                el: ".swiper-pagination",
                clickable: true
            }
        });
    }

    function renderDailyChatTrend() {
        const bars = Array.from(document.querySelectorAll("#dailyChatTrend .trend-bar"));

        if (bars.length === 0) {
            return;
        }

        const counts = bars.map(function (bar) {
            return Number(bar.dataset.count || 0);
        });

        const maxCount = Math.max(...counts, 1);

        bars.forEach(function (bar) {
            const count = Number(bar.dataset.count || 0);
            const height = count === 0 ? 10 : Math.max(28, Math.round((count / maxCount) * 180));
            bar.style.height = height + "px";
        });
    }

    function renderCategoryProgress() {
        const progressBars = Array.from(document.querySelectorAll("#categoryStats .category-progress span"));

        if (progressBars.length === 0) {
            return;
        }

        const counts = progressBars.map(function (bar) {
            return Number(bar.dataset.count || 0);
        });

        const maxCount = Math.max(...counts, 1);

        progressBars.forEach(function (bar) {
            const count = Number(bar.dataset.count || 0);
            const width = count === 0 ? 3 : Math.max(8, Math.round((count / maxCount) * 100));
            bar.style.width = width + "%";
        });
    }
})();
