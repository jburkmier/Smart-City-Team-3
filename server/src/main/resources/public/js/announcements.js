// Announcements section: every user can read the city's announcements (GET /announcements, newest first).
// City administrators also see a form to post one, which notifies every citizen, and a Delete button on each.

const announcementsView = document.getElementById('announcements-view');
const announcementList = document.getElementById('announcement-list');
const announcementsMessage = document.getElementById('announcements-message');
const announcementForm = document.getElementById('announcement-form');

function isAdministrator() {
    const user = Session.user();
    return !!user && user.role === 'CITY_ADMINISTRATOR';
}

function openAnnouncements() {
    document.getElementById('events-view').hidden = true;
    document.getElementById('requests-view').hidden = true;
    document.getElementById('notifications-view').hidden = true;
    document.getElementById('notice').hidden = true;
    document.getElementById('error').hidden = true;
    announcementForm.hidden = !isAdministrator();
    announcementForm.reset();
    hideAnnouncementsMessage();
    announcementsView.hidden = false;
    loadAnnouncements();
}

async function loadAnnouncements() {
    try {
        const announcements = await Api.get('/announcements');
        announcementList.replaceChildren();
        if (!announcements.length) {
            const empty = document.createElement('p');
            empty.textContent = 'There are no announcements.';
            announcementList.appendChild(empty);
            return;
        }
        for (const announcement of announcements) {
            announcementList.appendChild(createAnnouncementCard(announcement));
        }
    } catch (loadError) {
        showAnnouncementsError(loadError);
    }
}

function createAnnouncementCard(announcement) {
    const emergency = announcement.type === 'EMERGENCY';
    const card = document.createElement('article');
    card.className = emergency ? 'announcement-card emergency' : 'announcement-card';

    const heading = document.createElement('h3');
    if (emergency) {
        const tag = document.createElement('span');
        tag.className = 'emergency-tag';
        tag.textContent = 'Emergency';
        heading.appendChild(tag);
    }
    heading.appendChild(document.createTextNode(announcement.title));
    card.appendChild(heading);

    const message = document.createElement('p');
    message.className = 'announcement-message';
    message.textContent = announcement.message;
    card.appendChild(message);

    const time = document.createElement('p');
    time.className = 'announcement-time';
    time.textContent = 'Posted ' + new Date(announcement.createdAt).toLocaleString();
    card.appendChild(time);

    if (isAdministrator()) {
        const deleteButton = document.createElement('button');
        deleteButton.className = 'button';
        deleteButton.type = 'button';
        deleteButton.textContent = 'Delete';
        deleteButton.addEventListener('click', async () => {
            if (!window.confirm('Delete "' + announcement.title + '"? This can\'t be undone.')) {
                return;
            }
            deleteButton.disabled = true;
            try {
                await Api.delete('/announcements/' + announcement.id);
                await loadAnnouncements();
                showAnnouncementsMessage('Announcement deleted.', 'success');
            } catch (deleteError) {
                showAnnouncementsError(deleteError);
                deleteButton.disabled = false;
            }
        });
        card.appendChild(deleteButton);
    }
    addFlagButton(card, 'ANNOUNCEMENT', announcement.id);
    return card;
}

announcementForm.addEventListener('submit', async event => {
    event.preventDefault();
    hideAnnouncementsMessage();
    const form = new FormData(announcementForm);
    const submitButton = announcementForm.querySelector('button[type="submit"]');
    submitButton.disabled = true;
    try {
        await Api.post('/announcements', {
            type: form.get('type'),
            title: form.get('title'),
            message: form.get('message')
        });
        announcementForm.reset();
        await loadAnnouncements();
        showAnnouncementsMessage('Announcement posted. Every citizen has been notified.', 'success');
    } catch (postError) {
        showAnnouncementsError(postError);
        // A server error after saving can leave the announcement posted, so show the current list.
        loadAnnouncements();
    } finally {
        submitButton.disabled = false;
    }
});

function showAnnouncementsMessage(message, type) {
    announcementsMessage.className = 'message message-' + type;
    announcementsMessage.textContent = message;
    announcementsMessage.hidden = false;
}

function showAnnouncementsError(announcementError) {
    const fieldErrors = Object.entries(announcementError.fields || {})
        .map(([field, message]) => field + ': ' + message)
        .join(' ');
    showAnnouncementsMessage(
        fieldErrors ? announcementError.message + ' ' + fieldErrors : announcementError.message, 'error');
}

function hideAnnouncementsMessage() {
    announcementsMessage.hidden = true;
}

document.getElementById('close-announcements').addEventListener('click', () => {
    announcementsView.hidden = true;
});
