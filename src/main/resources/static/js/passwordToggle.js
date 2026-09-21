(function () {
    var EYE = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>';
    var EYE_OFF = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>';

    function addStyles() {
        var style = document.createElement('style');
        style.textContent =
            '.pw-wrap { position: relative; display: block; }' +
            '.pw-wrap > input { display: block; padding-right: 46px !important; }' +
            '.pw-wrap > input::-ms-reveal, .pw-wrap > input::-ms-clear { display: none; }' +
            '.pw-toggle { position: absolute; right: 6px; top: 50%; transform: translateY(-50%); width: 34px; height: 34px; padding: 0; border: 0; border-radius: 50%; background: none; color: #928985; cursor: pointer; display: flex; align-items: center; justify-content: center; }' +
            '.pw-toggle:hover { color: #bd7076; }' +
            '.pw-toggle svg { width: 18px; height: 18px; }';
        document.head.appendChild(style);
    }

    function addToggle(input) {
        var wrap = document.createElement('div');
        wrap.className = 'pw-wrap';
        input.parentNode.insertBefore(wrap, input);
        wrap.appendChild(input);

        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'pw-toggle';
        btn.setAttribute('aria-label', 'Show password');
        btn.innerHTML = EYE;
        wrap.appendChild(btn);

        btn.addEventListener('click', function () {
            var showing = input.type === 'text';
            input.type = showing ? 'password' : 'text';
            btn.innerHTML = showing ? EYE : EYE_OFF;
            btn.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
        });
    }

    function init() {
        addStyles();
        document.querySelectorAll('input[type="password"]').forEach(addToggle);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();