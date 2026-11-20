// Public transit schedule (Check Public Transit Schedule): any logged-in user can read the seeded
// schedule (GET /transit) and narrow it to one route (GET /transit?route=...).

const transitView = document.getElementById('transit-view');
const transitMessage = document.getElementById('transit-message');
const transitRoute = document.getElementById('transit-route');
const transitRows = document.getElementById('transit-rows');

// Incremented on every load or close; a response renders only if no newer load has started.
let currentTransitLoad = 0;

function openTransit() {
    for (const id of ['events-view', 'requests-view', 'announcements-view', 'notifications-view',
        'flags-view', 'reports-view']) {
        document.getElementById(id).hidden = true;
    }
    document.getElementById('notice').hidden = true;
    document.getElementById('error').hidden = true;
    transitView.hidden = false;
    transitRoute.value = '';
    loadRouteOptions();
}

/** Loads the whole schedule, fills the route dropdown from it, and shows it. */
async function loadRouteOptions() {
    const loadId = ++currentTransitLoad;
    try {
        const trips = await Api.get('/transit');
        if (loadId !== currentTransitLoad) {
            return;
        }
        transitRoute.replaceChildren(new Option('All routes', ''));
        for (const route of [...new Set(trips.map(trip => trip.route))]) {
            transitRoute.appendChild(new Option(route, route));
        }
        renderTrips(trips);
    } catch (loadError) {
        if (loadId === currentTransitLoad) {
            showTransitError(loadError);
        }
    }
}

async function loadTrips(route) {
    const loadId = ++currentTransitLoad;
    try {
        const trips = await Api.get('/transit' + (route ? '?route=' + encodeURIComponent(route) : ''));
        if (loadId === currentTransitLoad) {
            renderTrips(trips);
        }
    } catch (loadError) {
        if (loadId === currentTransitLoad) {
            showTransitError(loadError);
        }
    }
}

function renderTrips(trips) {
    transitMessage.hidden = true;
    transitRows.replaceChildren();
    if (!trips.length) {
        transitMessage.className = 'message message-info';
        transitMessage.textContent = 'No scheduled trips.';
        transitMessage.hidden = false;
        return;
    }
    for (const trip of trips) {
        const row = document.createElement('tr');
        for (const value of [trip.route, trip.stop, trip.departureTime, trip.arrivalTime]) {
            const cell = document.createElement('td');
            cell.textContent = value;
            row.appendChild(cell);
        }
        transitRows.appendChild(row);
    }
}

function showTransitError(loadError) {
    transitRows.replaceChildren();
    transitMessage.className = 'message message-error';
    transitMessage.textContent = loadError.message;
    transitMessage.hidden = false;
}

transitRoute.addEventListener('change', () => loadTrips(transitRoute.value));

document.getElementById('close-transit').addEventListener('click', () => {
    currentTransitLoad++;
    transitView.hidden = true;
});
