(() => {
    const headers = { 'X-Console-Session': '1' };
    const signIn = document.querySelector('[data-session-refresh]');
    if (signIn) {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 10000);
        signIn.addEventListener('submit', () => controller.abort(), { once: true });
        fetch(signIn.dataset.sessionRefresh, { method: 'POST', headers, signal: controller.signal })
            .then(response => {
                if (response.ok && !controller.signal.aborted) {
                    window.location.replace(signIn.dataset.sessionHome);
                }
            })
            .catch(() => {})
            .finally(() => clearTimeout(timeout));
    }

    const logout = document.querySelector('[data-session-logout]');
    if (logout) {
        logout.addEventListener('click', async () => {
            logout.disabled = true;
            const error = document.getElementById('console-logout-error');
            error.hidden = true;
            try {
                const response = await fetch(logout.dataset.sessionLogout, {
                    method: 'POST', headers, signal: AbortSignal.timeout(15000),
                });
                if (!response.ok) throw new Error();
                window.location.replace(logout.dataset.sessionLogin);
            } catch {
                error.hidden = false;
                logout.disabled = false;
            }
        });
    }
})();
