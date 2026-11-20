// Loads and renders computed reports for administrators and department managers.

const reportsView = document.getElementById('reports-view');
const reportsMessage = document.getElementById('reports-message');
const reportMetrics = document.getElementById('report-metrics');
const reportBreakdowns = document.getElementById('report-breakdowns');
const reportGeneratedAt = document.getElementById('report-generated-at');
const departmentReportForm = document.getElementById('department-report-form');
const departmentReportId = document.getElementById('department-report-id');

// Incremented on every report load or close; a response renders only if no newer load has started.
let currentReportLoad = 0;

function openReports(feature, user) {
    for (const id of ['events-view', 'requests-view', 'announcements-view', 'notifications-view', 'flags-view']) {
        const view = document.getElementById(id);
        if (view) {
            view.hidden = true;
        }
    }
    reportsView.hidden = false;
    reportsMessage.hidden = true;
    reportGeneratedAt.textContent = '';
    reportMetrics.replaceChildren();
    reportBreakdowns.replaceChildren();
    document.getElementById('back-to-main-report').hidden = true;

    const isAdmin = user.role === 'CITY_ADMINISTRATOR';
    document.getElementById('reports-title').textContent =
        feature === 'Engagement Stats' ? 'City Engagement Report' : 'Department Report';
    departmentReportForm.hidden = !isAdmin;

    if (isAdmin) {
        loadEngagementReport();
        loadDepartmentOptions();
    } else {
        loadDepartmentReport(user.departmentId);
    }
}

async function loadEngagementReport() {
    const loadId = ++currentReportLoad;
    try {
        const report = await Api.get('/reports/engagement');
        if (loadId === currentReportLoad) {
            renderEngagementReport(report);
        }
    } catch (reportError) {
        if (loadId === currentReportLoad) {
            showReportsError(reportError);
        }
    }
}

async function loadDepartmentOptions() {
    try {
        const departments = await Api.get('/departments');
        departmentReportId.replaceChildren();
        const placeholder = document.createElement('option');
        placeholder.value = '';
        placeholder.textContent = 'Choose a department';
        departmentReportId.appendChild(placeholder);
        for (const department of departments) {
            const option = document.createElement('option');
            option.value = department.id;
            option.textContent = department.name;
            departmentReportId.appendChild(option);
        }
    } catch (reportError) {
        showReportsError(reportError);
    }
}

async function loadDepartmentReport(departmentId) {
    const loadId = ++currentReportLoad;
    if (!departmentId) {
        showReportsError(new Error('Your account is not associated with a department.'));
        return;
    }
    try {
        const report = await Api.get('/reports/departments/' + encodeURIComponent(departmentId));
        if (loadId === currentReportLoad) {
            renderDepartmentReport(report);
        }
    } catch (reportError) {
        if (loadId === currentReportLoad) {
            showReportsError(reportError);
        }
    }
}

function renderEngagementReport(report) {
    reportGeneratedAt.textContent = 'Generated ' + formatReportDate(report.generatedAt);
    renderMetricCards([
        ['Total users', report.totalUsers],
        ['Citizens', report.totalCitizens],
        ['Active users (last 7 days)', report.activeUsersLast7Days],
        ['Upcoming events', report.upcomingEvents],
        ['Event registrations', report.totalEventRegistrations],
        ['Open flags', report.openFlags]
    ]);
    renderCountTable('Requests by status', report.requestsByStatus);
    renderCountTable('Requests by type', report.requestsByType);
}

function renderDepartmentReport(report) {
    document.getElementById('reports-title').textContent = report.departmentName + ' Department Report';
    reportGeneratedAt.textContent = 'Generated ' + formatReportDate(report.generatedAt);
    renderMetricCards([
        ['Total requests', report.totalRequests],
        ['Average resolution time', report.averageResolutionHours == null
            ? 'Not available' : Number(report.averageResolutionHours).toFixed(1) + ' hours'],
        ['Resolved (last 30 days)', report.resolvedLast30Days]
    ]);
    renderCountTable('Requests by status', report.requestsByStatus);

    const rows = report.openTasksByWorker.map(worker => [worker.workerName, worker.openTasks]);
    renderTable('Open tasks by worker', 'Worker', 'Open tasks',
        rows.length ? rows : [['No workers in this department', '—']]);
    document.getElementById('back-to-main-report').hidden =
        Session.user().role !== 'CITY_ADMINISTRATOR';
}

function renderMetricCards(metrics) {
    reportMetrics.replaceChildren();
    for (const [label, value] of metrics) {
        const card = document.createElement('article');
        card.className = 'report-metric';
        const heading = document.createElement('h3');
        heading.textContent = label;
        const content = document.createElement('p');
        content.textContent = value == null ? '—' : String(value);
        card.append(heading, content);
        reportMetrics.appendChild(card);
    }
}

function renderCountTable(title, counts) {
    const entries = Object.entries(counts || {}).map(([key, count]) => [formatReportLabel(key), count]);
    renderTable(title, 'Category', 'Requests', entries);
}

function renderTable(title, firstHeading, secondHeading, rows) {
    const section = document.createElement('section');
    section.className = 'report-breakdown';
    const heading = document.createElement('h3');
    heading.textContent = title;
    const table = document.createElement('table');
    const thead = document.createElement('thead');
    const headerRow = document.createElement('tr');
    for (const label of [firstHeading, secondHeading]) {
        const cell = document.createElement('th');
        cell.scope = 'col';
        cell.textContent = label;
        headerRow.appendChild(cell);
    }
    thead.appendChild(headerRow);
    const tbody = document.createElement('tbody');
    for (const row of rows) {
        const tableRow = document.createElement('tr');
        for (const value of row) {
            const cell = document.createElement('td');
            cell.textContent = String(value);
            tableRow.appendChild(cell);
        }
        tbody.appendChild(tableRow);
    }
    table.append(thead, tbody);
    section.append(heading, table);
    reportBreakdowns.appendChild(section);
}

function formatReportLabel(value) {
    return value.toLowerCase().replaceAll('_', ' ')
        .replace(/\b\w/g, character => character.toUpperCase());
}

function formatReportDate(value) {
    return value ? new Date(value).toLocaleString() : 'time unavailable';
}

function showReportsError(reportError) {
    reportsMessage.textContent = reportError.message;
    reportsMessage.hidden = false;
}

departmentReportForm.addEventListener('submit', event => {
    event.preventDefault();
    if (!departmentReportId.value) {
        departmentReportId.reportValidity();
        return;
    }
    reportsMessage.hidden = true;
    reportGeneratedAt.textContent = '';
    reportMetrics.replaceChildren();
    reportBreakdowns.replaceChildren();
    document.getElementById('back-to-main-report').hidden = true;
    loadDepartmentReport(departmentReportId.value);
});

document.getElementById('back-to-main-report').addEventListener('click', () => {
    document.getElementById('reports-title').textContent = 'City Engagement Report';
    document.getElementById('back-to-main-report').hidden = true;
    reportsMessage.hidden = true;
    reportGeneratedAt.textContent = '';
    reportMetrics.replaceChildren();
    reportBreakdowns.replaceChildren();
    loadEngagementReport();
});

document.getElementById('close-reports').addEventListener('click', () => {
    currentReportLoad++;
    reportsView.hidden = true;
});
