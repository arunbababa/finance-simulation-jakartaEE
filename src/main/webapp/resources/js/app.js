/**
 * ローン返済シミュレーター - フロントエンドアプリケーション
 */

// API Base URL
const API_BASE = '/loan-simulator/api';

// ========================================
// 初期化
// ========================================

document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    loadRepaymentTypes();
    loadDemoCardInfo();
    initEventListeners();
    updateLegalRateHint();
});

// ========================================
// タブ機能
// ========================================

function initTabs() {
    const tabs = document.querySelectorAll('.tab');
    const contents = document.querySelectorAll('.tab-content');

    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const targetId = tab.dataset.tab;

            tabs.forEach(t => t.classList.remove('active'));
            contents.forEach(c => c.classList.remove('active'));

            tab.classList.add('active');
            document.getElementById(targetId).classList.add('active');
        });
    });
}

// ========================================
// イベントリスナー
// ========================================

function initEventListeners() {
    // シミュレーションフォーム
    document.getElementById('simulationForm').addEventListener('submit', handleSimulation);

    // 返済方式変更
    document.getElementById('repaymentType').addEventListener('change', handleRepaymentTypeChange);

    // 借入金額変更（法的金利表示更新）
    document.getElementById('principal').addEventListener('change', updateLegalRateHint);

    // 比較フォーム
    document.getElementById('compareForm').addEventListener('submit', handleCompare);

    // カード検証フォーム
    document.getElementById('cardValidateForm').addEventListener('submit', handleCardValidation);
}

// ========================================
// 返済方式の読み込み
// ========================================

async function loadRepaymentTypes() {
    try {
        const response = await fetch(`${API_BASE}/simulation/repayment-types`);
        const types = await response.json();

        const select = document.getElementById('repaymentType');
        select.innerHTML = '<option value="">返済方式を選択</option>';

        types.forEach(type => {
            const option = document.createElement('option');
            option.value = type.code;
            option.textContent = type.displayName;
            option.dataset.description = type.description;
            select.appendChild(option);
        });
    } catch (error) {
        console.error('返済方式の読み込みに失敗:', error);
        // フォールバック
        const select = document.getElementById('repaymentType');
        select.innerHTML = `
            <option value="">返済方式を選択</option>
            <option value="EQUAL_TOTAL_PAYMENT">元利均等返済</option>
            <option value="EQUAL_PRINCIPAL_PAYMENT">元金均等返済</option>
            <option value="REVOLVING_FIXED_AMOUNT">リボ払い（定額方式）</option>
            <option value="REVOLVING_BALANCE_SLIDE">リボ払い（残高スライド方式）</option>
            <option value="REVOLVING_FIXED_RATE">リボ払い（定率方式）</option>
            <option value="INSTALLMENT">分割払い</option>
            <option value="LUMP_SUM">一括払い</option>
            <option value="CASHING_LUMP_SUM">キャッシング一括</option>
            <option value="CASHING_REVOLVING">キャッシングリボ</option>
        `;
    }
}

// ========================================
// 返済方式変更ハンドラ
// ========================================

function handleRepaymentTypeChange(e) {
    const type = e.target.value;
    const option = e.target.options[e.target.selectedIndex];
    const description = option.dataset.description || '';

    document.getElementById('repaymentDescription').textContent = description;

    // 動的パラメータの表示/非表示
    const groups = {
        termMonthsGroup: ['EQUAL_TOTAL_PAYMENT', 'EQUAL_PRINCIPAL_PAYMENT'],
        monthlyPaymentGroup: ['REVOLVING_FIXED_AMOUNT', 'CASHING_REVOLVING'],
        repaymentRateGroup: ['REVOLVING_FIXED_RATE'],
        balanceSlideTypeGroup: ['REVOLVING_BALANCE_SLIDE'],
        installmentCountGroup: ['INSTALLMENT']
    };

    Object.entries(groups).forEach(([groupId, types]) => {
        const group = document.getElementById(groupId);
        group.style.display = types.includes(type) ? 'block' : 'none';
    });
}

// ========================================
// 法的金利のヒント更新
// ========================================

async function updateLegalRateHint() {
    const principal = document.getElementById('principal').value;
    const hint = document.getElementById('legalRateHint');

    if (!principal || principal <= 0) {
        hint.textContent = '';
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/simulation/legal-rate?principal=${principal}`);
        const data = await response.json();
        hint.textContent = `利息制限法上限: 年${data.maxAnnualRatePercent}%`;
    } catch (error) {
        // ローカル計算にフォールバック
        const p = parseFloat(principal);
        let maxRate;
        if (p < 100000) maxRate = 20;
        else if (p < 1000000) maxRate = 18;
        else maxRate = 15;
        hint.textContent = `利息制限法上限: 年${maxRate}%`;
    }
}

// ========================================
// シミュレーション実行
// ========================================

async function handleSimulation(e) {
    e.preventDefault();

    const form = e.target;
    const submitBtn = form.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = '計算中...';

    const formData = new FormData(form);
    const repaymentType = formData.get('repaymentType');

    const request = {
        principal: parseFloat(formData.get('principal')),
        annualRate: parseFloat(formData.get('annualRate')) / 100,
        repaymentType: repaymentType
    };

    // 返済方式に応じたパラメータ追加
    switch (repaymentType) {
        case 'EQUAL_TOTAL_PAYMENT':
        case 'EQUAL_PRINCIPAL_PAYMENT':
            request.termMonths = parseInt(formData.get('termMonths'));
            break;
        case 'REVOLVING_FIXED_AMOUNT':
        case 'CASHING_REVOLVING':
            request.monthlyPayment = parseFloat(formData.get('monthlyPayment'));
            break;
        case 'REVOLVING_FIXED_RATE':
            request.repaymentRate = parseFloat(formData.get('repaymentRate')) / 100;
            break;
        case 'REVOLVING_BALANCE_SLIDE':
            request.balanceSlideType = formData.get('balanceSlideType');
            break;
        case 'INSTALLMENT':
            request.installmentCount = parseInt(formData.get('installmentCount'));
            break;
    }

    try {
        const response = await fetch(`${API_BASE}/simulation/calculate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(request)
        });

        const result = await response.json();

        if (result.error) {
            alert('エラー: ' + result.message);
            return;
        }

        displayResult(result);
    } catch (error) {
        console.error('シミュレーションエラー:', error);
        alert('シミュレーション中にエラーが発生しました');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'シミュレーション実行';
    }
}

// ========================================
// 結果表示
// ========================================

function displayResult(result) {
    const container = document.getElementById('result');
    container.style.display = 'block';

    // 警告表示
    const warning = document.getElementById('rateWarning');
    if (result.rateWarning) {
        warning.textContent = result.rateWarning;
        warning.style.display = 'block';
    } else {
        warning.style.display = 'none';
    }

    // サマリー
    document.getElementById('resultPrincipal').textContent = formatCurrency(result.principal);
    document.getElementById('resultRate').textContent = formatPercent(result.annualRate);
    document.getElementById('resultTotalPayment').textContent = formatCurrency(result.totalPayment);
    document.getElementById('resultTotalInterest').textContent = formatCurrency(result.totalInterest);
    document.getElementById('resultTerm').textContent = result.termDisplay;
    document.getElementById('resultMonthlyPayment').textContent = formatCurrency(result.monthlyPayment);

    // 月次明細テーブル
    const tbody = document.querySelector('#monthlyDetailsTable tbody');
    tbody.innerHTML = '';

    const details = result.monthlyDetails || [];
    const maxRows = 60; // 最大表示行数

    details.slice(0, maxRows).forEach(detail => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td style="text-align: center">${detail.paymentNumber}</td>
            <td style="text-align: center">${detail.paymentMonth}</td>
            <td>${formatCurrency(detail.totalPayment)}</td>
            <td>${formatCurrency(detail.principalPayment)}</td>
            <td>${formatCurrency(detail.interestPayment)}</td>
            <td>${formatCurrency(detail.balanceAfter)}</td>
        `;
        tbody.appendChild(row);
    });

    if (details.length > maxRows) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td colspan="6" style="text-align: center; color: var(--text-secondary)">
                ... 以下 ${details.length - maxRows} 件省略 ...
            </td>
        `;
        tbody.appendChild(row);
    }

    // 適用法令
    const lawsList = document.getElementById('applicableLaws');
    lawsList.innerHTML = '';
    (result.applicableLaws || []).forEach(law => {
        const li = document.createElement('li');
        li.textContent = law;
        lawsList.appendChild(li);
    });

    // スクロール
    container.scrollIntoView({ behavior: 'smooth' });
}

// ========================================
// 比較機能
// ========================================

async function handleCompare(e) {
    e.preventDefault();

    const principal = document.getElementById('comparePrincipal').value;
    const annualRate = parseFloat(document.getElementById('compareRate').value) / 100;
    const termMonths = document.getElementById('compareTermMonths').value;
    const monthlyPayment = document.getElementById('compareMonthlyPayment').value;

    try {
        const params = new URLSearchParams({
            principal,
            annualRate,
            termMonths,
            monthlyPayment
        });

        const response = await fetch(`${API_BASE}/simulation/compare?${params}`);
        const data = await response.json();

        displayCompareResult(data);
    } catch (error) {
        console.error('比較エラー:', error);
        alert('比較中にエラーが発生しました');
    }
}

function displayCompareResult(data) {
    const container = document.getElementById('compareResult');
    container.style.display = 'block';

    const tbody = document.querySelector('#compareTable tbody');
    tbody.innerHTML = '';

    (data.results || []).forEach(result => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td style="text-align: left">${result.repaymentTypeName}${result.installmentCount ? ` (${result.installmentCount}回)` : ''}</td>
            <td>${formatCurrency(result.totalPayment)}</td>
            <td style="color: var(--danger-color)">${formatCurrency(result.totalInterest)}</td>
            <td>${result.termDisplay}</td>
            <td>${formatCurrency(result.monthlyPayment)}</td>
        `;
        tbody.appendChild(row);
    });

    container.scrollIntoView({ behavior: 'smooth' });
}

// ========================================
// デモカード情報
// ========================================

async function loadDemoCardInfo() {
    try {
        const response = await fetch(`${API_BASE}/card/demo`);
        const data = await response.json();

        const container = document.getElementById('demoCardInfo');
        const card = data.cardInfo;

        container.innerHTML = `
            <div style="margin-bottom: 1rem; padding: 0.5rem; background: #fef3c7; border-radius: 0.25rem; font-size: 0.75rem;">
                ⚠️ ${data.disclaimer}
            </div>
            <table style="width: 100%; font-size: 0.875rem;">
                <tr><td style="width: 40%">カード番号</td><td><strong>${card.maskedCardNumber}</strong></td></tr>
                <tr><td>ブランド</td><td>${card.cardBrand}</td></tr>
                <tr><td>名義人</td><td>${card.cardHolderName}</td></tr>
                <tr><td>有効期限</td><td>${card.expiryDate}</td></tr>
                <tr><td>ショッピング枠</td><td>${formatCurrency(card.shoppingLimit)}</td></tr>
                <tr><td>利用可能額</td><td>${formatCurrency(card.availableShoppingLimit)}</td></tr>
                <tr><td>キャッシング枠</td><td>${formatCurrency(card.cashingLimit)}</td></tr>
                <tr><td>リボ残高</td><td>${formatCurrency(card.revolvingBalance)}</td></tr>
                <tr><td>リボ金利</td><td>${formatPercent(card.shoppingRevolvingRate)}</td></tr>
                <tr><td>キャッシング金利</td><td>${formatPercent(card.cashingRate)}</td></tr>
                <tr><td>締め日/支払日</td><td>毎月${card.closingDay}日締め / ${card.paymentDay}日払い</td></tr>
            </table>
        `;
    } catch (error) {
        console.error('デモカード情報の読み込みに失敗:', error);
        document.getElementById('demoCardInfo').innerHTML = '<p>デモ情報を読み込めませんでした</p>';
    }
}

// ========================================
// カード番号検証
// ========================================

async function handleCardValidation(e) {
    e.preventDefault();

    const cardNumber = document.getElementById('cardNumber').value;
    const resultDiv = document.getElementById('cardValidateResult');

    if (!cardNumber) {
        resultDiv.textContent = '';
        resultDiv.className = '';
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/card/validate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ cardNumber })
        });

        const result = await response.json();

        resultDiv.innerHTML = `
            <strong>${result.valid ? '✓ 有効' : '✗ 無効'}</strong><br>
            ブランド: ${result.brand}<br>
            マスク番号: ${result.maskedNumber}<br>
            ${result.message}
        `;
        resultDiv.className = result.valid ? 'valid' : 'invalid';
    } catch (error) {
        // ローカルで検証
        const digits = cardNumber.replace(/[^0-9]/g, '');
        const isValid = validateLuhn(digits);

        resultDiv.innerHTML = `
            <strong>${isValid ? '✓ 有効' : '✗ 無効'}</strong><br>
            （ローカル検証）
        `;
        resultDiv.className = isValid ? 'valid' : 'invalid';
    }
}

// ========================================
// Luhnアルゴリズム（ローカル）
// ========================================

function validateLuhn(digits) {
    if (digits.length < 13 || digits.length > 19) {
        return false;
    }

    let sum = 0;
    let alternate = false;

    for (let i = digits.length - 1; i >= 0; i--) {
        let n = parseInt(digits.charAt(i), 10);
        if (alternate) {
            n *= 2;
            if (n > 9) {
                n = (n % 10) + 1;
            }
        }
        sum += n;
        alternate = !alternate;
    }

    return (sum % 10 === 0);
}

// ========================================
// ユーティリティ関数
// ========================================

function formatCurrency(value) {
    if (value === null || value === undefined) return '-';
    return new Intl.NumberFormat('ja-JP', {
        style: 'currency',
        currency: 'JPY',
        minimumFractionDigits: 0
    }).format(value);
}

function formatPercent(value) {
    if (value === null || value === undefined) return '-';
    return (value * 100).toFixed(2) + '%';
}
