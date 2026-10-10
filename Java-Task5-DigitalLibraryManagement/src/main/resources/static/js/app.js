// Toasts auto-dismiss, confirm dialogs, mobile nav, demo credential fill
document.addEventListener('DOMContentLoaded', () => {
  // toasts
  document.querySelectorAll('.toast').forEach((t, i) => {
    const close = () => { t.classList.add('hide'); setTimeout(() => t.remove(), 300); };
    t.querySelector('button')?.addEventListener('click', close);
    setTimeout(close, 6000 + i * 600);
  });

  // mobile nav
  document.querySelector('.nav-toggle')?.addEventListener('click', () =>
    document.querySelector('.nav').classList.toggle('open'));

  // confirm modal for any form with data-confirm
  const back = document.getElementById('confirm-modal');
  let pending = null;
  document.querySelectorAll('form[data-confirm]').forEach(f => {
    f.addEventListener('submit', e => {
      if (f.dataset.ok) return;
      e.preventDefault();
      pending = f;
      back.querySelector('#confirm-text').textContent = f.dataset.confirm;
      back.classList.add('open');
    });
  });
  back?.querySelector('#confirm-cancel').addEventListener('click', () => back.classList.remove('open'));
  back?.addEventListener('click', e => { if (e.target === back) back.classList.remove('open'); });
  back?.querySelector('#confirm-ok').addEventListener('click', () => {
    if (pending) { pending.dataset.ok = '1'; pending.requestSubmit ? pending.requestSubmit() : pending.submit(); }
  });
  document.addEventListener('keydown', e => { if (e.key === 'Escape') back?.classList.remove('open'); });

  // demo credentials quick-fill
  document.querySelectorAll('[data-fill]').forEach(b => b.addEventListener('click', () => {
    const [email, pw] = b.dataset.fill.split('|');
    document.getElementById('email').value = email;
    document.getElementById('password').value = pw;
  }));

  // animated counters
  document.querySelectorAll('[data-count]').forEach(el => {
    const end = +el.dataset.count; let cur = 0; const step = Math.max(1, Math.ceil(end / 40));
    const tick = () => { cur = Math.min(end, cur + step); el.textContent = cur; if (cur < end) requestAnimationFrame(tick); };
    tick();
  });
});
