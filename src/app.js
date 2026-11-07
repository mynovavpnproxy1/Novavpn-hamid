const button = document.getElementById('connectBtn');
const status = document.getElementById('status');
const message = document.getElementById('message');

button.addEventListener('click', () => {
  status.textContent = 'موتور VPN هنوز نصب نشده';
  message.textContent = 'برای جلوگیری از نمایش اتصال غیرواقعی، اتصال غیرفعال است.';
});
