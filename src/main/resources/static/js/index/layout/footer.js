// 하단 정보 영역
function renderGlobalFooter() {
    return `
        <footer class="global-footer">
            <div class="global-footer-inner">
                <section class="footer-notice">
                    <a href="/board/notice" class="footer-notice-title">
                        공지사항
                    </a>

                    <a href="/board/notice">
                        포털 서비스 이용 안내
                    </a>
                </section>

                <nav class="footer-links" aria-label="하단 메뉴">
                    <a href="/board/question">문의 일람</a>
                    <a href="/board/write">고객센터</a>
                </nav>

                <section class="footer-company">
                    <p>2026 IT Project 2nd</p>
                    <p>이 사이트는 학습 및 포트폴리오 목적으로 제작되었습니다.</p>
                    <p>Copyright © 2026 IT Project 2nd. All rights reserved.</p>
                </section>
            </div>
        </footer>
    `;
}