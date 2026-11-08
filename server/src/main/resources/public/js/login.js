// Login page: sends the email and password, saves the session, and opens the home page.

if (Session.token()) {
    window.location.href = 'home.html';
}

const form = document.getElementById('login-form');
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const loginButton = document.getElementById('login-button');
const notice = document.getElementById('notice');
const error = document.getElementById('error');

// After registering, register.js sends the user here with ?email=...&created=1.
const params = new URLSearchParams(window.location.search);
if (params.get('email')) {
    emailInput.value = params.get('email');
}
if (params.get('created') === '1') {
    notice.textContent = 'Account created. Please log in.';
    notice.hidden = false;
}
(emailInput.value ? passwordInput : emailInput).focus();

function showError(message) {
    error.textContent = message;
    error.hidden = false;
}

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    error.hidden = true;

    const email = emailInput.value.trim();
    const password = passwordInput.value;
    if (!email || !password) {
        showError('Enter your email and password.');
        return;
    }

    loginButton.disabled = true;
    try {
        const response = await Api.post('/auth/login', { email, password });
        Session.save(response.token, response.user);
        window.location.href = 'home.html';
    } catch (e) {
        showError(e.message);
        passwordInput.value = '';
        passwordInput.focus();
    } finally {
        loginButton.disabled = false;
    }
});
