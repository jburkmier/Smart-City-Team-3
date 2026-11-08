// Landing page: checks that the server and database are up before the user logs in.

const statusMessage = document.getElementById('status');
const retryButton = document.getElementById('retry-button');

async function checkServer() {
    retryButton.hidden = true;
    statusMessage.className = 'message message-info';
    statusMessage.textContent = 'Connecting to the server...';

    try {
        await Api.get('/health');
    } catch (e) {
        // Covers both an unreachable server and a 503 when the database is down.
        statusMessage.className = 'message message-error';
        statusMessage.textContent = 'Server unavailable. Start the server and retry.';
        retryButton.hidden = false;
        return;
    }

    if (Session.token()) {
        window.location.href = 'home.html';
        return;
    }
    statusMessage.textContent = 'Welcome to SmartCity Hub. Log in or register to get started.';
}

retryButton.addEventListener('click', checkServer);
checkServer();
