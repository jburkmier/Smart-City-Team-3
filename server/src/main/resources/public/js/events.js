const eventsView = document.getElementById('events-view');
const eventsList = document.getElementById('event-list');
const eventsMessage = document.getElementById('events-message');
const eventsTitle = document.getElementById('events-title');
const eventFilters = document.getElementById('event-filters');
const eventForm = document.getElementById('event-form');
const eventFormTitle = document.getElementById('event-form-title');
const showRegistrationsButton = document.getElementById('show-registrations');
const upcomingOnlyLabel = document.getElementById('upcoming-only-label');
const upcomingOnly = document.getElementById('upcoming-only');

let viewingRegistrations = false;
let editingEventId = null;

function openEvents(registrations) {
    const user = Session.user();
    const isAdmin = user && user.role === 'CITY_ADMINISTRATOR';
    viewingRegistrations = registrations;
    eventsView.hidden = false;
    document.getElementById('notice').hidden = true;
    document.getElementById('error').hidden = true;
    showRegistrationsButton.hidden = !user || user.role !== 'CITIZEN';
    upcomingOnlyLabel.hidden = !registrations;
    eventFilters.hidden = registrations;
    eventForm.hidden = !isAdmin || registrations;
    eventsTitle.textContent = registrations ? 'My Registrations' : 'Community Events';
    showRegistrationsButton.textContent = registrations ? 'Browse events' : 'My registrations';
    if (!registrations) {
        editingEventId = null;
        eventForm.reset();
        eventFormTitle.textContent = 'Create an event';
    }
    loadEvents();
}

document.getElementById('close-events').addEventListener('click', () => {
    eventsView.hidden = true;
});

showRegistrationsButton.addEventListener('click', () => {
    openEvents(!viewingRegistrations);
});

upcomingOnly.addEventListener('change', loadEvents);

eventFilters.addEventListener('submit', event => {
    event.preventDefault();
    loadEvents();
});

eventForm.addEventListener('submit', async event => {
    event.preventDefault();
    const form = new FormData(eventForm);
    const payload = {
        title: form.get('title'),
        description: form.get('description') || null,
        category: form.get('category'),
        startTime: form.get('startTime'),
        district: form.get('district'),
        locationDetail: form.get('locationDetail') || null,
        price: Number(form.get('price'))
    };
    const submitButton = eventForm.querySelector('button[type="submit"]');
    submitButton.disabled = true;
    try {
        if (editingEventId === null) {
            await Api.post('/events', payload);
            showEventsMessage('Event created.', 'success');
        } else {
            await Api.put('/events/' + editingEventId, payload);
            showEventsMessage('Event updated.', 'success');
        }
        eventForm.reset();
        editingEventId = null;
        eventFormTitle.textContent = 'Create an event';
        await loadEvents(false);
    } catch (error) {
        showEventsError(error);
    } finally {
        submitButton.disabled = false;
    }
});

document.getElementById('cancel-event-edit').addEventListener('click', () => {
    eventForm.reset();
    editingEventId = null;
    eventFormTitle.textContent = 'Create an event';
});

async function loadEvents(clearMessage = true) {
    if (clearMessage) {
        clearEventsMessage();
    }
    eventsList.replaceChildren();
    try {
        let events;
        if (viewingRegistrations) {
            const query = upcomingOnly.checked ? '?upcoming=true' : '';
            events = await Api.get('/me/registrations' + query);
        } else {
            const values = new URLSearchParams();
            for (const [key, value] of new FormData(eventFilters)) {
                if (value) {
                    values.set(key, value);
                }
            }
            const suffix = values.size ? '?' + values.toString() : '';
            events = await Api.get('/events' + suffix);
        }
        if (!events.length) {
            const empty = document.createElement('p');
            empty.textContent = viewingRegistrations
                ? 'You have no matching registrations.'
                : 'No upcoming events match these filters.';
            eventsList.appendChild(empty);
            return;
        }
        for (const event of events) {
            eventsList.appendChild(createEventCard(event));
        }
    } catch (error) {
        showEventsError(error);
    }
}

function createEventCard(event) {
    const card = document.createElement('article');
    card.className = 'event-card';
    const title = document.createElement('h3');
    title.textContent = event.title;
    card.appendChild(title);
    appendEventText(card, event.description || 'No description provided.');
    appendEventText(card, event.category.replaceAll('_', ' ') + ' · ' + event.district);
    appendEventText(card, new Date(event.startTime).toLocaleString());
    appendEventText(card, event.locationDetail || 'Location details to be announced.');
    appendEventText(card, Number(event.price) === 0 ? 'Free' : '$' + Number(event.price).toFixed(2));
    appendEventText(card, event.registrationCount + ' registered');

    const actions = document.createElement('div');
    actions.className = 'event-actions';

    const user = Session.user();
    if (user && user.role === 'CITIZEN') {
        const registration = document.createElement('button');
        registration.type = 'button';
        registration.className = 'button';
        registration.textContent = event.registeredByMe ? 'Cancel registration' : 'Register';
        registration.addEventListener('click', async () => {
            try {
                if (event.registeredByMe) {
                    await Api.delete('/events/' + event.id + '/registration');
                    showEventsMessage('Registration cancelled.', 'success');
                } else {
                    await Api.post('/events/' + event.id + '/registration');
                    showEventsMessage('You are registered for this event.', 'success');
                }
                await loadEvents(false);
            } catch (error) {
                showEventsError(error);
            }
        });
        actions.appendChild(registration);
    }
    if (user && user.role === 'CITY_ADMINISTRATOR') {
        const edit = document.createElement('button');
        edit.type = 'button';
        edit.className = 'button';
        edit.textContent = 'Edit';
        edit.addEventListener('click', () => populateEventForm(event));
        actions.appendChild(edit);

        const cancel = document.createElement('button');
        cancel.type = 'button';
        cancel.className = 'button';
        cancel.textContent = 'Cancel event';
        cancel.addEventListener('click', async () => {
            if (!window.confirm('Cancel this event? It will disappear from the public event list.')) {
                return;
            }
            try {
                await Api.delete('/events/' + event.id);
                showEventsMessage('Event cancelled.', 'success');
                await loadEvents(false);
            } catch (error) {
                showEventsError(error);
            }
        });
        actions.appendChild(cancel);
    }
    card.appendChild(actions);
    return card;
}

function populateEventForm(event) {
    editingEventId = event.id;
    eventFormTitle.textContent = 'Edit event';
    eventForm.elements.title.value = event.title;
    eventForm.elements.description.value = event.description || '';
    eventForm.elements.category.value = event.category;
    eventForm.elements.startTime.value = event.startTime.slice(0, 16);
    eventForm.elements.district.value = event.district;
    eventForm.elements.locationDetail.value = event.locationDetail || '';
    eventForm.elements.price.value = Number(event.price).toFixed(2);
    eventForm.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function appendEventText(container, text) {
    const line = document.createElement('p');
    line.textContent = text;
    container.appendChild(line);
}

function showEventsMessage(message, type) {
    eventsMessage.className = 'message message-' + type;
    eventsMessage.textContent = message;
    eventsMessage.hidden = false;
}

function showEventsError(error) {
    const fieldErrors = Object.entries(error.fields || {})
        .map(([field, message]) => field + ': ' + message)
        .join(' ');
    showEventsMessage(fieldErrors ? error.message + ' ' + fieldErrors : error.message, 'error');
}

function clearEventsMessage() {
    eventsMessage.textContent = '';
    eventsMessage.hidden = true;
}
