/*
 * main.js - small UI helpers used on every page.
 * No business rules live here; Java (service/util layer) owns all of those.
 *
 *  1. Sidebar toggle on tablets/phones (off-canvas drawer)
 *  2. Delete confirmation using a Bootstrap modal (any <form data-confirm="...">)
 *  3. Auto-hide success alerts after a few seconds
 *  4. Filter forms that submit when a dropdown changes (<form data-autosubmit>)
 */
(function () {
    'use strict';

    /* ---------- 1. Sidebar toggle ---------- */
    const body = document.body;

    function openSidebar() {
        body.classList.add('sidebar-open');
    }

    function closeSidebar() {
        body.classList.remove('sidebar-open');
    }

    document.querySelectorAll('[data-sidebar-toggle]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            if (body.classList.contains('sidebar-open')) {
                closeSidebar();
            } else {
                openSidebar();
            }
        });
    });

    document.querySelectorAll('[data-sidebar-close]').forEach(function (el) {
        el.addEventListener('click', closeSidebar);
    });

    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') {
            closeSidebar();
        }
    });

    // If the window is resized to desktop width, make sure the drawer state is reset.
    window.addEventListener('resize', function () {
        if (window.innerWidth >= 992) {
            closeSidebar();
        }
    });

    /* ---------- 2. Delete confirmation ---------- */
    const modalEl = document.getElementById('confirmModal');
    let pendingForm = null;

    if (modalEl && window.bootstrap) {
        const modal = new bootstrap.Modal(modalEl);
        const titleEl = document.getElementById('confirmModalTitle');
        const messageEl = document.getElementById('confirmModalMessage');
        const okBtn = document.getElementById('confirmModalOk');

        document.querySelectorAll('form[data-confirm]').forEach(function (form) {
            form.addEventListener('submit', function (event) {
                if (form.dataset.confirmed === 'true') {
                    return; // already confirmed - let the browser submit it
                }
                event.preventDefault();
                pendingForm = form;
                titleEl.textContent = form.dataset.confirmTitle || 'Are you sure?';
                messageEl.textContent = form.dataset.confirm || 'This cannot be undone.';
                modal.show();
            });
        });

        okBtn.addEventListener('click', function () {
            if (pendingForm) {
                pendingForm.dataset.confirmed = 'true';
                okBtn.disabled = true;
                pendingForm.submit();
            }
        });

        modalEl.addEventListener('hidden.bs.modal', function () {
            pendingForm = null;
            okBtn.disabled = false;
        });
    } else {
        // Fallback when Bootstrap JS could not load (e.g. offline): use the browser dialog.
        document.querySelectorAll('form[data-confirm]').forEach(function (form) {
            form.addEventListener('submit', function (event) {
                if (!window.confirm(form.dataset.confirm || 'Are you sure?')) {
                    event.preventDefault();
                }
            });
        });
    }

    /* ---------- 3. Auto-dismiss success alerts ---------- */
    document.querySelectorAll('[data-autodismiss]').forEach(function (alertEl) {
        window.setTimeout(function () {
            if (window.bootstrap && bootstrap.Alert) {
                bootstrap.Alert.getOrCreateInstance(alertEl).close();
            } else {
                alertEl.remove();
            }
        }, 5000);
    });

    /* ---------- 4. Auto-submit filter forms when a dropdown changes ---------- */
    document.querySelectorAll('form[data-autosubmit]').forEach(function (form) {
        form.querySelectorAll('select').forEach(function (select) {
            select.addEventListener('change', function () {
                form.submit();
            });
        });
    });
})();
