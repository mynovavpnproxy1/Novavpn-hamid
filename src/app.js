const connectButton = document.getElementById('connectBtn');
const disconnectButton = document.getElementById('disconnectBtn');
const configInput = document.getElementById('configInput');
const status = document.getElementById('status');
const message = document.getElementById('message');

configInput.value = localStorage.getItem('nova_vless_config') || '';

function setStatus(label, detail) {
  status.textContent = label;
  message.textContent = detail || '';
}

function nativeVpn() {
  return window.Capacitor?.Plugins?.NovaVpn;
}

connectButton.addEventListener('click', async () => {
  const config = configInput.value.trim();
  if (!config.startsWith('vless://')) {
    setStatus('کانفیگ نامعتبر', 'یک لینک معتبر VLESS وارد کنید.');
    return;
  }

  localStorage.setItem('nova_vless_config', config);
  const vpn = nativeVpn();
  if (!vpn) {
    setStatus('نسخه اندروید لازم است', 'اتصال VPN فقط در نسخه نصب‌شده اندروید فعال است.');
    return;
  }

  connectButton.disabled = true;
  setStatus('در حال اتصال', 'درخواست اتصال به سرویس اندروید ارسال شد.');
  try {
    await vpn.connect({ config });
    await new Promise(resolve => setTimeout(resolve, 1200));
    const result = await vpn.getStatus();
    if (result.connected) {
      setStatus('موتور فعال است', 'موتور Xray اجرا شده است؛ برای تأیید عبور ترافیک باید اتصال سرور آزمایش شود.');
    } else {
      setStatus('اتصال برقرار نشد', result.error || 'موتور هنوز فعال نشده است.');
    }
  } catch (error) {
    setStatus('خطا در اتصال', error?.message || 'درخواست اتصال ناموفق بود.');
  } finally {
    connectButton.disabled = false;
  }
});

disconnectButton.addEventListener('click', async () => {
  const vpn = nativeVpn();
  if (!vpn) return setStatus('قطع اتصال', 'نسخه اندروید لازم است.');
  try {
    await vpn.disconnect();
    setStatus('قطع اتصال', 'درخواست قطع اتصال ارسال شد.');
  } catch (error) {
    setStatus('خطا', error?.message || 'قطع اتصال ناموفق بود.');
  }
});
