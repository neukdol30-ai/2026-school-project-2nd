//계산기 캘린더
function renderCalculator() {
    const buttons = ["7", "8", "9", "/", "4", "5", "6", "*", "1", "2", "3", "-", "0"];

    return `
        <input
            class="calculator-display"
            type="text"
            value="${state.calculatorText}"
            readonly
        >

        <div class="calculator-buttons">
            ${buttons.map((value) => `
                <button data-action="append-calc" data-value="${value}">
                    ${value === "/" ? "÷" : value === "*" ? "×" : value}
                </button>
            `).join("")}

            <button data-action="clear-calc">C</button>
            <button data-action="calculate">=</button>
            <button data-action="append-calc" data-value="+">+</button>
        </div>
    `;
}

//계산 수행
function calculate() {
    try {
        if (!/^[0-9+\-*/.() ]+$/.test(state.calculatorText)) {
            state.calculatorText = "Error";
            refreshWidgetContent(6);
            return;
        }

        const result = Function(`"use strict"; return (${state.calculatorText})`)();
        state.calculatorText = String(result);
    } catch (error) {
        state.calculatorText = "Error";
    }

    refreshWidgetContent(6);
}