/*
 * admin-dashboard-visual.js
 * 대시보드 차트성 UI(막대/프로그레스) 렌더링 담당.
 */
(function (window, document) {
    const Admin = window.SecondProAdmin = window.SecondProAdmin || {};

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

    Admin.Visual = {
        init: function () {
            renderDailyChatTrend();
            renderCategoryProgress();
        }
    };
})(window, document);
