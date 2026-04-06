(function() {
    const form = document.getElementById('loginForm');
    const message = document.getElementById('message');
    const loginBtn = document.getElementById('loginBtn');

    form.addEventListener('submit', async function(e) {
        e.preventDefault();

        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;

        message.textContent = '';
        message.className = 'message';

        loginBtn.disabled = true;
        loginBtn.textContent = 'Logging in...';

        try {
            const response = await fetch('/api/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, password })
            });

            const data = await response.json();

            if (response.ok) {
                message.textContent = data.message || 'Login successful!';
                message.className = 'message success';
                form.reset();
            } else {
                message.textContent = data.message || 'Login failed';
                message.className = 'message error';
            }
        } catch (err) {
            message.textContent = 'Network error. Please try again.';
            message.className = 'message error';
        } finally {
            loginBtn.disabled = false;
            loginBtn.textContent = 'Login';
        }
    });
})();
