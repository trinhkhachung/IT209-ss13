import type { CampusEvent } from '../api/events';
import RegistrationForm from '../components/RegistrationForm';

export default function EventDetailPage({ event, onBack, onRegister, registering, successMessage, errorMessage }: {
  event: CampusEvent; onBack: () => void; onRegister: (name: string, email: string) => void;
  registering: boolean; successMessage: string; errorMessage: string;
}) {
  const start = new Date(event.startsAt);
  const end = new Date(event.endsAt);
  return <section className="content-width detail-page">
    <button className="back-link" onClick={onBack}><span aria-hidden="true">←</span> Tất cả sự kiện</button>
    <div className="detail-grid"><article className="detail-content">
      <div className="detail-art"><span className="eyebrow">{event.category} / CAMPUS 2026</span><span aria-hidden="true">✳</span><p>MEET<br />YOUR<br /><em>PEOPLE.</em></p></div>
      <p className="eyebrow detail-label">SỰ KIỆN NỔI BẬT</p><h1>{event.title}</h1><p className="detail-description">{event.description}</p>
      <div className="detail-facts"><div><span>NGÀY & GIỜ</span><strong>{start.toLocaleString('vi-VN', { dateStyle: 'long', timeStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' })} – {end.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Ho_Chi_Minh' })}</strong></div>
        <div><span>ĐỊA ĐIỂM</span><strong>{event.location}</strong></div><div><span>CHỖ TRỐNG</span><strong>{event.remainingSeats} / {event.capacity}</strong></div></div>
    </article><aside><RegistrationForm onSubmit={onRegister} submitting={registering} successMessage={successMessage} errorMessage={errorMessage} disabled={!event.registrationOpen || event.remainingSeats === 0} /></aside></div>
  </section>;
}
