// 메인 화면의 실제 로그인 정보 읽기
function getLoginUserFromPage() {
    const app = document.querySelector("#app");

    if (!app || app.dataset.login !== "true") {
        return null;
    }

    return {
        username: app.dataset.memberId || "",
        nickname:
            app.dataset.name
            || app.dataset.memberId
            || "회원"
    };
}

// 메인 대시보드 상태
const state = {
    isEditMode: false,
    isSettingsOpen: false,

    memoText: "",
    calculatorText: "",

    currentUser: getLoginUserFromPage(),

    todayScheduleItems: [],
    todayScheduleLoading: false,
    todayScheduleError: "",

    calendarYear: new Date().getFullYear(),
    calendarMonth: new Date().getMonth(),
    dayNames: ["일", "월", "화", "수", "목", "금", "토"],
    miniCalendarDayMap: new Map(),
    miniCalendarLoading: false,
    miniCalendarError: "",
    googleCalendarConnected: false,

    newsItems: [],
    newsLoading: false,
    newsError: "",
    weather: null,
    weatherLoading: false,
    weatherError: "",
    sunTime: {
        sunrise: "--:--",
        sunset: "--:--"
    },

    stockItems: [
        {
            symbol: "005930",
            name: "삼성전자",
            price: 78400,
            changePrice: 1200,
            changeRate: 1.55,
            points: [
                { time: "09:00", price: 77000 },
                { time: "10:00", price: 77500 },
                { time: "11:00", price: 77900 },
                { time: "12:00", price: 78100 },
                { time: "13:00", price: 78400 }
            ]
        },
        {
            symbol: "035420",
            name: "NAVER",
            price: 186500,
            changePrice: -1500,
            changeRate: -0.80,
            points: [
                { time: "09:00", price: 188000 },
                { time: "10:00", price: 187500 },
                { time: "11:00", price: 187000 },
                { time: "12:00", price: 186800 },
                { time: "13:00", price: 186500 }
            ]
        },
        {
            symbol: "035720",
            name: "카카오",
            price: 42100,
            changePrice: 600,
            changeRate: 1.45,
            points: [
                { time: "09:00", price: 41400 },
                { time: "10:00", price: 41700 },
                { time: "11:00", price: 41900 },
                { time: "12:00", price: 42000 },
                { time: "13:00", price: 42100 }
            ]
        },
        {
            symbol: "000660",
            name: "SK하이닉스",
            price: 238000,
            changePrice: -2500,
            changeRate: -1.04,
            points: [
                { time: "09:00", price: 240500 },
                { time: "10:00", price: 239800 },
                { time: "11:00", price: 239000 },
                { time: "12:00", price: 238600 },
                { time: "13:00", price: 238000 }
            ]
        }
    ],
    stockLoading: false,
    stockError: "",
    stockSlideIndex: 0,
    stockCharts: [],
    stockSwiper: null,

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
            title: "예정 일정",
            icon: "📌",
            description: "다가오는 일정을 한눈에 확인하세요.",
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
            orderNo: 5
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
            zone: "main",
            type: "miniCalendar",
            title: "월간 캘린더",
            icon: "📅",
            description: "원하는 날짜를 선택해 보세요.",
            visible: true,
            collapsed: false,
            orderNo: 4
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
        },
        {
            id: 9,
            zone: "side",
            type: "currentTime",
            title: "현재시간",
            icon: "🕒",
            description: "현재 시간과 일출·일몰 정보를 표시합니다.",
            visible: true,
            collapsed: false,
            orderNo: 5
        }

    ]
};