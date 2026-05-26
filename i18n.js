// js/i18n.js
let translations = {};
let currentLang = 'en';

async function initI18n() {
    currentLang = localStorage.getItem('preferredLanguage') || 'en';

    try {
        const res = await fetch(`/js/translations/${currentLang}.json`);
        translations = await res.json();
    } catch (e) {
        console.warn("Translation file not found, using English");
        const enRes = await fetch('/js/translations/en.json');
        translations = await enRes.json();
    }
    applyTranslations();
}

function applyTranslations() {
    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (translations[key]) el.textContent = translations[key];
    });

    document.querySelectorAll('[data-i18n-placeholder]').forEach(el => {
        const key = el.getAttribute('data-i18n-placeholder');
        if (translations[key]) el.placeholder = translations[key];
    });
}

window.initI18n = initI18n;