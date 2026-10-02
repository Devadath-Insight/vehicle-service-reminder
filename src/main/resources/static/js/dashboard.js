/*
 * dashboard.js - draws the Chart.js charts on the Dashboard and Reports pages.
 *
 * The numbers are calculated in Java (ReportService) and placed in window.chartData
 * by the page's Thymeleaf inline script. This file only turns them into pictures.
 * A chart is only drawn when its <canvas> exists on the current page.
 */
(function () {
    'use strict';

    const data = window.chartData || {};
    if (typeof window.Chart === 'undefined') {
        return; // Chart.js CDN not reachable (offline) - tables on the page still show the numbers.
    }

    const COLORS = {
        primary: '#1E88E5',
        dark: '#111827',
        success: '#16A34A',
        warning: '#F59E0B',
        danger: '#DC2626',
        muted: '#6B7280',
        grid: '#E5E7EB'
    };
    // Calm palette for categories: brand blue first, then neutrals and the status colours.
    const PALETTE = ['#1E88E5', '#111827', '#16A34A', '#F59E0B', '#64748B', '#DC2626',
                     '#0EA5E9', '#9CA3AF', '#7C3AED', '#14B8A6'];

    const rupee = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
    const plain = new Intl.NumberFormat('en-IN');

    Chart.defaults.font.family = "'Barlow', system-ui, -apple-system, 'Segoe UI', sans-serif";
    Chart.defaults.color = COLORS.muted;
    Chart.defaults.maintainAspectRatio = false;   // the .chart-box wrapper controls the height
    Chart.defaults.responsive = true;
    Chart.defaults.plugins.legend.labels.boxWidth = 12;

    /** Numbers may arrive as numbers or strings (BigDecimal) - make them plain JS numbers. */
    function toNumbers(values) {
        return (values || []).map(function (v) { return Number(v) || 0; });
    }

    function hasData(values) {
        return toNumbers(values).some(function (v) { return v > 0; });
    }

    /** Replace a canvas with a short message when there is nothing to plot. */
    function showEmpty(canvas, message) {
        const box = canvas.parentElement;
        box.innerHTML = '';
        const p = document.createElement('p');
        p.className = 'chart-empty';
        p.textContent = message;
        box.appendChild(p);
    }

    function moneyAxis() {
        return {
            beginAtZero: true,
            grid: { color: COLORS.grid },
            border: { display: false },
            ticks: { callback: function (value) { return rupee.format(value); } }
        };
    }

    /** Tooltip for vertical bar charts whose values are rupees. */
    function moneyTooltip() {
        return {
            callbacks: {
                label: function (ctx) { return ' ' + rupee.format(ctx.parsed.y); }
            }
        };
    }

    /* ---------- Monthly expense (bar) - dashboard and reports ---------- */
    const monthlyCanvas = document.getElementById('monthlyChart');
    if (monthlyCanvas) {
        if (!hasData(data.monthlyValues)) {
            showEmpty(monthlyCanvas, 'No spending recorded in this period yet.');
        } else {
            new Chart(monthlyCanvas, {
                type: 'bar',
                data: {
                    labels: data.monthlyLabels,
                    datasets: [{
                        label: 'Expense',
                        data: toNumbers(data.monthlyValues),
                        backgroundColor: COLORS.primary,
                        borderRadius: 6,
                        maxBarThickness: 42
                    }]
                },
                options: {
                    plugins: { legend: { display: false }, tooltip: moneyTooltip() },
                    scales: { x: { grid: { display: false } }, y: moneyAxis() }
                }
            });
        }
    }

    /* ---------- Expense by category (doughnut) ---------- */
    const categoryCanvas = document.getElementById('categoryChart');
    if (categoryCanvas) {
        if (!hasData(data.categoryValues)) {
            showEmpty(categoryCanvas, 'No expenses recorded yet.');
        } else {
            // Leave out empty categories so the legend stays readable.
            const labels = [];
            const values = [];
            toNumbers(data.categoryValues).forEach(function (v, i) {
                if (v > 0) {
                    labels.push(data.categoryLabels[i]);
                    values.push(v);
                }
            });
            new Chart(categoryCanvas, {
                type: 'doughnut',
                data: {
                    labels: labels,
                    datasets: [{ data: values, backgroundColor: PALETTE, borderWidth: 2, borderColor: '#FFFFFF' }]
                },
                options: {
                    cutout: '62%',
                    plugins: {
                        legend: { position: 'right' },
                        tooltip: {
                            callbacks: {
                                label: function (ctx) { return ' ' + ctx.label + ': ' + rupee.format(ctx.parsed); }
                            }
                        }
                    }
                }
            });
        }
    }

    /* ---------- Services by type (horizontal bar, counts) ---------- */
    const serviceTypeCanvas = document.getElementById('serviceTypeChart');
    if (serviceTypeCanvas) {
        if (!hasData(data.serviceTypeValues)) {
            showEmpty(serviceTypeCanvas, 'No services recorded yet.');
        } else {
            new Chart(serviceTypeCanvas, {
                type: 'bar',
                data: {
                    labels: data.serviceTypeLabels,
                    datasets: [{
                        label: 'Services',
                        data: toNumbers(data.serviceTypeValues),
                        backgroundColor: COLORS.dark,
                        borderRadius: 6,
                        maxBarThickness: 26
                    }]
                },
                options: {
                    indexAxis: 'y',
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                label: function (ctx) {
                                    const n = ctx.parsed.x;
                                    return ' ' + plain.format(n) + (n === 1 ? ' service' : ' services');
                                }
                            }
                        }
                    },
                    scales: {
                        x: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: COLORS.grid }, border: { display: false } },
                        y: { grid: { display: false } }
                    }
                }
            });
        }
    }

    /* ---------- Expense by vehicle (bar) ---------- */
    const vehicleCanvas = document.getElementById('vehicleChart');
    if (vehicleCanvas) {
        if (!hasData(data.vehicleValues)) {
            showEmpty(vehicleCanvas, 'Add vehicles and expenses to compare them here.');
        } else {
            new Chart(vehicleCanvas, {
                type: 'bar',
                data: {
                    labels: data.vehicleLabels,
                    datasets: [{
                        label: 'Total expense',
                        data: toNumbers(data.vehicleValues),
                        backgroundColor: COLORS.success,
                        borderRadius: 6,
                        maxBarThickness: 56
                    }]
                },
                options: {
                    plugins: { legend: { display: false }, tooltip: moneyTooltip() },
                    scales: { x: { grid: { display: false } }, y: moneyAxis() }
                }
            });
        }
    }
})();
