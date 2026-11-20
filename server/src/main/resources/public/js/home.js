// Home page: greets the logged-in user and shows the features for their role.
// The features themselves come in Parts 2 and 3; for now each button says "coming soon".

const ROLE_NAMES = {
    CITIZEN: 'Citizen',
    CITY_ADMINISTRATOR: 'City Administrator',
    DEPARTMENT_MANAGER: 'Department Manager',
    WORKER: 'Worker'
};

const ROLE_FEATURES = {
    CITIZEN: ['Events', 'My Registrations', 'Report an Issue', 'My Requests', 'Transit', 'Notifications',
        'Announcements'],
    CITY_ADMINISTRATOR: ['Manage Events', 'Review Requests', 'Announcements', 'Notifications',
        'Flagged Content', 'Engagement Stats'],
    DEPARTMENT_MANAGER: ['Department Requests', 'Assign Tasks', 'Reports', 'Announcements'],
    WORKER: ['My Tasks', 'Notifications', 'Announcements']
};

const notice = document.getElementById('notice');
const error = document.getElementById('error');
const requestsView = document.getElementById('requests-view');
const requestsTitle = document.getElementById('requests-title');
const requestList = document.getElementById('request-list');
const requestForm = document.getElementById('request-form');
const requestMessage = document.getElementById('request-message');
const requestFilters = document.getElementById('request-filters');

let loadedRequestEntries = [];
let renderRequestEntry = null;

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
        button.dataset.feature = feature;
        button.addEventListener('click', () => {
            document.getElementById('notifications-view').hidden = true;
            document.getElementById('announcements-view').hidden = true;
            document.getElementById('flags-view').hidden = true;
            document.getElementById('reports-view').hidden = true;
            if (feature === 'Notifications') {
                openNotifications();
                return;
            }
            if (feature === 'Announcements') {
                openAnnouncements();
                return;
            }
            if (feature === 'Flagged Content') {
                openFlaggedContent();
                return;
            }
            if (feature === 'Engagement Stats' || feature === 'Reports') {
                openReports(feature, user);
                return;
            }
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
            if (feature === 'Assign Tasks') {
                openRequests(user, true);
                return;
            }
            if (feature === 'My Tasks') {
                openMyTasks(user);
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
    // Rebuilding the nav resets the button labels, so show the unread count again.
    // notifications.js loads after this file, so the function may not exist yet on the first call.
    if (typeof renderNotificationCount === 'function') {
        renderNotificationCount();
    }
}

function isRequestListFeature(role, feature) {
    return (role === 'CITIZEN' && feature === 'My Requests')
        || (role === 'CITY_ADMINISTRATOR' && feature === 'Review Requests')
        || (role === 'DEPARTMENT_MANAGER' && feature === 'Department Requests');
}

async function openRequests(user, assignmentMode) {
    const citizenView = user.role === 'CITIZEN';
    document.getElementById('events-view').hidden = true;
    requestsTitle.textContent = citizenView
        ? 'My Requests'
        : user.role === 'CITY_ADMINISTRATOR' ? 'Review Requests'
            : assignmentMode ? 'Assign Tasks' : 'Department Requests';
    requestsView.hidden = false;
    requestForm.hidden = true;
    requestFilters.hidden = false;
    requestList.hidden = false;
    requestMessage.hidden = true;
    notice.hidden = true;
    error.hidden = true;
    requestList.replaceChildren();
    loadedRequestEntries = [];
    renderRequestEntry = null;

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
        let departments = [];
        let departmentLoadError = null;
        let workers = [];
        let workerLoadError = null;
        if (user.role === 'CITY_ADMINISTRATOR' && requests.some(request => request.status === 'UNDER_REVIEW')) {
            try {
                departments = await Api.get('/departments');
            } catch (departmentError) {
                departmentLoadError = departmentError.message;
            }
        }
        if (user.role === 'DEPARTMENT_MANAGER' && assignmentMode
            && requests.some(request => request.status === 'ASSIGNED' || request.status === 'IN_PROGRESS')) {
            try {
                workers = await Api.get('/departments/' + user.departmentId + '/workers');
            } catch (workerError) {
                workerLoadError = workerError.message;
            }
        }
        const displayedRequests = assignmentMode
            ? requests.filter(request => request.status === 'ASSIGNED' || request.status === 'IN_PROGRESS')
            : requests;
        if (assignmentMode && displayedRequests.length === 0) {
            const empty = document.createElement('p');
            empty.textContent = 'There are no assigned or in-progress requests to manage.';
            requestList.appendChild(empty);
            return;
        }
        loadedRequestEntries = displayedRequests;
        renderRequestEntry = request => createRequestCard(
            request, user.role === 'CITY_ADMINISTRATOR', departments, departmentLoadError,
            user.role === 'DEPARTMENT_MANAGER' && assignmentMode, workers, workerLoadError);
        renderFilteredRequests();
    } catch (requestError) {
        error.textContent = requestError.message;
        error.hidden = false;
    }
}

async function openMyTasks(user) {
    document.getElementById('events-view').hidden = true;
    requestsTitle.textContent = 'My Tasks';
    requestsView.hidden = false;
    requestForm.hidden = true;
    requestFilters.hidden = false;
    requestList.hidden = false;
    requestMessage.hidden = true;
    notice.hidden = true;
    error.hidden = true;
    requestList.replaceChildren();
    loadedRequestEntries = [];
    renderRequestEntry = null;

    try {
        const tasks = await Api.get('/me/tasks');
        if (!tasks.length) {
            const empty = document.createElement('p');
            empty.textContent = 'You have no assigned tasks.';
            requestList.appendChild(empty);
            return;
        }
        for (const task of tasks) {
            loadedRequestEntries.push(task);
        }
        renderRequestEntry = task => {
            const card = createRequestCard(task.request, false, [], null, false, [], null, task);
            appendRequestText(card, 'Assigned: ' + new Date(task.assignedAt).toLocaleString());
            if (task.notes) {
                appendRequestText(card, 'Task notes: ' + task.notes);
            }
            return card;
        };
        renderFilteredRequests();
    } catch (taskError) {
        error.textContent = taskError.message;
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
    requestFilters.hidden = true;
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

requestFilters.addEventListener('submit', event => {
    event.preventDefault();
    const filters = new FormData(requestFilters);
    if (filters.get('from') && filters.get('to') && filters.get('from') > filters.get('to')) {
        error.textContent = '"Submitted from" must be on or before "Submitted to".';
        error.hidden = false;
        return;
    }
    error.hidden = true;
    renderFilteredRequests();
});

document.getElementById('reset-request-filters').addEventListener('click', () => {
    requestFilters.reset();
    error.hidden = true;
    renderFilteredRequests();
});

function filterRequests(entries) {
    const filters = new FormData(requestFilters);
    const status = filters.get('status');
    const type = filters.get('type');
    const from = filters.get('from');
    const to = filters.get('to');
    const sort = filters.get('sort');

    return entries
        .filter(entry => {
            const request = entry.request || entry;
            return !status || request.status === status;
        })
        .filter(entry => {
            const request = entry.request || entry;
            return !type || request.type === type;
        })
        .filter(entry => {
            const request = entry.request || entry;
            return !from || request.submittedAt.slice(0, 10) >= from;
        })
        .filter(entry => {
            const request = entry.request || entry;
            return !to || request.submittedAt.slice(0, 10) <= to;
        })
        .sort((left, right) => {
            const leftRequest = left.request || left;
            const rightRequest = right.request || right;
            const difference = new Date(leftRequest.submittedAt) - new Date(rightRequest.submittedAt);
            return sort === 'oldest' ? difference : -difference;
        });
}

function renderFilteredRequests() {
    requestList.replaceChildren();
    const filteredEntries = filterRequests(loadedRequestEntries);
    if (!filteredEntries.length) {
        const empty = document.createElement('p');
        empty.textContent = loadedRequestEntries.length
            ? 'No requests match these filters.'
            : 'No requests are available.';
        requestList.appendChild(empty);
        return;
    }
    for (const entry of filteredEntries) {
        requestList.appendChild(renderRequestEntry(entry));
    }
}

function createRequestCard(request, canReview, departments, departmentLoadError,
    canManageTasks, workers, workerLoadError, task) {
    const card = document.createElement('article');
    card.className = 'request-card';

    const title = document.createElement('h3');
    title.textContent = 'Request #' + request.id + ' · ' + request.type.replaceAll('_', ' ');
    card.appendChild(title);
    appendRequestText(card, request.description);
    appendRequestText(card, request.location);
    const status = document.createElement('p');
    status.className = 'request-status status-' + request.status.toLowerCase().replaceAll('_', '-');
    status.textContent = 'Status: ' + request.status.replaceAll('_', ' ');
    card.appendChild(status);
    appendRequestText(card, 'Submitted: ' + new Date(request.submittedAt).toLocaleString());
    if (request.citizenName) {
        appendRequestText(card, 'Submitted by: ' + request.citizenName);
    }
    if (request.departmentName) {
        appendRequestText(card, 'Department: ' + request.departmentName);
    }
    if (canManageTasks && (request.status === 'ASSIGNED' || request.status === 'IN_PROGRESS')) {
        appendTaskAssignee(card, request.assignedWorkerName);
    } else if (request.assignedWorkerName) {
        appendRequestText(card, 'Assigned to: ' + request.assignedWorkerName);
    }
    if (request.rejectionReason) {
        appendRequestText(card, 'Rejection reason: ' + request.rejectionReason);
    }
    if (request.resolutionNotes) {
        appendRequestText(card, 'Resolution: ' + request.resolutionNotes);
    }
    const user = Session.user();
    if (user && ['CITY_ADMINISTRATOR', 'DEPARTMENT_MANAGER', 'WORKER'].includes(user.role)) {
        addFlagButton(card, 'REQUEST', request.id);
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
    if (canReview && request.status === 'UNDER_REVIEW') {
        if (departmentLoadError) {
            appendRequestText(card, 'Unable to load departments: ' + departmentLoadError);
        } else {
            const departmentSelect = document.createElement('select');
            departmentSelect.className = 'field';
            departmentSelect.required = true;
            departmentSelect.setAttribute('aria-label', 'Assign request to department');
            const placeholder = document.createElement('option');
            placeholder.value = '';
            placeholder.textContent = 'Choose a department';
            departmentSelect.appendChild(placeholder);
            for (const department of departments) {
                const option = document.createElement('option');
                option.value = department.id;
                option.textContent = department.name;
                departmentSelect.appendChild(option);
            }
            card.appendChild(departmentSelect);

            if (departments.length === 0) {
                appendRequestText(card, 'No departments are available for assignment.');
            }
            const assignButton = document.createElement('button');
            assignButton.className = 'button';
            assignButton.type = 'button';
            assignButton.textContent = 'Assign to department';
            assignButton.disabled = departments.length === 0;
            assignButton.addEventListener('click', async () => {
                if (!departmentSelect.value) {
                    departmentSelect.reportValidity();
                    return;
                }
                assignButton.disabled = true;
                error.hidden = true;
                try {
                    await Api.post('/requests/' + request.id + '/assign', {
                        departmentId: Number(departmentSelect.value)
                    });
                    await openRequests(Session.user());
                    requestMessage.className = 'message message-success';
                    requestMessage.textContent = 'Request assigned to department.';
                    requestMessage.hidden = false;
                } catch (requestError) {
                    error.textContent = requestError.message;
                    error.hidden = false;
                    assignButton.disabled = false;
                }
            });
            card.appendChild(assignButton);
        }

        const rejectionForm = document.createElement('div');
        rejectionForm.className = 'request-form';
        const reasonInput = document.createElement('textarea');
        reasonInput.className = 'field';
        reasonInput.maxLength = 500;
        reasonInput.required = true;
        reasonInput.rows = 3;
        reasonInput.setAttribute('aria-label', 'Reason for rejecting request');
        reasonInput.placeholder = 'Enter a rejection reason';
        rejectionForm.appendChild(reasonInput);

        const rejectButton = document.createElement('button');
        rejectButton.className = 'button';
        rejectButton.type = 'button';
        rejectButton.textContent = 'Reject request';
        rejectButton.addEventListener('click', async () => {
            if (!reasonInput.value.trim()) {
                reasonInput.setCustomValidity('Enter a reason for rejecting this request.');
                reasonInput.reportValidity();
                return;
            }
            reasonInput.setCustomValidity('');
            rejectButton.disabled = true;
            error.hidden = true;
            try {
                await Api.post('/requests/' + request.id + '/reject', {
                    reason: reasonInput.value
                });
                await openRequests(Session.user());
                requestMessage.className = 'message message-success';
                requestMessage.textContent = 'Request rejected.';
                requestMessage.hidden = false;
            } catch (requestError) {
                error.textContent = requestError.message;
                error.hidden = false;
                rejectButton.disabled = false;
            }
        });
        reasonInput.addEventListener('input', () => reasonInput.setCustomValidity(''));
        rejectionForm.appendChild(rejectButton);
        card.appendChild(rejectionForm);
    }

    const canStart = request.status === 'ASSIGNED'
        && (!!task || (canManageTasks && request.assignedWorkerId !== null));
    const canResolve = request.status === 'IN_PROGRESS' && (canManageTasks || !!task);
    const canAssignTask = canManageTasks
        && (request.status === 'ASSIGNED' || request.status === 'IN_PROGRESS');
    if (canAssignTask) {
        if (workerLoadError) {
            appendRequestText(card, 'Unable to load workers: ' + workerLoadError);
        } else if (workers.length === 0) {
            appendRequestText(card, 'No workers are available in this department.');
        } else {
            const assignmentPanel = document.createElement('div');
            assignmentPanel.className = 'task-assignment-panel';
            assignmentPanel.hidden = true;
            assignmentPanel.id = 'task-assignment-' + request.id;

            const workerLabel = document.createElement('label');
            workerLabel.className = 'task-assignment-label';
            workerLabel.textContent = request.assignedWorkerId ? 'Choose a worker to reassign to' : 'Choose a worker';
            const workerSelect = document.createElement('select');
            workerSelect.className = 'field';
            workerSelect.required = true;
            workerSelect.setAttribute('aria-label', 'Choose a worker');
            const workerPlaceholder = document.createElement('option');
            workerPlaceholder.value = '';
            workerPlaceholder.textContent = 'Select a worker';
            workerPlaceholder.disabled = true;
            workerPlaceholder.selected = true;
            workerSelect.appendChild(workerPlaceholder);
            for (const worker of workers) {
                const option = document.createElement('option');
                option.value = worker.id;
                option.textContent = worker.name + (worker.skill ? ' · ' + worker.skill : '');
                workerSelect.appendChild(option);
            }
            workerLabel.appendChild(workerSelect);
            assignmentPanel.appendChild(workerLabel);

            const taskNotes = document.createElement('textarea');
            taskNotes.className = 'field';
            taskNotes.maxLength = 1000;
            taskNotes.rows = 2;
            taskNotes.setAttribute('aria-label', 'Optional task notes');
            taskNotes.placeholder = 'Optional task notes';
            assignmentPanel.appendChild(taskNotes);

            const assignmentActions = document.createElement('div');
            assignmentActions.className = 'task-assignment-actions';
            const confirmButton = document.createElement('button');
            confirmButton.className = 'button';
            confirmButton.type = 'button';
            confirmButton.textContent = request.assignedWorkerId ? 'Reassign to selected worker' : 'Assign selected worker';
            confirmButton.addEventListener('click', async () => {
                if (!workerSelect.value) {
                    workerSelect.reportValidity();
                    return;
                }
                confirmButton.disabled = true;
                cancelButton.disabled = true;
                error.hidden = true;
                try {
                    const result = await Api.put('/requests/' + request.id + '/task', {
                        workerId: Number(workerSelect.value),
                        notes: taskNotes.value
                    });
                    await refreshCurrentRequests(Session.user());
                    requestMessage.className = 'message message-success';
                    requestMessage.textContent = result.workerName + ' assigned to request #' + request.id + '.';
                    requestMessage.hidden = false;
                } catch (taskError) {
                    error.textContent = taskError.message;
                    error.hidden = false;
                    confirmButton.disabled = false;
                    cancelButton.disabled = false;
                }
            });
            const cancelButton = document.createElement('button');
            cancelButton.className = 'button button-secondary';
            cancelButton.type = 'button';
            cancelButton.textContent = 'Cancel';
            cancelButton.addEventListener('click', () => {
                assignmentPanel.hidden = true;
                assignmentButton.setAttribute('aria-expanded', 'false');
            });
            assignmentActions.append(confirmButton, cancelButton);
            assignmentPanel.appendChild(assignmentActions);

            const assignmentButton = document.createElement('button');
            assignmentButton.className = 'button';
            assignmentButton.type = 'button';
            assignmentButton.textContent = request.assignedWorkerId ? 'Reassign worker' : 'Assign worker';
            assignmentButton.setAttribute('aria-expanded', 'false');
            assignmentButton.setAttribute('aria-controls', assignmentPanel.id);
            assignmentButton.addEventListener('click', () => {
                assignmentPanel.hidden = !assignmentPanel.hidden;
                assignmentButton.setAttribute('aria-expanded', String(!assignmentPanel.hidden));
                if (!assignmentPanel.hidden) {
                    workerSelect.focus();
                }
            });
            card.append(assignmentButton, assignmentPanel);
        }
    }

    if (canStart) {
        addRequestAction(card, request, 'Start work', '/start', null,
            canManageTasks ? () => refreshCurrentRequests(Session.user()) : () => openMyTasks(Session.user()),
            'Request moved to In Progress.');
    }
    if (canResolve) {
        const resolutionNotes = document.createElement('textarea');
        resolutionNotes.className = 'field';
        resolutionNotes.maxLength = 1000;
        resolutionNotes.rows = 3;
        resolutionNotes.setAttribute('aria-label', 'Resolution notes');
        resolutionNotes.placeholder = 'Optional resolution notes';
        card.appendChild(resolutionNotes);
        addRequestAction(card, request, 'Resolve request', '/resolve',
            () => ({ resolutionNotes: resolutionNotes.value }),
            canManageTasks ? () => refreshCurrentRequests(Session.user()) : () => openMyTasks(Session.user()),
            'Request resolved.');
    }
    return card;
}

function refreshCurrentRequests(user) {
    return openRequests(user, user.role === 'DEPARTMENT_MANAGER' && requestsTitle.textContent === 'Assign Tasks');
}

function addRequestAction(card, request, label, endpoint, body, refresh, successMessage) {
    const button = document.createElement('button');
    button.className = 'button';
    button.type = 'button';
    button.textContent = label;
    button.addEventListener('click', async () => {
        button.disabled = true;
        error.hidden = true;
        try {
            await Api.post('/requests/' + request.id + endpoint, body ? body() : undefined);
            await refresh();
            requestMessage.className = 'message message-success';
            requestMessage.textContent = successMessage;
            requestMessage.hidden = false;
        } catch (actionError) {
            error.textContent = actionError.message;
            error.hidden = false;
            button.disabled = false;
        }
    });
    card.appendChild(button);
}

function appendRequestText(container, text) {
    const line = document.createElement('p');
    line.textContent = text;
    container.appendChild(line);
}

function appendTaskAssignee(container, workerName) {
    const assignee = document.createElement('div');
    assignee.className = 'task-assignee';
    const label = document.createElement('span');
    label.className = 'task-assignee-label';
    label.textContent = 'Assigned worker';
    const value = document.createElement('strong');
    value.textContent = workerName || 'Unassigned';
    assignee.append(label, value);
    container.appendChild(assignee);
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
