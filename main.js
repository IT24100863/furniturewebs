// js/main.js
let currentUser = null;

// Translation fallback
window.t = (key) => {
    const defaults = {
        'nav.login': 'Login',
        'nav.register': 'Register',
        'nav.shop': 'Shop',
        'nav.categories': 'Categories',
        'nav.offers': 'Offers',
        'nav.contact': 'Contact',
        'nav.profile': 'Profile',
        'nav.logout': 'Logout',
        'nav.cart': 'My Cart',
        'hero.title': 'Elegant Comfort for Modern Living',
        'hero.subtitle': 'Discover handcrafted furniture that combines timeless design with everyday luxury.',
        'hero.btn': 'Explore Collection',
        'auth.login.title': 'Sign In',
        'auth.email': 'Email',
        'auth.password': 'Password',
        'auth.forgot': 'Forgot password?',
        'auth.login.btn': 'Login',
        'auth.google': 'Continue with Google',
        'auth.noaccount': "Don't have an account?",
        'auth.signup': 'Sign up',
        'profile.details': 'Personal Details',
        'profile.password': 'Change Password',
        'profile.history': 'Login History',
        'profile.edit': 'Edit Profile',
        'profile.save': 'Save Changes',
        'profile.logout': 'Logout',
        'profile.deactivate': 'Deactivate Account',
        'success.profile_updated': 'Profile updated successfully!',
        'error.required': 'Please fill all required fields',
        'error.network': 'Something went wrong. Please try again.',
        'auth.invalid': 'Invalid email or password',
        'forgot.sent': "We've sent a password reset link to your email."
    };
    return window.translations?.[key] || defaults[key] || key;
};

// Improved checkAuth with retries
async function checkAuth(maxAttempts = 6, delayMs = 600) {
    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            const res = await fetch('/api/user/profile', {
                credentials: 'include',
                cache: 'no-store'
            });

            if (res.ok) {
                currentUser = await res.json();
                updateAuthUI(true);
                await loadCartCount();  // ← load cart count right after login detected
                return true;
            }

            if (res.status === 401 && attempt < maxAttempts) {
                await new Promise(r => setTimeout(r, delayMs));
                continue;
            }

            currentUser = null;
            updateAuthUI(false);
            return false;
        } catch (err) {
            console.error("checkAuth error:", err);
            currentUser = null;
            updateAuthUI(false);
            return false;
        }
    }
    return false;
}

function updateAuthUI(isLoggedIn) {
    const authSection = document.getElementById('auth-section');
    if (!authSection) return;

    if (isLoggedIn && currentUser) {
        authSection.innerHTML = `
            <a href="/cart.html" style="display:flex; align-items:center; gap:8px; color:#5D4037; text-decoration:none; font-weight:500; position:relative;">
                <span style="font-size:1.6rem;">🛒</span>
                <span>${window.t('nav.cart')}</span>
                <span id="cart-count" class="cart-count">0</span>
            </a>

            <div class="user-menu">
                <div class="user-trigger">
                    <div class="avatar">${(currentUser.firstName || currentUser.email?.[0] || 'U').toUpperCase()}</div>
                    <span class="username">${currentUser.firstName || currentUser.email?.split('@')[0] || 'User'}</span>
                    <span class="arrow">▼</span>
                </div>
                <div class="dropdown-menu" id="dropdown-menu">
                    <a href="/profile.html" class="dropdown-item">👤 ${window.t('nav.profile')}</a>
                    <a href="#" onclick="logout(); event.preventDefault()" class="dropdown-item">🚪 ${window.t('nav.logout')}</a>
                </div>
            </div>
        `;

        // Dropdown toggle logic
        const trigger = authSection.querySelector('.user-trigger');
        const menu = document.getElementById('dropdown-menu');
        if (trigger && menu) {
            trigger.addEventListener('click', (e) => {
                e.stopPropagation();
                menu.style.display = menu.style.display === 'block' ? 'none' : 'block';
            });
            document.addEventListener('click', () => {
                if (menu) menu.style.display = 'none';
            });
        }
    } else {
        authSection.innerHTML = `
            <a href="/login.html" style="display:flex; align-items:center; gap:7px; color:#2c3e50; text-decoration:none;">
                <span style="font-size:1.5rem;">👤</span>
                <span>${window.t('nav.login')}</span>
            </a>
            <a href="/register.html" style="display:flex; align-items:center; gap:7px; color:#2c3e50; text-decoration:none;">
                <span style="font-size:1.5rem;">✍️</span>
                <span>${window.t('auth.signup')}</span>
            </a>
        `;
    }
}

// Logout
async function logout() {
    try {
        await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
    } catch (err) {
        console.error("Logout failed:", err);
    }
    localStorage.removeItem('preferredLanguage');
    currentUser = null;
    updateAuthUI(false);
    window.location.replace('/');
}

// Fetch and display cart item count (for navbar badge)
async function loadCartCount() {
    const countEl = document.getElementById('cart-count');
    if (!countEl) return;

    try {
        const res = await fetch('/api/cart/count', {
            credentials: 'include',
            cache: 'no-store'
        });

        if (res.ok) {
            const count = await res.json();
            countEl.textContent = count > 99 ? '99+' : count;
            countEl.style.display = count > 0 ? 'flex' : 'none';
        } else {
            countEl.textContent = '0';
            countEl.style.display = 'none';
        }
    } catch (err) {
        console.warn("Cart count failed (likely not logged in)");
        if (countEl) {
            countEl.textContent = '0';
            countEl.style.display = 'none';
        }
    }
}

// Language initialization
async function applyLanguage() {
    if (typeof window.initI18n === 'function') {
        await window.initI18n();
    }
    // Re-apply UI after language change (in case nav text changed)
    updateAuthUI(!!currentUser);
}

// Page load
window.addEventListener('load', async () => {
    // Small delay helps with DOM readiness in some cases
    await new Promise(r => setTimeout(r, 300));

    await applyLanguage();
    await checkAuth();

    // Optional: refresh cart count every 30 seconds (only if logged in)
    if (currentUser) {
        setInterval(() => {
            if (document.getElementById('cart-count')) {
                loadCartCount();
            }
        }, 30000);
    }
});

// Expose useful functions globally
window.checkAuth     = checkAuth;
window.logout        = logout;
window.updateAuthUI  = updateAuthUI;
window.loadCartCount = loadCartCount;