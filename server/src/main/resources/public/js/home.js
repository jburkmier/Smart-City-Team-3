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
            notice.textContent = feature + ' is coming soon.';
            notice.hidden = false;
        });
        nav.appendChild(button);
    }
}

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
