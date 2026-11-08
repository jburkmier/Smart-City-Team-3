// Small helper for calling the SmartCity Hub REST API (see docs/api.md).
// Every page includes this file, then calls for example:
//
//   const response = await Api.post('/auth/login', { email, password });
//   Session.save(response.token, response.user);
//
// Errors are thrown as ApiError, so pages can show error.message
// (and error.fields for VALIDATION_FAILED) next to the inputs.

const API_BASE = '/api';

/** An error response from the server, in the shape described in docs/api.md section 1. */
class ApiError extends Error {
    constructor(status, code, message, fields) {
        super(message);
        this.status = status;   // HTTP status, or 0 if the server could not be reached
        this.code = code;       // e.g. "VALIDATION_FAILED", "EMAIL_TAKEN", "SERVER_UNAVAILABLE"
        this.fields = fields || {};
    }
}

/** The logged-in user's token and UserDto. Kept in sessionStorage, which is cleared when the tab closes. */
const Session = {
    save(token, user) {
        sessionStorage.setItem('token', token);
        sessionStorage.setItem('user', JSON.stringify(user));
    },
    token() {
        return sessionStorage.getItem('token');
    },
    user() {
        const user = sessionStorage.getItem('user');
        return user ? JSON.parse(user) : null;
    },
    clear() {
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('user');
    }
};

const Api = {
    get(path) {
        return request('GET', path);
    },
    post(path, body) {
        return request('POST', path, body);
    },
    put(path, body) {
        return request('PUT', path, body);
    },
    delete(path) {
        return request('DELETE', path);
    }
};

async function request(method, path, body) {
    const headers = {};
    if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
    }
    if (Session.token()) {
        headers['Authorization'] = 'Bearer ' + Session.token();
    }

    let response;
    try {
        response = await fetch(API_BASE + path, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body)
        });
    } catch (networkError) {
        throw new ApiError(0, 'SERVER_UNAVAILABLE', 'Server unavailable. Start the server and retry.');
    }

    if (response.status === 204) {
        return null;
    }

    const data = await response.json().catch(() => null);

    if (!response.ok) {
        // api.md rule: any 401 (except a failed login) means the session is gone.
        if (response.status === 401 && path !== '/auth/login') {
            Session.clear();
            window.location.href = 'login.html';
        }
        throw new ApiError(
            response.status,
            data ? data.error : 'UNKNOWN',
            data ? data.message : 'Unexpected error (' + response.status + ').',
            data ? data.fields : undefined
        );
    }

    return data;
}
