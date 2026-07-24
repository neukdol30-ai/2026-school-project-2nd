// 그래프 함수 추가
// 각 종목의 1개월 가격 그래프 생성
function drawStockCharts() {
    document
        .querySelectorAll("[data-stock-chart-index]")
        .forEach((canvas) => {
            const index = Number(canvas.dataset.stockChartIndex);
            const stock = state.stockItems[index];
            const slide = canvas.closest(".swiper-slide");

            const isActive =
                slide &&
                slide.classList.contains("swiper-slide-active");

            if (
                !stock ||
                !stock.points ||
                stock.points.length === 0
            ) {
                return;
            }

            if (state.stockCharts[index]) {
                state.stockCharts[index].destroy();
            }

            const changeRate = Number(stock.changeRate ?? 0);
            const context = canvas.getContext("2d");

            let lineColor;
            let fillStartColor;
            let fillEndColor;

            if (changeRate > 0) {
                // 상승: 빨간색
                lineColor = "#e5484d";
                fillStartColor = "rgba(229, 72, 77, 0.22)";
                fillEndColor = "rgba(229, 72, 77, 0.01)";
            } else if (changeRate < 0) {
                // 하락: 파란색
                lineColor = "#3182f6";
                fillStartColor = "rgba(49, 130, 246, 0.22)";
                fillEndColor = "rgba(49, 130, 246, 0.01)";
            } else {
                // 보합: 회색
                lineColor = "#6b7280";
                fillStartColor = "rgba(107, 114, 128, 0.18)";
                fillEndColor = "rgba(107, 114, 128, 0.01)";
            }

            // 선 아래쪽에 적용할 세로 그라데이션
            const gradient = context.createLinearGradient(
                0,
                0,
                0,
                190
            );

            gradient.addColorStop(0, fillStartColor);
            gradient.addColorStop(1, fillEndColor);

            state.stockCharts[index] = new Chart(context, {
                type: "line",

                data: {
                    labels: stock.points.map(
                        (point) => point.time
                    ),

                    datasets: [
                        {
                            data: stock.points.map(
                                (point) => point.price
                            ),

                            borderColor: lineColor,
                            backgroundColor: gradient,

                            borderWidth: 2,
                            pointRadius: 0,
                            pointHoverRadius: 4,

                            // 약간만 부드럽게 연결
                            tension: 0.18,

                            // 선 아래 영역 채우기
                            fill: true
                        }
                    ]
                },

                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: false,

                    interaction: {
                        mode: "index",
                        intersect: false
                    },

                    layout: {
                        padding: {
                            top: 8,
                            bottom: 4
                        }
                    },

                    plugins: {
                        legend: {
                            display: false
                        },

                        tooltip: {
                            enabled: isActive,

                            callbacks: {
                                label: function (context) {
                                    return Number(
                                        context.raw
                                    ).toLocaleString() + "원";
                                }
                            }
                        }
                    },

                    scales: {
                        x: {
                            display: false
                        },

                        y: {
                            display: false
                        }
                    }
                }
            });
        });
}

//swiper 슬라이더 초기화
function initStockSwiper() {
    const swiperElement = document.querySelector(".stock-swiper");


    if (!swiperElement) {
        if(state.stockSwiper){
            rememberStockSwiperIndex();
            state.stockSwiper.destroy(true, true);
            state.stockSwiper = null;
        }

        return;

    }

    if (state.stockSwiper) {
        state.stockSwiper.destroy(true, true);
        state.stockSwiper = null;
    }

    const initialIndex = Math.min(
        Math.max(state.stockSlideIndex, 0),
        Math.max(state.stockItems.length -1, 0)
    );

    state.stockSwiper = new Swiper(swiperElement, {
        // 한 번에 그래프 하나만 표시
        slidesPerView: 1,
        spaceBetween: 0,

        // 일정한 속도로 이동
        speed: 600,

        // 마지막 다음에 첫 번째가 같은 방향으로 이어짐
        loop: true,
        rewind: false,
        loopAdditionalSlides: 2,

        initialSlide: initialIndex,

        autoplay: {
            delay: 5000,
            disableOnInteraction: false,
            reverseDirection: false,
            waitForTransition: true
        },

        navigation: {
            nextEl: ".stock-nav-next",
            prevEl: ".stock-nav-prev"
        },

        on: {
            init: function () {
                this.slideToLoop(initialIndex, 0, false);
                updateStockQuoteSelection(initialIndex);

                requestAnimationFrame(() => {
                    drawStockCharts();
                });
            },

            slideChange: function () {
                const currentIndex =
                    this.realIndex ?? this.activeIndex;

                state.stockSlideIndex = currentIndex;
                updateStockQuoteSelection(currentIndex);
            },

            slideChangeTransitionEnd: function () {
                state.stockSlideIndex =
                    this.realIndex ?? this.activeIndex;

                requestAnimationFrame(() => {
                    drawStockCharts();
                });
            }
        }
    });
}

//슬라이스 위치 저장
function rememberStockSwiperIndex() {
    if (!state.stockSwiper) {
        return;
    }

    state.stockSlideIndex = state.stockSwiper.realIndex ?? state.stockSwiper.activeIndex ?? 0;
}

// 상승·하락에 따른 색상과 화살표 결정
function getStockDirection(stock) {
    const changeRate = Number(stock.changeRate ?? 0);

    if (changeRate > 0) {
        return {
            className: "stock-up",
            symbol: "▲"
        };
    }

    if (changeRate < 0) {
        return {
            className: "stock-down",
            symbol: "▼"
        };
    }

    return {
        className: "stock-flat",
        symbol: "-"
    };
}

//주식 관련 위젯
function renderStockWidget() {
    if (state.stockLoading) {
        return `
            <div class="stock-dashboard stock-status">
                <p class="widget-desc">
                    증권 정보를 불러오는 중입니다.
                </p>
            </div>
        `;
    }

    if (state.stockError) {
        return `
            <div class="stock-dashboard stock-status">
                <p class="widget-desc">
                    ${escapeHtml(state.stockError)}
                </p>
            </div>
        `;
    }

    if (!state.stockItems || state.stockItems.length === 0) {
        return `
            <div class="stock-dashboard stock-status">
                <p class="widget-desc">
                    표시할 증권 정보가 없습니다.
                </p>
            </div>
        `;
    }

    return `
        <div class="stock-dashboard"> 
                <section class="stock-focus-area">
                <div class="swiper stock-swiper">
                    <div class="swiper-wrapper">
                        ${state.stockItems.map((stock, index) => {
        const direction = getStockDirection(stock);

        return `
                                <div class="swiper-slide stock-focus-slide">
                                    <div class="stock-focus-heading">
                                        <div>
                                            <strong class="stock-focus-name">
                                                ${escapeHtml(stock.name)}
                                            </strong>

                                            <span class="stock-focus-symbol">
                                                ${escapeHtml(stock.symbol)}
                                            </span>
                                        </div>
                                    </div>

                                    <div class="stock-focus-price">
                                        ${Number(stock.price ?? 0).toLocaleString()}
                                        <span>원</span>
                                    </div>

                                    <div class="stock-focus-change ${direction.className}">
                                        ${direction.symbol}
                                        ${Math.abs(
                                        Number(stock.changePrice ?? 0)
                                    ).toLocaleString()}원
                            
                                                                    <span>
                                                                        ${Math.abs(
                                        Number(stock.changeRate ?? 0)
                                    ).toFixed(2)}%
                                        </span>
                                    </div>

                                    <div class="stock-chart-wrap">
                                        <canvas
                                            class="stock-chart"
                                            data-stock-chart-index="${index}"
                                        ></canvas>
                                    </div>

                                    <div class="stock-chart-period">
                                        최근 1개월
                                    </div>
                                </div>
                            `;
    }).join("")}
                    </div>

                    <button
                        class="swiper-button-prev stock-nav-button stock-nav-prev"
                        type="button"
                        aria-label="이전 종목"
                    >‹</button>

                    <button
                        class="swiper-button-next stock-nav-button stock-nav-next"
                        type="button"
                        aria-label="다음 종목"
                    >›</button>
                </div>
            </section>

            <aside class="stock-quote-panel">
                <div class="stock-quote-list">
                    ${state.stockItems.map((stock, index) => {
        const direction = getStockDirection(stock);

        return `
                            <button
                                class="stock-quote-item ${
                                        state.stockSlideIndex === index ? "active" : ""
                                    }"
                                type="button"
                                data-action="select-stock"
                                data-value="${index}"
                                data-stock-index="${index}"
                                aria-pressed="${
                                        state.stockSlideIndex === index
                                    }"
                            >
                                <span class="stock-quote-name">
                                    ${escapeHtml(stock.name)}
                                </span>

                                <span class="stock-quote-values">
                                    <span class="${direction.className}">
                                        ${direction.symbol}
                                        ${Math.abs(
            Number(stock.changePrice ?? 0)
        ).toLocaleString()}
                                    </span>

                                    <strong>
                                        ${Number(stock.price ?? 0).toLocaleString()}
                                    </strong>
                                </span>
                            </button>
                        `;
    }).join("")}
                </div>
            </aside>
        </div>
    `;
}

//증권 api 연결
async function fetchStocks() {
    state.stockLoading = true;
    state.stockError = "";
    refreshWidgetContent(4);

    try {
        const response = await fetch("/api/stocks");

        if (!response.ok) {
            throw new Error("증권 정보를 불러오지 못했습니다.");
        }

        const stocks = await response.json();

        state.stockItems = stocks;
        state.stockUpdatedAt = new Date();
    } catch (error) {
        state.stockError = "증권 정보를 불러오지 못했습니다.";
    } finally {
        state.stockLoading = false;
        refreshWidgetContent(4);
    }
}

// 현재 그래프에 해당하는 오른쪽 종목 강조
function updateStockQuoteSelection(index) {
    document
        .querySelectorAll("[data-stock-index]")
        .forEach((item) => {
            const itemIndex = Number(item.dataset.stockIndex);
            const isActive = itemIndex === index;

            item.classList.toggle("active", isActive);
            item.setAttribute(
                "aria-pressed",
                String(isActive)
            );
        });
}

// 증권 정보 갱신 시간 표시
function formatStockUpdatedAt(value) {
    if (!value) {
        return "--.-- --:--";
    }

    const date = new Date(value);

    return new Intl.DateTimeFormat("ko-KR", {
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    }).format(date);
}