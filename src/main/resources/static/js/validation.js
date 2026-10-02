/*
 * validation.js - friendly client-side checks and small form conveniences.
 *
 * IMPORTANT: this only improves the experience. The real validation is done on the server
 * with Jakarta Bean Validation (@NotBlank, @Email, @Positive...) and in the controllers,
 * so the app is still safe if JavaScript is turned off.
 */
(function () {
    'use strict';

    /* ---------- Helpers ---------- */

    function todayIso() {
        const now = new Date();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        return now.getFullYear() + '-' + month + '-' + day;
    }

    /** Human friendly message for a field that fails the browser's built-in checks. */
    function messageFor(field) {
        const v = field.validity;
        const label = field.labels && field.labels[0]
            ? field.labels[0].textContent.replace('*', '').trim()
            : 'This field';

        if (v.customError) {
            return field.validationMessage;
        }
        if (v.valueMissing) {
            return field.tagName === 'SELECT' ? 'Please choose an option' : label + ' is required';
        }
        if (v.typeMismatch && field.type === 'email') {
            return 'Enter a valid email address';
        }
        if (v.patternMismatch) {
            if (field.type === 'tel') {
                return 'Enter 10 to 15 digits, optionally starting with +';
            }
            return 'Use only letters, numbers, spaces and hyphens';
        }
        if (v.tooShort) {
            return label + ' must be at least ' + field.minLength + ' characters';
        }
        if (v.tooLong) {
            return label + ' must be at most ' + field.maxLength + ' characters';
        }
        if (v.rangeUnderflow) {
            return field.type === 'date' ? 'Date is too early' : 'Value must be at least ' + field.min;
        }
        if (v.rangeOverflow) {
            return field.type === 'date' ? 'Date cannot be in the future' : 'Value must be at most ' + field.max;
        }
        if (v.stepMismatch) {
            return field.step === '1' ? 'Enter a whole number' : 'Use at most 2 decimal places';
        }
        if (v.badInput) {
            return 'Enter a valid value';
        }
        return field.validationMessage;
    }

    /** Make sure the field has an .invalid-feedback element and put our message in it. */
    function showMessage(field) {
        const container = field.closest('.input-group') || field.parentElement;
        let feedback = container.querySelector('.invalid-feedback');
        if (!feedback) {
            feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            container.appendChild(feedback);
        }
        feedback.textContent = messageFor(field);
    }

    /* ---------- "Confirm password" style matching: data-match="#otherField" ---------- */

    function checkMatch(field) {
        const other = document.querySelector(field.dataset.match);
        if (!other) {
            return;
        }
        if (field.value && other.value !== field.value) {
            field.setCustomValidity('Passwords do not match');
        } else {
            field.setCustomValidity('');
        }
    }

    document.querySelectorAll('[data-match]').forEach(function (field) {
        const other = document.querySelector(field.dataset.match);
        field.addEventListener('input', function () { checkMatch(field); });
        if (other) {
            other.addEventListener('input', function () { checkMatch(field); });
        }
    });

    /* ---------- Bootstrap validation styling for .needs-validation forms ---------- */

    document.querySelectorAll('form.needs-validation').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            form.querySelectorAll('[data-match]').forEach(checkMatch);

            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
                form.querySelectorAll('input, select, textarea').forEach(function (field) {
                    if (!field.checkValidity()) {
                        showMessage(field);
                    }
                });
                const firstInvalid = form.querySelector(':invalid');
                if (firstInvalid) {
                    firstInvalid.focus();
                }
            } else {
                // Prevent double submissions while the server works.
                const submit = form.querySelector('button[type="submit"]');
                if (submit) {
                    submit.disabled = true;
                }
            }
            form.classList.add('was-validated');
        });

        // Once the user edits a field that the SERVER marked invalid, clear the red state.
        form.querySelectorAll('.is-invalid').forEach(function (field) {
            const clear = function () { field.classList.remove('is-invalid'); };
            field.addEventListener('input', clear, { once: true });
            field.addEventListener('change', clear, { once: true });
        });

        // Keep messages up to date while typing after the first failed submit.
        form.addEventListener('input', function (event) {
            if (form.classList.contains('was-validated') && event.target.matches('input, select, textarea')) {
                if (!event.target.checkValidity()) {
                    showMessage(event.target);
                }
            }
        });
    });

    /* ---------- Registration numbers in capitals: data-uppercase ---------- */

    document.querySelectorAll('[data-uppercase]').forEach(function (field) {
        field.addEventListener('blur', function () {
            field.value = field.value.toUpperCase().replace(/\s+/g, ' ').trim();
        });
    });

    /* ---------- Dates that cannot be in the future: data-max-today ---------- */

    document.querySelectorAll('input[type="date"][data-max-today]').forEach(function (field) {
        field.max = todayIso();
    });

    /* ---------- Service form: fill odometer from the selected vehicle ---------- */

    const mileageSource = document.querySelector('[data-mileage-source]');
    const mileageTarget = document.querySelector('[data-mileage-target]');
    if (mileageSource && mileageTarget) {
        let autoFilled = mileageTarget.value === '';
        mileageTarget.addEventListener('input', function () { autoFilled = false; });

        mileageSource.addEventListener('change', function () {
            const option = mileageSource.options[mileageSource.selectedIndex];
            const mileage = option ? option.dataset.mileage : '';
            if (mileage && (autoFilled || mileageTarget.value === '')) {
                mileageTarget.value = mileage;
                autoFilled = true;
            }
        });
    }

    /* ---------- Reminder form: suggest a title and show the current odometer ---------- */

    const titleSource = document.querySelector('[data-title-source]');
    const titleTarget = document.querySelector('[data-title-target]');
    if (titleSource && titleTarget) {
        let lastSuggestion = '';
        titleSource.addEventListener('change', function () {
            const option = titleSource.options[titleSource.selectedIndex];
            if (!option || !option.value) {
                return;
            }
            // Only replace the title if the user has not typed their own.
            if (titleTarget.value === '' || titleTarget.value === lastSuggestion) {
                titleTarget.value = option.textContent.trim();
                lastSuggestion = titleTarget.value;
            }
        });
    }

    const dueMileageHint = document.getElementById('dueMileageHint');
    const reminderVehicle = dueMileageHint ? document.getElementById('vehicleId') : null;
    if (dueMileageHint && reminderVehicle) {
        const updateHint = function () {
            const option = reminderVehicle.options[reminderVehicle.selectedIndex];
            const mileage = option ? option.dataset.mileage : '';
            dueMileageHint.textContent = mileage
                ? 'Current odometer: ' + Number(mileage).toLocaleString('en-IN') + ' km'
                : '';
        };
        reminderVehicle.addEventListener('change', updateHint);
        updateHint();
    }
})();
