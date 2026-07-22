//뉴스 위젯
// 카테고리 버튼과 뉴스 목록을 함께 표시
function renderNewsWidget() {
    const categoryButtons = state.newsCategories.map((category) => `
        <button
            class="issue-tag ${
        state.newsCategory === category ? "active" : ""
    }"
            type="button"
            data-action="select-news-category"
            data-value="${escapeHtml(category)}"
        >
            ${escapeHtml(category)}
        </button>
    `).join("");

    let newsContent = "";

    if (state.newsLoading) {
        newsContent = `
            <p class="widget-desc">뉴스를 불러오는 중입니다.</p>
        `;
    } else if (state.newsError) {
        newsContent = `
            <p class="widget-desc">
                ${escapeHtml(state.newsError)}
            </p>
        `;
    } else if (state.newsItems.length === 0) {
        newsContent = `
        <p class="widget-desc">
            해당 분야의 뉴스를 찾지 못했습니다.
        </p>
    `;
    } else {
        newsContent = `
    <ul class="news-compact-list">
        ${state.newsItems.map((news) => `
            <li class="news-compact-item">
                <a
                    class="news-compact-link"
                    href="${escapeHtml(news.link)}"
                    target="_blank"
                    rel="noopener noreferrer"
                >
                    ${news.thumbnailUrl ? `
                        <img
                            class="news-compact-thumbnail"
                            src="${escapeHtml(news.thumbnailUrl)}"
                            alt=""
                            loading="lazy"
                        >
                    ` : `
                        <div class="news-compact-thumbnail placeholder">
                            이미지 없음
                        </div>
                    `}

                    <div class="news-compact-text">
                        <strong class="news-compact-title">
                            ${escapeHtml(news.title)}
                        </strong>

                        <span class="news-compact-meta">
                            ${escapeHtml(getNewsSource(news.link))}
                            ·
                            ${escapeHtml(formatNewsDate(news.publishedAt))}
                        </span>
                    </div>
                </a>
            </li>
        `).join("")}
    </ul>
`;
    }

    return `
        <div class="issue-tags news-category-tabs">
            ${categoryButtons}
        </div>

        <div class="news-result-area">
            ${newsContent}
        </div>
    `;
}

// 뉴스 API 연결
// 선택한 카테고리 뉴스 API 연결
async function fetchNews(category = state.newsCategory) {
    state.newsCategory = category;
    state.newsLoading = true;
    state.newsError = "";

    refreshWidgetContent(1);

    try {
        const response = await fetch(
            `/api/news?category=${encodeURIComponent(category)}`
        );

        if (!response.ok) {
            throw new Error("뉴스를 불러오지 못했습니다.");
        }

        state.newsItems = await response.json();
    } catch (error) {
        state.newsError = error.message;
    } finally {
        state.newsLoading = false;
        refreshWidgetContent(1);
    }
}

// 기사 링크에서 간단한 언론사 도메인 추출
function getNewsSource(link) {
    try {
        return new URL(link).hostname.replace("www.", "");
    } catch {
        return "뉴스";
    }
}

// API의 긴 날짜 문자열을 짧게 표시
function formatNewsDate(value) {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return new Intl.DateTimeFormat("ko-KR", {
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    }).format(date);
}
