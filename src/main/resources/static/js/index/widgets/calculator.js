// 계산기 화면
function renderCalculator() {
    const buttons = [
        {
            label: "C",
            action: "clear-calc",
            className: "is-clear"
        },
        {
            label: "%",
            action: "percent-calc",
            className: "is-function"
        },
        {
            label: "÷",
            action: "append-calc",
            value: "/",
            className: "is-operator"
        },
        {
            label: "⌫",
            action: "backspace-calc",
            className: "is-function",
            ariaLabel: "한 글자 지우기"
        },

        {
            label: "7",
            action: "append-calc",
            value: "7"
        },
        {
            label: "8",
            action: "append-calc",
            value: "8"
        },
        {
            label: "9",
            action: "append-calc",
            value: "9"
        },
        {
            label: "×",
            action: "append-calc",
            value: "*",
            className: "is-operator"
        },

        {
            label: "4",
            action: "append-calc",
            value: "4"
        },
        {
            label: "5",
            action: "append-calc",
            value: "5"
        },
        {
            label: "6",
            action: "append-calc",
            value: "6"
        },
        {
            label: "−",
            action: "append-calc",
            value: "-",
            className: "is-operator"
        },

        {
            label: "1",
            action: "append-calc",
            value: "1"
        },
        {
            label: "2",
            action: "append-calc",
            value: "2"
        },
        {
            label: "3",
            action: "append-calc",
            value: "3"
        },
        {
            label: "+",
            action: "append-calc",
            value: "+",
            className: "is-operator"
        },

        {
            label: "( )",
            action: "parentheses-calc",
            className: "is-function"
        },
        {
            label: "0",
            action: "append-calc",
            value: "0"
        },
        {
            label: ".",
            action: "decimal-calc"
        },
        {
            label: "=",
            action: "calculate",
            className: "is-equals"
        }
    ];

    return `
        <input
            class="calculator-display"
            type="text"
            value="${state.calculatorText}"
            readonly
            aria-label="계산 결과"
        >

        <div class="calculator-buttons">
            ${buttons.map((button) => `
                <button
                    type="button"
                    class="calculator-button ${button.className || ""}"
                    data-action="${button.action}"
                    ${button.value !== undefined
        ? `data-value="${button.value}"`
        : ""}
                    ${button.ariaLabel
        ? `aria-label="${button.ariaLabel}"`
        : ""}
                >
                    ${button.label}
                </button>
            `).join("")}
        </div>
    `;
}

// 계산기 표시창만 갱신
function updateCalculatorDisplay() {
    const display =
        document.querySelector(
            '[data-widget-content="6"] .calculator-display'
        );

    if (!display) {
        return;
    }

    display.value =
        state.calculatorText;
}

// 오류 상태에서 새 입력을 시작
function resetCalculatorError() {
    if (
        state.calculatorText === "Error"
        || state.calculatorText === "Infinity"
        || state.calculatorText === "NaN"
    ) {
        state.calculatorText = "";
    }
}

// 숫자와 연산자 입력
function appendCalculatorValue(value) {
    resetCalculatorError();

    const text =
        state.calculatorText;

    const lastCharacter =
        text.slice(-1);

    const isNumber =
        /^[0-9]$/.test(value);

    const isOperator =
        /^[+\-*/]$/.test(value);

    if (isOperator) {
        // 빈 화면에서는 음수 입력용 -만 허용
        if (text === "") {
            if (value === "-") {
                state.calculatorText =
                    "-";

                updateCalculatorDisplay();
            }

            return;
        }

        // 연산자를 연속으로 누르면 추가 입력을 무시
        if (
            /[+\-*/]$/.test(text)
            || lastCharacter === "."
            || lastCharacter === "("
        ) {
            return;
        }

        state.calculatorText +=
            value;

        updateCalculatorDisplay();

        return;
    }

    if (isNumber) {
        const currentNumber =
            text.split(/[+\-*/()]/).pop();

        // 0000처럼 불필요한 0 연속 입력 차단
        if (
            currentNumber === "0"
            && value === "0"
        ) {
            return;
        }

        // 01을 입력하면 기존 0을 1로 교체
        if (
            currentNumber === "0"
            && value !== "0"
        ) {
            state.calculatorText =
                text.slice(0, -1)
                + value;

            updateCalculatorDisplay();

            return;
        }

        // 닫는 괄호 뒤 숫자는 곱하기로 연결
        if (lastCharacter === ")") {
            state.calculatorText +=
                "*" + value;
        } else {
            state.calculatorText +=
                value;
        }

        updateCalculatorDisplay();
    }
}

// 한 글자 삭제
function removeCalculatorCharacter() {
    resetCalculatorError();

    state.calculatorText =
        state.calculatorText.slice(0, -1);

    updateCalculatorDisplay();
}

// 괄호 자동 입력
function appendCalculatorParenthesis() {
    resetCalculatorError();

    const text =
        state.calculatorText.trim();

    const lastCharacter =
        text.slice(-1);

    const openCount =
        (text.match(/\(/g) || []).length;

    const closeCount =
        (text.match(/\)/g) || []).length;

    // 비어 있으면 여는 괄호 입력
    if (text === "") {
        state.calculatorText = "(";
        updateCalculatorDisplay();
        return;
    }

    // 여는 괄호 연타 차단
    if (lastCharacter === "(") {
        return;
    }

    // 연산자 다음에는 여는 괄호 입력
    if (/[+\-*/]$/.test(text)) {
        state.calculatorText += "(";
        updateCalculatorDisplay();
        return;
    }

    // 열린 괄호가 남아 있고 숫자나 닫는 괄호로 끝나면 닫기
    if (
        openCount > closeCount
        && /[0-9)]$/.test(text)
    ) {
        state.calculatorText += ")";
        updateCalculatorDisplay();
        return;
    }

    // 괄호가 모두 닫힌 뒤 새 괄호를 시작하면 곱하기로 연결
    if (
        openCount === closeCount
        && /[0-9)]$/.test(text)
    ) {
        state.calculatorText += "*(";
        updateCalculatorDisplay();
    }
}

// 소수점 입력
function appendCalculatorDecimal() {
    resetCalculatorError();

    const text =
        state.calculatorText;

    const lastCharacter =
        text.slice(-1);

    // 닫는 괄호 바로 뒤 소수점 입력 차단
    if (lastCharacter === ")") {
        return;
    }

    const currentNumber =
        text.split(/[+\-*/()]/).pop();

    // 같은 숫자에 소수점 중복 입력 차단
    if (currentNumber.includes(".")) {
        return;
    }

    if (
        text === ""
        || /[+\-*/(]$/.test(text)
    ) {
        state.calculatorText +=
            "0.";
    } else {
        state.calculatorText +=
            ".";
    }

    updateCalculatorDisplay();
}

// 퍼센트 계산
function applyCalculatorPercent() {
    resetCalculatorError();

    const text =
        state.calculatorText.trim();

    if (!text) {
        return;
    }

    // 연산자나 소수점 뒤에서는 퍼센트 입력 차단
    if (
        /[+\-*/.(]$/.test(text)
    ) {
        return;
    }

    const operationMatch =
        text.match(
            /^(.*)([+\-*/])(-?\d+(?:\.\d+)?)$/
        );

    if (
        operationMatch
        && operationMatch[1]
    ) {
        const leftExpression =
            operationMatch[1];

        const operator =
            operationMatch[2];

        const percentNumber =
            operationMatch[3];

        if (
            operator === "+"
            || operator === "-"
        ) {
            state.calculatorText =
                `${leftExpression}${operator}`
                + `((${leftExpression})`
                + `*${percentNumber}/100)`;
        } else {
            state.calculatorText =
                `${leftExpression}${operator}`
                + `(${percentNumber}/100)`;
        }

        updateCalculatorDisplay();

        return;
    }

    const numberMatch =
        text.match(
            /-?\d+(?:\.\d+)?$/
        );

    if (!numberMatch) {
        return;
    }

    const numberText =
        numberMatch[0];

    const startIndex =
        numberMatch.index;

    state.calculatorText =
        text.slice(0, startIndex)
        + `(${numberText}/100)`;

    updateCalculatorDisplay();
}

// 계산 수행
function calculate() {
    const expression =
        String(
            state.calculatorText || ""
        ).trim();

    if (!expression) {
        return;
    }

    // 연산자, 소수점, 여는 괄호로 끝나면 계산하지 않음
    if (
        /[+\-*/.(]$/.test(expression)
    ) {
        return;
    }

    const openCount =
        (expression.match(/\(/g) || [])
            .length;

    const closeCount =
        (expression.match(/\)/g) || [])
            .length;

    // 괄호 개수가 맞지 않으면 계산하지 않음
    if (openCount !== closeCount) {
        return;
    }

    // 사용할 수 없는 문자가 있으면 계산하지 않음
    if (
        !/^[0-9+\-*/.() ]+$/.test(
            expression
        )
    ) {
        return;
    }

    // 연산자가 연속으로 들어간 식은 계산하지 않음
    if (
        /[+\-*/]{2,}/.test(
            expression
        )
    ) {
        return;
    }

    try {
        const result =
            Function(
                `"use strict"; return (${expression})`
            )();

        if (
            typeof result !== "number"
            || !Number.isFinite(result)
        ) {
            state.calculatorText =
                "Error";

            updateCalculatorDisplay();

            return;
        }

        state.calculatorText =
            String(
                Number.isInteger(result)
                    ? result
                    : Number(
                        result.toFixed(10)
                    )
            );

        updateCalculatorDisplay();
    } catch (error) {
        // 잘못된 수식은 Error로 바꾸지 않고 입력값 유지
        return;
    }
}