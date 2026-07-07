const { createApp } = Vue;

createApp({
    data() {
        return {
            isEditMode: false,
            memoText: "",
            calculatorText: "",
            newsItems: [],
            newsLoading: false,
            newsError: "",
            dayNames: ["월", "화", "수", "목", "금", "토", "일"],

            currentUser: null,

            loginForm: {
                username: "",
                password: ""
            },

            newsItems: [
                {
                    title: "주요 뉴스",
                    description: "뉴스 요약 내용",
                    link: "https://example.com",
                    source: "언론사",
                    pubDate: "2020-00-00"
                }
            ],

            widgets: [
                {
                    id: 1,
                    zone: "main",
                    type: "news",
                    title: "뉴스",
                    icon: "📰",
                    description: "주요 뉴스를 표시합니다.",
                    visible: true,
                    collapsed: false,
                    orderNo: 1
                },
                {
                    id: 2,
                    zone: "main",
                    type: "issue",
                    title: "주요 이슈",
                    icon: "🔥",
                    description: "오늘의 이슈 키워드를 표시합니다.",
                    visible: true,
                    collapsed: false,
                    orderNo: 2
                },
                {
                    id: 3,
                    zone: "main",
                    type: "schedule",
                    title: "오늘 일정",
                    icon: "📌",
                    description: "오늘의 주요 일정을 표시합니다.",
                    visible: true,
                    collapsed: false,
                    orderNo: 3
                },
                {
                    id: 4,
                    zone: "main",
                    type: "stock",
                    title: "증권",
                    icon: "📈",
                    description: "관심 종목과 차트를 표시합니다.",
                    visible: true,
                    collapsed: false,
                    orderNo: 4
                },
                {
                    id: 5,
                    zone: "side",
                    type: "weather",
                    title: "날씨",
                    icon: "🌤️",
                    description: "현재 날씨를 간단히 표시합니다.",
                    visible: true,
                    collapsed: false,
                    orderNo: 1
                },
                {
                    id: 6,
                    zone: "side",
                    type: "calculator",
                    title: "계산기",
                    icon: "🧮",
                    description: "간단한 계산을 수행합니다.",
                    visible: true,
                    collapsed: false,
                    orderNo: 2
                },
                {
                    id: 7,
                    zone: "side",
                    type: "miniCalendar",
                    title: "미니 캘린더",
                    icon: "📅",
                    description: "이번 달 날짜를 표시합니다.",
                    visible: true,
                    collapsed: false,
                    orderNo: 3
                },
                {
                    id: 8,
                    zone: "side",
                    type: "memo",
                    title: "메모",
                    icon: "📝",
                    description: "간단한 메모를 작성합니다.",
                    visible: true,
                    collapsed: false,
                    orderNo: 4
                }
            ]
        };
    },

    computed: {
        mainWidgets() {
            return this.widgets
                .filter(widget => widget.visible && widget.zone === "main")
                .sort((a, b) => a.orderNo - b.orderNo);
        },

        sideWidgets() {
            return this.widgets
                .filter(widget => widget.visible && widget.zone === "side")
                .sort((a, b) => a.orderNo - b.orderNo);
        }
    },

    mounted() {
        this.fetchNews();
    },

    methods: {

        toggleEditMode(){
            this.isEditMode= !this.isEditMode;
        },

        toggleWidget(id) {
            const widget = this.widgets.find(widget => widget.id === id);

            if (!widget) {
                return;
            }

            widget.visible = !widget.visible;
        },

        async fetchNews() {
            this.newsLoading = true;
            this.newsError = "";

            try{
                const response = await fetch("/api/news");

                if (!response.ok) {
                    throw new Error("뉴스를 불러오지 못했습니다.");
                }

                this.newsItems = await response.json();
            } catch (error) {
                this.newsError = error.message;
            } finally {
                this.newsLoading = false;
            }
        },

        mockLogin() {
            if (!this.loginForm.username) {
                alert("아이디를 입력하세요.");
                return;
            }

            this.currentUser = {
                username: this.loginForm.username,
                nickname: this.loginForm.username
            };

            this.loginForm.password = "";
        },

        mockLogout() {
            this.currentUser = null;
        },

        toggleCollapse(id) {
            const widget = this.widgets.find(widget => widget.id === id);

            if (!widget) {
                return;
            }

            widget.collapsed = !widget.collapsed;
        },

        moveUp(id) {
            const widget = this.widgets.find(widget => widget.id === id);

            if (!widget) {
                return;
            }

            const sameZoneWidgets = this.getVisibleWidgetsByZone(widget.zone);
            const index = sameZoneWidgets.findIndex(item => item.id === id);

            if (index <= 0) {
                return;
            }

            const prevWidget = sameZoneWidgets[index - 1];
            this.swapOrder(widget, prevWidget);
        },

        moveDown(id) {
            const widget = this.widgets.find(widget => widget.id === id);

            if (!widget) {
                return;
            }

            const sameZoneWidgets = this.getVisibleWidgetsByZone(widget.zone);
            const index = sameZoneWidgets.findIndex(item => item.id === id);

            if (index === -1 || index >= sameZoneWidgets.length - 1) {
                return;
            }

            const nextWidget = sameZoneWidgets[index + 1];
            this.swapOrder(widget, nextWidget);
        },

        getVisibleWidgetsByZone(zone) {
            return this.widgets
                .filter(widget => widget.visible && widget.zone === zone)
                .sort((a, b) => a.orderNo - b.orderNo);
        },

        swapOrder(firstWidget, secondWidget) {
            const temp = firstWidget.orderNo;
            firstWidget.orderNo = secondWidget.orderNo;
            secondWidget.orderNo = temp;
        },

        appendCalc(value) {
            this.calculatorText += value;
        },

        clearCalc() {
            this.calculatorText = "";
        },

        calculate() {
            try {
                if (!/^[0-9+\-*/.() ]+$/.test(this.calculatorText)) {
                    this.calculatorText = "Error";
                    return;
                }

                const result = Function(`"use strict"; return (${this.calculatorText})`)();
                this.calculatorText = String(result);
            } catch (e) {
                this.calculatorText = "Error";
            }
        }
    }
}).mount("#app");