//뉴스 위젯
function renderNewsWidget() {
    if (state.newsLoading) {
        return `<p class="widget-desc">뉴스를 불러오는 중입니다.</p>`;
    }

    if (state.newsError) {
        return `<p class="widget-desc">${escapeHtml(state.newsError)}</p>`;
    }

    return `
        <ul class="news-list">
            ${state.newsItems.map((news) => `
                <li class="news-item">
                    <a
                        class="news-title"
                        href="${escapeHtml(news.link)}"
                        target="_blank"
                        rel="noopener noreferrer"
                    >
                        ${escapeHtml(news.title)}
                    </a>

                    <p class="news-summary">
                        ${escapeHtml(news.summary)}
                    </p>

                    <div class="news-meta">
                        ${escapeHtml(news.source)} · ${escapeHtml(news.publishedAt)}
                    </div>
                </li>
            `).join("")}
        </ul>
    `;
}

// 뉴스 API 연결
async function fetchNews() {
    state.newsLoading = true;
    state.newsError = "";
    render();

    try {
        const response = await fetch("/api/news");

        if (!response.ok) {
            throw new Error("뉴스를 불러오지 못했습니다.");
        }

        state.newsItems = await response.json();
    } catch (error) {
        state.newsError = error.message;
    } finally {
        state.newsLoading = false;
        render();
    }
}
