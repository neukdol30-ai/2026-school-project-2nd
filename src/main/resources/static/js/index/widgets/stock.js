// 그래프 함수 추가
function drawStockCharts() {
    document.querySelectorAll("[data-stock-chart-index]").forEach((canvas) => {
        const index = Number(canvas.dataset.stockChartIndex);
        const stock = state.stockItems[index];
        const slide = canvas.closest(".swiper-slide");
        const isActive = slide && slide.classList.contains("swiper-slide-active");

        if (!stock || !stock.points || stock.points.length === 0) {
            return;
        }

        if (state.stockCharts[index]) {
            state.stockCharts[index].destroy();
        }

        state.stockCharts[index] = new Chart(canvas, {
            type: "line",
            data: {
                labels: stock.points.map((point) => point.time),
                datasets: [
                    {
                        data: stock.points.map((point) => point.price),
                        borderColor: stock.changeRate >= 0 ? "#dc2626" : "#2563eb",
                        backgroundColor: "transparent",
                        borderWidth: isActive ? 2.5 : 1.5,
                        tension: 0,
                        pointRadius: 0
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: false,
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        enabled: isActive
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
        slidesPerView: "auto",
        spaceBetween: 12,
        speed: 450,
        loop: true,
        initialSlide: initialIndex,
        observer: true,
        observeParents: true,
        autoplay: {
            delay: 5000,
            disableOnInteraction: false
        },
        navigation: {
            nextEl: ".stock-nav-next",
            prevEl: ".stock-nav-prev"
        },
        on: {
            init: function () {
                this.slideToLoop(initialIndex, 0, false);
                requestAnimationFrame(() => {
                    drawStockCharts();
                });
            },

            slideChange: function () {
                state.stockSlideIndex = this.realIndex ?? this.activeIndex;
            },

            slideChangeTransitionEnd: function () {
                state.stockSlideIndex = this.realIndex ?? this.activeIndex;
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

//주식 관련 위젯
function renderStockWidget() {
    if (state.stockLoading) {
        return `<p class="widget-desc">증권 정보를 불러오는 중입니다.</p>`;
    }

    if (state.stockError) {
        return `<p class="widget-desc">${state.stockError}</p>`;
    }

    if (!state.stockItems || state.stockItems.length === 0) {
        return `<p class="widget-desc">표시할 증권 정보가 없습니다.</p>`;
    }

    return `
    <div class="stock-film-widget">
        <div class="swiper stock-swiper">
            <div class="swiper-wrapper">
                ${state.stockItems.map((stock, index) => `
                    <div class="swiper-slide stock-film-card">
                        <div class="stock-card-top">
                            <div>
                                <strong>${escapeHtml(stock.name)}</strong>
                                <span>${escapeHtml(stock.symbol)}</span>
                            </div>

                            <div class="${stock.changeRate >= 0 ? "stock-up" : "stock-down"}">
                                <strong>${stock.price.toLocaleString()}원</strong>
                                <span>${stock.changeRate}%</span>
                            </div>
                        </div>

                        <div class="stock-chart-wrap">
                            <canvas
                                class="stock-chart"
                                data-stock-chart-index="${index}"
                            ></canvas>
                        </div>

                        <div class="stock-mini-price">
                            ${stock.changePrice > 0 ? "+" : ""}${stock.changePrice.toLocaleString()}원
                        </div>
                    </div>
                `).join("")}
            </div>
            
            <button class="swiper-button-prev stock-nav-button stock-nav-prev" type="button">‹</button>
            <button class="swiper-button-next stock-nav-button stock-nav-next" type="button">›</button>
        </div>
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
    } catch (error) {
        state.stockError = "증권 정보를 불러오지 못했습니다.";
    } finally {
        state.stockLoading = false;
        refreshWidgetContent(4);
    }
}