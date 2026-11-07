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

const wait = ms => new Promise(resolve => setTimeout(resolve, ms));

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
  setStatus('در حال اتصال', 'در حال راه‌اندازی رابط VPN و موتور Xray...');
  try {
    await vpn.connect({ config });
    for (let attempt = 0; attempt < 30; attempt++) {
      await wait(500);
      const result = await vpn.getStatus();
      if (result.connected) {
        setStatus('موتور فعال است', 'Xray اجرا شده است؛ برقراری ارتباط واقعی با سرور هنوز باید آزمایش شود.');
        return;
      }
      if (result.error) {
        setStatus('خطا در اتصال', result.error);
        return;
      }
      if (!result.starting && attempt > 1) {
        setStatus('اتصال برقرار نشد', 'سرویس VPN متوقف شده است. لینک VLESS و گزارش خطا را بررسی کنید.');
        return;
      }
    }
    setStatus('زمان اتصال طولانی شد', 'موتور هنوز پاسخ نداده است. یک بار قطع اتصال و تلاش مجدد را انجام دهید.');
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
