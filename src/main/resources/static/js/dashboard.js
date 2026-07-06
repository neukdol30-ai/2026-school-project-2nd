const app = Vue.createApp({
    data() {
        return {
            title: "대시보드",
            message: "Vue 시도중"
        };
    },
    methods: {
        changeMessage() {
            this.message = "외부 JS 파일에서 변경되었습니다.";
        }
    }
});

app.mount("#app");