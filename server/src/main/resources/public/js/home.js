// Home page: greets the logged-in user and shows the features for their role.
// The features themselves come in Parts 2 and 3; for now each button says "coming soon".

const ROLE_NAMES = {
    CITIZEN: 'Citizen',
    CITY_ADMINISTRATOR: 'City Administrator',
    DEPARTMENT_MANAGER: 'Department Manager',
    WORKER: 'Worker'
};

const ROLE_FEATURES = {
    CITIZEN: ['Events', 'My Registrations', 'Report an Issue', 'My Requests', 'Transit', 'Notifications'],
    CITY_ADMINISTRATOR: ['Manage Events', 'Review Requests', 'Announcements', 'Flagged Content', 'Engagement Stats'],
    DEPARTMENT_MANAGER: ['Department Requests', 'Assign Tasks', 'Reports'],
    WORKER: ['My Tasks']
};

const notice = document.getElementById('notice');
const error = document.getElementById('error');
const requestsView = document.getElementById('requests-view');
const requestsTitle = document.getElementById('requests-title');
const requestList = document.getElementById('request-list');
const requestForm = document.getElementById('request-form');
const requestMessage = document.getElementById('request-message');

function showUser(user) {
    document.getElementById('welcome').textContent = 'Welcome, ' + user.name;
    document.getElementById('role').textContent = ROLE_NAMES[user.role] || user.role;

    const nav = document.getElementById('nav');
    nav.replaceChildren();
    for (const feature of ROLE_FEATURES[user.role] || []) {
        const button = document.createElement('button');
        button.className = 'button';
        button.type = 'button';
        button.textContent = feature;
        button.addEventListener('click', () => {
            if (feature === 'Events' || feature === 'Manage Events') {
                openEvents(false);
                return;
            }
            if (feature === 'My Registrations') {
                openEvents(true);
                return;
            }
            if (feature === 'Report an Issue') {
                openRequestForm(user);
                return;
            }
            if (isRequestListFeature(user.role, feature)) {
                openRequests(user);
                return;
            }
            notice.textContent = feature + ' is coming soon.';
            notice.hidden = false;
        });
        nav.appendChild(button);
    }
}

function isRequestListFeature(role, feature) {
    return (role === 'CITIZEN' && feature === 'My Requests')
        || (role === 'CITY_ADMINISTRATOR' && feature === 'Review Requests')
        || (role === 'DEPARTMENT_MANAGER' && feature === 'Department Requests');
}

async function openRequests(user) {
    const citizenView = user.role === 'CITIZEN';
    document.getElementById('events-view').hidden = true;
    requestsTitle.textContent = citizenView
        ? 'My Requests'
        : user.role === 'CITY_ADMINISTRATOR' ? 'Review Requests' : 'Department Requests';
    requestsView.hidden = false;
    requestForm.hidden = true;
    requestList.hidden = false;
    requestMessage.hidden = true;
    notice.hidden = true;
    error.hidden = true;
    requestList.replaceChildren();

    try {
        const requests = await Api.get(citizenView ? '/me/requests' : '/requests');
        if (!requests.length) {
            const empty = document.createElement('p');
            empty.textContent = citizenView
                ? 'You have not submitted any requests.'
                : 'No service requests are available.';
            requestList.appendChild(empty);
            return;
        }
        for (const request of requests) {
            requestList.appendChild(createRequestCard(request, user.role === 'CITY_ADMINISTRATOR'));
        }
    } catch (requestError) {
        error.textContent = requestError.message;
        error.hidden = false;
    }
}

function openRequestForm(user) {
    if (user.role !== 'CITIZEN') {
        return;
    }
    document.getElementById('events-view').hidden = true;
    requestsTitle.textContent = 'Report an Issue';
    requestsView.hidden = false;
    requestList.hidden = true;
    requestForm.hidden = false;
    requestForm.reset();
    requestMessage.hidden = true;
    notice.hidden = true;
    error.hidden = true;
}

requestForm.addEventListener('submit', async event => {
    event.preventDefault();
    requestMessage.hidden = true;
    error.hidden = true;
    const form = new FormData(requestForm);
    const submitButton = requestForm.querySelector('button[type="submit"]');
    submitButton.disabled = true;
    try {
        await Api.post('/requests', {
            type: form.get('type'),
            description: form.get('description'),
            location: form.get('location')
        });
        requestForm.reset();
        await openRequests(Session.user());
        requestMessage.className = 'message message-success';
        requestMessage.textContent = 'Service request submitted.';
        requestMessage.hidden = false;
    } catch (requestError) {
        requestMessage.className = 'message message-error';
        requestMessage.textContent = requestError.message;
        requestMessage.hidden = false;
    } finally {
        submitButton.disabled = false;
    }
});

document.getElementById('cancel-request').addEventListener('click', () => {
    openRequests(Session.user());
});

document.getElementById('close-requests').addEventListener('click', () => {
    requestsView.hidden = true;
});

function createRequestCard(request, canReview) {
    const card = document.createElement('article');
    card.className = 'request-card';

    const title = document.createElement('h3');
    title.textContent = 'Request #' + request.id + ' · ' + request.type.replaceAll('_', ' ');
    card.appendChild(title);
    appendRequestText(card, request.description);
    appendRequestText(card, request.location);
    appendRequestText(card, 'Status: ' + request.status.replaceAll('_', ' '));
    appendRequestText(card, 'Submitted: ' + new Date(request.submittedAt).toLocaleString());
    if (request.citizenName) {
        appendRequestText(card, 'Submitted by: ' + request.citizenName);
    }
    if (request.departmentName) {
        appendRequestText(card, 'Department: ' + request.departmentName);
    }
    if (request.assignedWorkerName) {
        appendRequestText(card, 'Assigned to: ' + request.assignedWorkerName);
    }
    if (request.rejectionReason) {
        appendRequestText(card, 'Rejection reason: ' + request.rejectionReason);
    }
    if (request.resolutionNotes) {
        appendRequestText(card, 'Resolution: ' + request.resolutionNotes);
    }
    if (canReview && request.status === 'SUBMITTED') {
        const reviewButton = document.createElement('button');
        reviewButton.className = 'button';
        reviewButton.type = 'button';
        reviewButton.textContent = 'Review request';
        reviewButton.addEventListener('click', async () => {
            reviewButton.disabled = true;
            error.hidden = true;
            try {
                await Api.post('/requests/' + request.id + '/review');
                await openRequests(Session.user());
                requestMessage.className = 'message message-success';
                requestMessage.textContent = 'Request moved to Under Review.';
                requestMessage.hidden = false;
            } catch (requestError) {
                error.textContent = requestError.message;
                error.hidden = false;
                reviewButton.disabled = false;
            }
        });
        card.appendChild(reviewButton);
    }
    return card;
}

function appendRequestText(container, text) {
    const line = document.createElement('p');
    line.textContent = text;
    container.appendChild(line);
}

document.getElementById('close-requests').addEventListener('click', () => {
    requestsView.hidden = true;
    requestForm.hidden = true;
    requestList.hidden = false;
});

async function loadUser() {
    try {
        // Asks the server, so a session that ended (logout, server restart) is noticed right away.
        // A 401 here makes api.js clear the session and go to login.html.
        const user = await Api.get('/me');
        Session.save(Session.token(), user);
        showUser(user);
    } catch (e) {
        if (e.status !== 401) {
            error.textContent = e.message;
            error.hidden = false;
        }
    }
}

document.getElementById('logout-button').addEventListener('click', async () => {
    try {
        await Api.post('/auth/logout');
    } catch (e) {
        // Logging out locally is enough if the server can't be reached.
    }
    Session.clear();
    window.location.href = 'login.html';
});

if (!Session.token()) {
    window.location.href = 'login.html';
} else {
    if (Session.user()) {
        showUser(Session.user());
    }
    loadUser();
}
