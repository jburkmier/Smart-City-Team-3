// Notifications section: the caller's notifications from GET /me/notifications (docs/api.md section 7),
// newest first. There is no live push, so the list loads when the section opens and on Refresh.
// The nav button shows the unread count, for example "Notifications (3)".

// Only these roles receive notifications (docs/api.md notification table), so only they see the button.
const NOTIFICATION_ROLES = ['CITIZEN', 'CITY_ADMINISTRATOR', 'WORKER'];

const NOTIFICATION_TYPE_NAMES = {
    ANNOUNCEMENT: 'Announcement',
    EMERGENCY_ALERT: 'Emergency alert',
    REQUEST_STATUS: 'Request update',
    TASK_ASSIGNED: 'Task assigned',
    EVENT_CANCELLED: 'Event cancelled',
    FLAG_SUBMITTED: 'New content flag'
};

const notificationsView = document.getElementById('notifications-view');
const notificationList = document.getElementById('notification-list');
const notificationsMessage = document.getElementById('notifications-message');
const markAllNotificationsButton = document.getElementById('mark-all-notifications');

let unreadNotificationCount = 0;

function canReceiveNotifications(user) {
    return !!user && NOTIFICATION_ROLES.includes(user.role);
}

/** Shows the last known unread count on the nav button. */
function renderNotificationCount() {
    const button = document.querySelector('#nav button[data-feature="Notifications"]');
    if (!button) {
        return;
    }
    button.textContent = unreadNotificationCount > 0
        ? 'Notifications (' + unreadNotificationCount + ')'
        : 'Notifications';
}

/** Asks the server for the unread count, for the nav button. */
async function updateNotificationCount() {
    if (!canReceiveNotifications(Session.user())) {
        return;
    }
    try {
        const unread = await Api.get('/me/notifications?unreadOnly=true');
        unreadNotificationCount = unread.length;
        renderNotificationCount();
    } catch (countError) {
        // The count is a convenience; the section itself shows any error when it is opened.
    }
}

function openNotifications() {
    document.getElementById('events-view').hidden = true;
    document.getElementById('requests-view').hidden = true;
    document.getElementById('announcements-view').hidden = true;
    document.getElementById('notice').hidden = true;
    document.getElementById('error').hidden = true;
    notificationsView.hidden = false;
    loadNotifications();
}

async function loadNotifications() {
    hideNotificationsMessage();
    try {
        const notifications = await Api.get('/me/notifications');
        unreadNotificationCount = notifications.filter(notification => !notification.read).length;
        renderNotificationCount();
        markAllNotificationsButton.disabled = unreadNotificationCount === 0;

        notificationList.replaceChildren();
        if (!notifications.length) {
            const empty = document.createElement('p');
            empty.textContent = 'You have no notifications.';
            notificationList.appendChild(empty);
            return;
        }
        for (const notification of notifications) {
            notificationList.appendChild(createNotificationCard(notification));
        }
    } catch (loadError) {
        showNotificationsError(loadError);
    }
}

function createNotificationCard(notification) {
    const emergency = notification.type === 'EMERGENCY_ALERT';
    const card = document.createElement('article');
    card.className = 'notification-card';
    if (!notification.read) {
        card.classList.add('notification-unread');
    }
    if (emergency) {
        card.classList.add('emergency');
    }

    const heading = document.createElement('h3');
    if (!notification.read) {
        const tag = document.createElement('span');
        tag.className = 'notification-tag';
        tag.textContent = 'New';
        heading.appendChild(tag);
    }
    if (emergency) {
        const tag = document.createElement('span');
        tag.className = 'emergency-tag';
        tag.textContent = 'Emergency';
        heading.appendChild(tag);
    }
    heading.appendChild(document.createTextNode(
        NOTIFICATION_TYPE_NAMES[notification.type] || notification.type));
    card.appendChild(heading);

    const message = document.createElement('p');
    message.textContent = notification.message;
    card.appendChild(message);

    const time = document.createElement('p');
    time.className = 'notification-time';
    time.textContent = new Date(notification.createdAt).toLocaleString();
    card.appendChild(time);

    if (notification.type === 'FLAG_SUBMITTED' && isAdministrator()) {
        const reviewButton = document.createElement('button');
        reviewButton.className = 'button';
        reviewButton.type = 'button';
        reviewButton.textContent = 'Review flagged content';
        reviewButton.addEventListener('click', () => openFlaggedContent(notification.relatedId));
        card.appendChild(reviewButton);
    }

    if (!notification.read) {
        const markReadButton = document.createElement('button');
        markReadButton.className = 'button';
        markReadButton.type = 'button';
        markReadButton.textContent = 'Mark read';
        markReadButton.addEventListener('click', async () => {
            markReadButton.disabled = true;
            try {
                await Api.post('/me/notifications/' + notification.id + '/read');
                await loadNotifications();
            } catch (markError) {
                showNotificationsError(markError);
                markReadButton.disabled = false;
            }
        });
        card.appendChild(markReadButton);
    }
    return card;
}

function showNotificationsError(notificationError) {
    notificationsMessage.className = 'message message-error';
    notificationsMessage.textContent = notificationError.message;
    notificationsMessage.hidden = false;
}

function hideNotificationsMessage() {
    notificationsMessage.hidden = true;
}

document.getElementById('refresh-notifications').addEventListener('click', () => {
    loadNotifications();
});

markAllNotificationsButton.addEventListener('click', async () => {
    markAllNotificationsButton.disabled = true;
    try {
        await Api.post('/me/notifications/read-all');
        await loadNotifications();
    } catch (markError) {
        showNotificationsError(markError);
        markAllNotificationsButton.disabled = false;
    }
});

document.getElementById('close-notifications').addEventListener('click', () => {
    notificationsView.hidden = true;
});

updateNotificationCount();
