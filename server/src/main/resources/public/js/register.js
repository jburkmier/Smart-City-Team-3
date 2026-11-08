// Register page: checks the form, creates a citizen account, then sends the user to log in.
// The server repeats every check; these only save a round trip for obvious mistakes.

const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
const FIELD_LABELS = { name: 'Name', email: 'Email', password: 'Password', address: 'Address', phone: 'Phone' };

const form = document.getElementById('register-form');
const registerButton = document.getElementById('register-button');
const error = document.getElementById('error');

function value(id) {
    return document.getElementById(id).value;
}

function showError(message) {
    error.textContent = message;
    error.hidden = false;
    error.scrollIntoView({ block: 'nearest' });
}

/** The first problem with the form, or null if it looks valid. */
function checkForm(request, confirmPassword) {
    if (!request.name) {
        return 'Name is required.';
    }
    if (!EMAIL_PATTERN.test(request.email)) {
        return 'Enter a valid email address.';
    }
    if (request.password.length < 8) {
        return 'Password must be at least 8 characters.';
    }
    if (request.password !== confirmPassword) {
        return 'Passwords do not match.';
    }
    return null;
}

/** The server's message, plus the per-field details for a VALIDATION_FAILED error. */
function describe(e) {
    const details = Object.entries(e.fields).map(([field, text]) => (FIELD_LABELS[field] || field) + ': ' + text);
    return details.length ? e.message + ' ' + details.join('. ') + '.' : e.message;
}

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    error.hidden = true;

    const request = {
        name: value('name').trim(),
        email: value('email').trim(),
        password: value('password'),
        address: value('address').trim() || null,
        phone: value('phone').trim() || null
    };
    const problem = checkForm(request, value('confirm-password'));
    if (problem) {
        showError(problem);
        return;
    }

    registerButton.disabled = true;
    try {
        const user = await Api.post('/auth/register', request);
        window.location.href = 'login.html?email=' + encodeURIComponent(user.email) + '&created=1';
    } catch (e) {
        showError(describe(e));
        registerButton.disabled = false;
    }
});
