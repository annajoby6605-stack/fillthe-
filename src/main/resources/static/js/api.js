/**
 * Fill the Pockets - Common API & Auth Client
 */

const API = {
    BASE_URL: '/api',

    // Authentication helpers
    getToken() {
        return localStorage.getItem('ftp_token');
    },

    setToken(token) {
        localStorage.setItem('ftp_token', token);
    },

    getUser() {
        try {
            const user = localStorage.getItem('ftp_user');
            return user ? JSON.parse(user) : null;
        } catch (error) {
            console.error('Could not read saved user:', error);
            return null;
        }
    },

    setUser(user) {
        localStorage.setItem('ftp_user', JSON.stringify(user));
    },

    clearAuth() {
        localStorage.removeItem('ftp_token');
        localStorage.removeItem('ftp_user');
    },

    logout() {
        const user = this.getUser();
        this.clearAuth();

        if (user && user.role === 'ADMIN') {
            window.location.href = '/admin-login.html';
        } else if (user && user.role === 'PANCHAYAT_EMPLOYEE') {
            window.location.href = '/employee-login.html';
        } else {
            window.location.href = '/villager-login.html';
        }
    },

    // Common HTTP request handler
    async request(endpoint, options = {}) {
        const headers = { ...(options.headers || {}) };
        const isFormData =
            typeof FormData !== 'undefined' &&
            options.body instanceof FormData;

        if (!isFormData && options.body !== undefined) {
            headers['Content-Type'] = 'application/json';
        }

        const token = this.getToken();

        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        try {
            const response = await fetch(`${this.BASE_URL}${endpoint}`, {
                ...options,
                headers
            });

            const contentType = response.headers.get('content-type') || '';
            let data = null;

            if (response.status !== 204) {
                if (contentType.includes('application/json')) {
                    data = await response.json();
                } else {
                    const text = await response.text();
                    data = text ? { message: text } : null;
                }
            }

            if (!response.ok) {
                if (
                    response.status === 401 &&
                    !endpoint.includes('/auth/')
                ) {
                    const user = this.getUser();
                    this.clearAuth();

                    if (user && user.role === 'ADMIN') {
                        window.location.href = '/admin-login.html';
                    } else if (
                        user &&
                        user.role === 'PANCHAYAT_EMPLOYEE'
                    ) {
                        window.location.href = '/employee-login.html';
                    } else {
                        window.location.href = '/villager-login.html';
                    }
                }

                const message =
                    data && data.message
                        ? data.message
                        : `Request failed with status ${response.status}`;

                throw new Error(message);
            }

            return data;
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    },

    // Authentication endpoints
    registerVillager(data) {
        return this.request('/auth/register-villager', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    loginVillager(email, password) {
        return this.request('/auth/login-villager', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
    },

    loginAdmin(email, password) {
        return this.request('/auth/login-admin', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
    },

    initiateEmployeeLogin(email, password) {
        return this.request('/auth/employee/initiate-login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
    },

    verifyEmployeeOtp(email, code) {
        return this.request('/auth/employee/verify-otp', {
            method: 'POST',
            body: JSON.stringify({ email, code })
        });
    },

    requestEmployeeAccess(data) {
        return this.request('/auth/employee/request-access', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    // Public statistics
    getPublicStats() {
        return this.request('/stats/public', {
            method: 'GET'
        });
    },

    // Villager endpoints
    getVillagerDashboard() {
        return this.request('/villager/dashboard', {
            method: 'GET'
        });
    },

    getWallet() {
        return this.request('/villager/wallet', {
            method: 'GET'
        });
    },

    submitRoadReport(formData) {
        return this.request('/reports/submit', {
            method: 'POST',
            body: formData
        });
    },

    getMyReports() {
        return this.request('/reports/my-reports', {
            method: 'GET'
        });
    },

    async getReportImage(reportId) {
        const token = this.getToken();
        if (!token) {
            throw new Error('Please sign in to view this report image.');
        }

        const response = await fetch(
            `${this.BASE_URL}/reports/${encodeURIComponent(reportId)}/image`,
            { headers: { Authorization: `Bearer ${token}` } }
        );
        if (!response.ok) {
            if (response.status === 401) {
                throw new Error('Your session expired. Sign in again to view this photo.');
            }
            if (response.status === 403) {
                throw new Error('Your account is not authorized to view this report photo.');
            }
            if (response.status === 404) {
                throw new Error('The uploaded photo is no longer available.');
            }
            throw new Error(`Photo request failed with status ${response.status}.`);
        }

        const image = await response.blob();
        if (!image.type.startsWith('image/')) {
            throw new Error('The server did not return an image.');
        }
        return URL.createObjectURL(image);
    },

    // Employee endpoints
    getEmployeeStats() {
        return this.request('/employee/dashboard-stats', {
            method: 'GET'
        });
    },

    getEmployeeReports(params = {}) {
        const query = new URLSearchParams(params).toString();

        return this.request(
            `/employee/reports${query ? '?' + query : ''}`,
            { method: 'GET' }
        );
    },

    getPendingEmployeeVerificationReports() {
        return this.request('/employee/reports/pending-image-verification', {
            method: 'GET'
        });
    },

    verifySubmission(submissionId, action = 'APPROVE', reason = null) {
        return this.request('/verify-submission', {
            method: 'POST',
            body: JSON.stringify({
                submissionId: Number(submissionId),
                reportId: Number(submissionId),
                action: action,
                reason: reason
            })
        });
    },

    approveEmployeeReport(reportId) {
        return this.verifySubmission(reportId, 'APPROVE');
    },

    rejectEmployeeReport(reportId, reason) {
        return this.verifySubmission(reportId, 'REJECT', reason);
    },

    updateRepairStatus(reportId, newStatus, notes) {
        return this.request(
            `/employee/reports/${reportId}/update-repair-status`,
            {
                method: 'POST',
                body: JSON.stringify({ newStatus, notes })
            }
        );
    },

    getRepairHistory(reportId) {
        return this.request(
            `/employee/reports/${reportId}/repair-history`,
            { method: 'GET' }
        );
    },

    getVillagerSummary(villagerId) {
        return this.request(
            `/employee/villagers/${villagerId}/summary`,
            { method: 'GET' }
        );
    },

    // Reward and coupon endpoints
    getCoupons() {
        return this.request('/rewards/coupons', {
            method: 'GET'
        });
    },

    redeemCoupon(couponId) {
        return this.request('/rewards/redeem', {
            method: 'POST',
            body: JSON.stringify({ couponId })
        });
    },

    getMyRedemptions() {
        return this.request('/rewards/my-redemptions', {
            method: 'GET'
        });
    },

    // Admin endpoints
    getPendingAdminReports() {
        return this.request('/admin/reports/pending', {
            method: 'GET'
        });
    }
};

function initializePasswordVisibilityToggles(root = document) {
    root.querySelectorAll('input[type="password"]').forEach((input) => {
        if (input.closest('.password-toggle-wrapper')) {
            return;
        }

        const wrapper = document.createElement('span');
        wrapper.className = 'password-toggle-wrapper';
        input.parentNode.insertBefore(wrapper, input);
        wrapper.appendChild(input);

        const toggle = document.createElement('button');
        toggle.type = 'button';
        toggle.className = 'password-toggle-button';
        toggle.setAttribute('aria-label', 'Show password');
        toggle.setAttribute('aria-pressed', 'false');
        toggle.title = 'Show password';
        toggle.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12Z"></path><circle cx="12" cy="12" r="3"></circle></svg>';
        toggle.addEventListener('click', () => {
            const showPassword = input.type === 'password';
            input.type = showPassword ? 'text' : 'password';
            toggle.setAttribute('aria-label', showPassword ? 'Hide password' : 'Show password');
            toggle.setAttribute('aria-pressed', String(showPassword));
            toggle.title = showPassword ? 'Hide password' : 'Show password';
        });
        wrapper.appendChild(toggle);
    });
}

const passwordToggleStyles = document.createElement('style');
passwordToggleStyles.textContent = `
    .password-toggle-wrapper {
        position: relative;
        display: block;
        width: 100%;
    }
    .password-toggle-wrapper > input[type="password"],
    .password-toggle-wrapper > input[type="text"] {
        box-sizing: border-box;
        padding-right: 3rem;
    }
    .password-toggle-button {
        position: absolute;
        top: 50%;
        right: 0.55rem;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 2rem;
        height: 2rem;
        padding: 0.35rem;
        transform: translateY(-50%);
        border: 0;
        border-radius: 0.4rem;
        background: transparent;
        color: #53645b;
        cursor: pointer;
    }
    .password-toggle-button:hover {
        background: #edf2ee;
        color: #163b32;
    }
    .password-toggle-button:focus-visible {
        outline: 2px solid #2d6a4f;
        outline-offset: 2px;
    }
    .password-toggle-button svg {
        width: 1.25rem;
        height: 1.25rem;
        fill: none;
        stroke: currentColor;
        stroke-width: 1.8;
        stroke-linecap: round;
        stroke-linejoin: round;
    }
`;
document.head.appendChild(passwordToggleStyles);
initializePasswordVisibilityToggles();
new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
        mutation.addedNodes.forEach((node) => {
            if (node.nodeType === Node.ELEMENT_NODE) {
                initializePasswordVisibilityToggles(node);
                if (node.matches('input[type="password"]')) {
                    initializePasswordVisibilityToggles(node.parentNode);
                }
            }
        });
    });
}).observe(document.body, { childList: true, subtree: true });

// UI toast notification helper
function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');

    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    const icon = document.createElement('span');
    icon.style.fontSize = '1.2rem';
    icon.textContent =
        type === 'success' ? '✓' :
        type === 'error' ? '⚠' : 'ℹ';

    const text = document.createElement('div');
    text.textContent = message;

    toast.append(icon, text);
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        toast.style.transition = 'all 0.3s ease';

        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// Format date helper
function formatDate(dateStr) {
    if (!dateStr) {
        return 'N/A';
    }

    const date = new Date(dateStr);

    if (Number.isNaN(date.getTime())) {
        return 'N/A';
    }

    return date.toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}