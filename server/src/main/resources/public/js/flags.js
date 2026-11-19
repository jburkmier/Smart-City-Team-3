// Reporting controls and city-administrator flag review.

const flagsView = document.getElementById('flags-view');
const flagList = document.getElementById('flag-list');
const flagsMessage = document.getElementById('flags-message');
const flagStatus = document.getElementById('flag-status');
const flagsEditForm = document.getElementById('flags-edit-form');
const flagsEditFields = document.getElementById('flags-edit-fields');
const flagsEditTitle = document.getElementById('flags-edit-title');

function addFlagButton(container, contentType, contentId) {
    const button = document.createElement('button');
    button.className = 'button';
    button.type = 'button';
    button.textContent = 'Flag';
    button.addEventListener('click', async () => {
        const reason = window.prompt('Why are you flagging this ' + contentType.toLowerCase() + '?');
        if (reason === null) {
            return;
        }
        if (!reason.trim()) {
            showFlagMessage('A reason is required.', 'error', container);
            return;
        }
        if (reason.trim().length > 500) {
            showFlagMessage('The reason must be at most 500 characters.', 'error', container);
            return;
        }
        button.disabled = true;
        try {
            await Api.post('/flags', {contentType, contentId, reason});
            showFlagMessage('Flag submitted. City administrators have been notified.', 'success', container);
            button.textContent = 'Flag submitted';
        } catch (flagError) {
            showFlagMessage(flagError.message, 'error', container);
            button.disabled = false;
        }
    });
    container.appendChild(button);
}

function showFlagMessage(message, type, container = flagsMessage) {
    const output = document.createElement('p');
    output.className = 'message message-' + type;
    output.setAttribute('role', type === 'error' ? 'alert' : 'status');
    output.textContent = message;
    container.appendChild(output);
}

function openFlaggedContent(focusId = null) {
    document.getElementById('events-view').hidden = true;
    document.getElementById('requests-view').hidden = true;
    document.getElementById('announcements-view').hidden = true;
    document.getElementById('notifications-view').hidden = true;
    document.getElementById('notice').hidden = true;
    document.getElementById('error').hidden = true;
    if (focusId !== null) {
        flagStatus.value = 'OPEN';
    }
    flagsView.hidden = false;
    flagsEditForm.hidden = true;
    loadFlags(focusId);
}

async function loadFlags(focusId = null) {
    flagsMessage.hidden = true;
    flagsEditForm.hidden = true;
    flagList.replaceChildren();
    try {
        const flags = await Api.get('/flags?status=' + encodeURIComponent(flagStatus.value));
        if (!flags.length) {
            const empty = document.createElement('p');
            empty.textContent = 'No ' + flagStatus.value.toLowerCase() + ' flags.';
            flagList.appendChild(empty);
            return;
        }
        for (const flag of flags) {
            const card = createFlagCard(flag);
            flagList.appendChild(card);
            if (focusId !== null && flag.id === Number(focusId)) {
                card.scrollIntoView({behavior: 'smooth', block: 'center'});
                card.focus();
            }
        }
        if (focusId !== null && !flags.some(flag => flag.id === Number(focusId))) {
            flagsMessage.className = 'message message-info';
            flagsMessage.textContent = 'That flag is no longer open.';
            flagsMessage.hidden = false;
        }
    } catch (loadError) {
        flagsMessage.className = 'message message-error';
        flagsMessage.textContent = loadError.message;
        flagsMessage.hidden = false;
    }
}

function createFlagCard(flag) {
    const card = document.createElement('article');
    card.className = 'flag-card';
    card.tabIndex = -1;

    const heading = document.createElement('h3');
    heading.textContent = flag.contentType + ' #' + flag.contentId + ' - ' + (flag.contentSummary || 'Content unavailable');
    card.appendChild(heading);
    appendFlagText(card, 'Reason: ' + flag.reason);
    appendFlagText(card, 'Reported by: ' + flag.flaggedByName);
    appendFlagText(card, 'Status: ' + flag.status);
    appendFlagText(card, 'Submitted: ' + new Date(flag.createdAt).toLocaleString());

    if (flag.status !== 'REMOVED') {
        const edit = document.createElement('button');
        edit.className = 'button';
        edit.type = 'button';
        edit.textContent = 'Edit content';
        edit.addEventListener('click', () => editFlaggedContent(flag, edit));
        card.appendChild(edit);
    }

    if (flag.status === 'OPEN') {
        const dismiss = document.createElement('button');
        dismiss.className = 'button';
        dismiss.type = 'button';
        dismiss.textContent = 'Dismiss flag';
        dismiss.addEventListener('click', () => moderateFlag(flag.id, 'dismiss', dismiss));
        card.appendChild(dismiss);

        const remove = document.createElement('button');
        remove.className = 'button';
        remove.type = 'button';
        remove.textContent = 'Remove content';
        remove.addEventListener('click', () => moderateFlag(flag.id, 'remove', remove));
        card.appendChild(remove);
    }
    return card;
}

function appendFlagText(container, text) {
    const line = document.createElement('p');
    line.textContent = text;
    container.appendChild(line);
}

async function moderateFlag(flagId, action, button) {
    if (action === 'remove' && !window.confirm('Remove this content from public view?')) {
        return;
    }
    button.disabled = true;
    try {
        await Api.post('/flags/' + flagId + '/' + action);
        await loadFlags();
    } catch (moderationError) {
        flagsMessage.className = 'message message-error';
        flagsMessage.textContent = moderationError.message;
        flagsMessage.hidden = false;
        button.disabled = false;
    }
}

async function editFlaggedContent(flag, button) {
    button.disabled = true;
    flagsMessage.hidden = true;
    try {
        const content = await loadContentForEdit(flag);
        configureEditForm(flag, content);
        flagsEditForm.hidden = false;
        flagsEditForm.scrollIntoView({behavior: 'smooth', block: 'center'});
    } catch (editError) {
        flagsMessage.className = 'message message-error';
        flagsMessage.textContent = editError.message;
        flagsMessage.hidden = false;
    } finally {
        button.disabled = false;
    }
}

async function loadContentForEdit(flag) {
    if (flag.contentType === 'EVENT') {
        return Api.get('/events/' + flag.contentId);
    }
    if (flag.contentType === 'ANNOUNCEMENT') {
        const announcements = await Api.get('/announcements');
        const announcement = announcements.find(item => item.id === flag.contentId);
        if (!announcement) {
            throw new Error('Announcement is no longer available to edit.');
        }
        return announcement;
    }
    const requests = await Api.get('/requests');
    const request = requests.find(item => item.id === flag.contentId);
    if (!request) {
        throw new Error('Request is no longer available to edit.');
    }
    if (request.description === '[removed by administrator]') {
        throw new Error('This request has already been removed and cannot be edited.');
    }
    return request;
}

function configureEditForm(flag, content) {
    flagsEditTitle.textContent = 'Edit ' + flag.contentType.toLowerCase() + ' #' + flag.contentId;
    flagsEditForm.dataset.contentType = flag.contentType;
    flagsEditForm.dataset.contentId = flag.contentId;
    flagsEditFields.replaceChildren();

    if (flag.contentType === 'EVENT') {
        addEditField('title', 'Title', content.title, 'text', {required: true, maxLength: 150});
        addEditField('description', 'Description', content.description || '', 'textarea',
            {maxLength: 2000, wide: true});
        addEditField('category', 'Category', content.category, 'select', {
            options: ['COMMUNITY_SERVICE', 'SPORTS', 'PUBLIC_SAFETY', 'EDUCATION', 'ENVIRONMENT', 'CULTURAL']
        });
        addEditField('startTime', 'Start time', content.startTime.slice(0, 16), 'datetime-local', {required: true});
        addEditField('district', 'District', content.district, 'select', {
            options: ['DOWNTOWN', 'NORTH', 'SOUTH', 'EAST', 'WEST']
        });
        addEditField('locationDetail', 'Location details', content.locationDetail || '', 'text', {maxLength: 255});
        addEditField('price', 'Price', content.price, 'number', {required: true, min: 0, step: '0.01'});
    } else if (flag.contentType === 'REQUEST') {
        addEditField('type', 'Request type', content.type, 'select', {
            options: ['STREETLIGHT', 'POTHOLE', 'GRAFFITI', 'WATER_LEAK', 'NOISE', 'OTHER']
        });
        addEditField('description', 'Description', content.description, 'textarea',
            {required: true, maxLength: 2000, wide: true});
        addEditField('location', 'Location', content.location, 'text', {required: true, maxLength: 255});
    } else {
        addEditField('type', 'Type', content.type, 'select', {options: ['GENERAL', 'EMERGENCY']});
        addEditField('title', 'Title', content.title, 'text', {required: true, maxLength: 150});
        addEditField('message', 'Message', content.message, 'textarea',
            {required: true, maxLength: 2000, wide: true});
    }
}

function addEditField(name, labelText, value, type, options = {}) {
    const label = document.createElement('label');
    if (options.wide) {
        label.classList.add('flags-edit-field-wide');
    }
    label.textContent = labelText;
    let input;
    if (type === 'textarea') {
        input = document.createElement('textarea');
        input.rows = 4;
    } else if (type === 'select') {
        input = document.createElement('select');
        for (const optionValue of options.options) {
            const option = document.createElement('option');
            option.value = optionValue;
            option.textContent = optionValue.replaceAll('_', ' ');
            input.appendChild(option);
        }
    } else {
        input = document.createElement('input');
        input.type = type;
    }
    input.className = 'field';
    input.name = name;
    input.value = value ?? '';
    input.required = !!options.required;
    if (options.maxLength !== undefined) {
        input.maxLength = options.maxLength;
    }
    if (options.min !== undefined) {
        input.min = options.min;
    }
    if (options.step !== undefined) {
        input.step = options.step;
    }
    label.appendChild(input);
    flagsEditFields.appendChild(label);
}

flagsEditForm.addEventListener('submit', async event => {
    event.preventDefault();
    const contentType = flagsEditForm.dataset.contentType;
    const contentId = flagsEditForm.dataset.contentId;
    const form = new FormData(flagsEditForm);
    const payload = Object.fromEntries(form.entries());
    if (contentType === 'EVENT') {
        payload.price = Number(payload.price);
    }
    const button = flagsEditForm.querySelector('button[type="submit"]');
    button.disabled = true;
    try {
        const path = contentType === 'EVENT' ? '/events/' + contentId
            : contentType === 'REQUEST' ? '/requests/' + contentId
                : '/announcements/' + contentId;
        await Api.put(path, payload);
        flagsEditForm.hidden = true;
        flagsMessage.className = 'message message-success';
        flagsMessage.textContent = 'Flagged content updated.';
        flagsMessage.hidden = false;
        await loadFlags();
    } catch (saveError) {
        flagsMessage.className = 'message message-error';
        flagsMessage.textContent = saveError.message;
        flagsMessage.hidden = false;
    } finally {
        button.disabled = false;
    }
});

document.getElementById('cancel-flags-edit').addEventListener('click', () => {
    flagsEditForm.hidden = true;
});

flagStatus.addEventListener('change', () => loadFlags());
document.getElementById('close-flags').addEventListener('click', () => {
    flagsView.hidden = true;
});
