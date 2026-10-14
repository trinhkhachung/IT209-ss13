import { useState } from 'react';

export default function RegistrationForm({ onSubmit, submitting, successMessage, errorMessage, disabled }: {
  onSubmit: (fullName: string, email: string) => void; submitting: boolean;
  successMessage: string; errorMessage: string; disabled: boolean;
}) {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); onSubmit(fullName.trim(), email.trim());
  }
  return <section className="registration-box" aria-labelledby="registration-title">
    <p className="eyebrow">BẠN SẼ THAM GIA CHỨ?</p><h2 id="registration-title">Giữ một<br /><em>chỗ cho mình.</em></h2>
    {successMessage ? <p className="feedback success" role="status">{successMessage}</p> : <form onSubmit={submit}>
      <label htmlFor="fullName">Họ và tên</label><input id="fullName" value={fullName} onChange={(event) => setFullName(event.target.value)} maxLength={100} required autoComplete="name" />
      <label htmlFor="email">Email</label><input id="email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} maxLength={254} required autoComplete="email" />
      {errorMessage && <p className="feedback error" role="alert">{errorMessage}</p>}
      <button className="button button-dark submit-button" disabled={disabled || submitting}>{submitting ? 'Đang gửi…' : disabled ? 'Đã đóng đăng ký' : 'Đăng ký tham gia'} <span aria-hidden="true">↗</span></button>
      <p className="privacy-note">Thông tin chỉ dùng để xác nhận đăng ký và không hiển thị công khai.</p>
    </form>}
  </section>;
}
