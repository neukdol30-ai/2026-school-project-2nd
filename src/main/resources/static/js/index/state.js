// 공통 화면 테마 설정
const PORTAL_THEME_STORAGE_KEY = "portalTheme";
const PORTAL_THEME_VALUES = new Set([
    "light",
    "dark"
]);

function normalizePortalTheme(theme) {
    return PORTAL_THEME_VALUES.has(theme)
        ? theme
        : "light";
}

function getStoredPortalTheme() {
    try {
        return normalizePortalTheme(
            localStorage.getItem(
                PORTAL_THEME_STORAGE_KEY
            )
        );
    } catch (error) {
        console.warn(
            "화면 테마 불러오기 실패:",
            error
        );

        return "light";
    }
}

function applyPortalTheme(theme) {
    const nextTheme = normalizePortalTheme(theme);

    state.theme = nextTheme;

    document.documentElement.dataset.theme =
        nextTheme;

    document.documentElement.style.colorScheme =
        nextTheme;
}

function setPortalTheme(theme) {
    const nextTheme = normalizePortalTheme(theme);

    applyPortalTheme(nextTheme);

    try {
        localStorage.setItem(
            PORTAL_THEME_STORAGE_KEY,
            nextTheme
        );
    } catch (error) {
        console.warn(
            "화면 테마 저장 실패:",
            error
        );
    }
}


// 메인 대시보드 상태
const state = {
    isEditMode: false,
    isSettingsOpen: false,

    // 메인, 캘린더, 지도에서 함께 사용할 화면 테마
    theme: getStoredPortalTheme(),

    // 현재시간 위젯은 상단 헤더에 표시
    headerWidgetId: 9,

    memoText: "",
    calculatorText: "",

    // 실제 로그인 세션 정보 사용
    currentUser:
        null,

    myPage: null,

    loginForm: {
        username: "",
        password: ""
    },

    // 뉴스 상태
    newsItems: [],
    newsLoading: false,
    newsError: "",
    newsCategory: "정치",

    newsCategories: [
        "정치",
        "경제",
        "엔터테인먼트",
        "스포츠",
        "사회",
        "해외"
    ],

    // 날씨 상태
    weather: null,
    weatherLoading: false,
    weatherError: "",

    // 현재시간과 날씨에서 사용하는 요일
    dayNames: [
        "월",
        "화",
        "수",
        "목",
        "금",
        "토",
        "일"
    ],

    sunTime: {
        sunrise: "--:--",
        sunset: "--:--"
    },

    // 오늘 일정 상태
    todayScheduleItems: [],
    todayScheduleLoading: false,
    todayScheduleError: "",

    // 미니 캘린더 상태
    calendarYear:
        new Date().getFullYear(),

    calendarMonth:
        new Date().getMonth(),

    miniCalendarDayMap:
        new Map(),

    miniCalendarLoading: false,
    miniCalendarError: "",

    // 구글 캘린더 연결 상태
    googleCalendarConnected: false,

    // 증권 상태
    stockItems: [
        {
            symbol: "005930",
            name: "삼성전자",
            price: 78400,
            changePrice: 1200,
            changeRate: 1.55,

            points: [
                {
                    time: "09:00",
                    price: 77000
                },
                {
                    time: "10:00",
                    price: 77500
                },
                {
                    time: "11:00",
                    price: 77900
                },
                {
                    time: "12:00",
                    price: 78100
                },
                {
                    time: "13:00",
                    price: 78400
                }
            ]
        },
        {
            symbol: "035420",
            name: "NAVER",
            price: 186500,
            changePrice: -1500,
            changeRate: -0.80,

            points: [
                {
                    time: "09:00",
                    price: 188000
                },
                {
                    time: "10:00",
                    price: 187500
                },
                {
                    time: "11:00",
                    price: 187000
                },
                {
                    time: "12:00",
                    price: 186800
                },
                {
                    time: "13:00",
                    price: 186500
                }
            ]
        },
        {
            symbol: "035720",
            name: "카카오",
            price: 42100,
            changePrice: 600,
            changeRate: 1.45,

            points: [
                {
                    time: "09:00",
                    price: 41400
                },
                {
                    time: "10:00",
                    price: 41700
                },
                {
                    time: "11:00",
                    price: 41900
                },
                {
                    time: "12:00",
                    price: 42000
                },
                {
                    time: "13:00",
                    price: 42100
                }
            ]
        },
        {
            symbol: "000660",
            name: "SK하이닉스",
            price: 238000,
            changePrice: -2500,
            changeRate: -1.04,

            points: [
                {
                    time: "09:00",
                    price: 240500
                },
                {
                    time: "10:00",
                    price: 239800
                },
                {
                    time: "11:00",
                    price: 239000
                },
                {
                    time: "12:00",
                    price: 238600
                },
                {
                    time: "13:00",
                    price: 238000
                }
            ]
        }
    ],

    stockLoading: false,
    stockError: "",
    stockUpdatedAt: null,
    stockSlideIndex: 0,
    stockCharts: [],
    stockSwiper: null,

    // 세계시간 위젯에 표시할 도시
    worldTimeCities: [
        {
            city: "서울",
            country: "대한민국",
            timeZone: "Asia/Seoul",
            zoneLabel: "KST",
            utcOffset: "UTC+9"
        },
        {
            city: "도쿄",
            country: "일본",
            timeZone: "Asia/Tokyo",
            zoneLabel: "JST",
            utcOffset: "UTC+9"
        },
        {
            city: "싱가포르",
            country: "싱가포르",
            timeZone: "Asia/Singapore",
            zoneLabel: "SGT",
            utcOffset: "UTC+8"
        },
        {
            city: "로스앤젤레스",
            country: "미국",
            timeZone: "America/Los_Angeles",
            zoneLabel: "PT",
            utcOffset: "UTC-8"
        },
        {
            city: "뉴욕",
            country: "미국",
            timeZone: "America/New_York",
            zoneLabel: "ET",
            utcOffset: "UTC-5"
        },
        {
            city: "런던",
            country: "영국",
            timeZone: "Europe/London",
            zoneLabel: "UK",
            utcOffset: "UTC+0"
        }
    ],

    // 사진의 위젯 배치 그대로 유지
    widgets: [
        {
            id: 1,
            zone: "main",
            type: "news",
            title: "뉴스",
            visible: true,
            collapsed: false,
            orderNo: 1
        },
        {
            id: 3,
            zone: "main",
            type: "schedule",
            title: "예정 일정",
            visible: true,
            collapsed: false,
            orderNo: 2
        },
        {
            id: 4,
            zone: "main",
            type: "stock",
            title: "국내 증권",
            visible: true,
            collapsed: false,
            orderNo: 3
        },
        {
            id: 5,
            zone: "side",
            type: "weather",
            title: "날씨",
            visible: true,
            collapsed: false,
            orderNo: 1
        },
        {
            id: 6,
            zone: "side",
            type: "calculator",
            title: "계산기",
            visible: true,
            collapsed: false,
            orderNo: 2
        },
        {
            id: 7,
            zone: "main",
            type: "miniCalendar",
            title: "월간 캘린더",
            visible: true,
            collapsed: false,
            orderNo: 3
        },
        {
            id: 8,
            zone: "side",
            type: "memo",
            title: "메모",
            visible: true,
            collapsed: false,
            orderNo: 4
        },
        {
            id: 9,
            zone: "side",
            type: "currentTime",
            title: "현재시간",
            visible: true,
            collapsed: false,
            orderNo: 5,
        },
        {
            id: 10,
            zone: "side",
            type: "worldTime",
            title: "세계시간",
            visible: true,
            collapsed: false,
            orderNo: 3
        }
    ]
};

// 첫 화면을 그리기 전에 저장된 테마를 적용한다.
applyPortalTheme(state.theme);

window.addEventListener(
    "storage",
    function (event) {
        if (event.key !== PORTAL_THEME_STORAGE_KEY) {
            return;
        }

        applyPortalTheme(event.newValue);
    }
);
