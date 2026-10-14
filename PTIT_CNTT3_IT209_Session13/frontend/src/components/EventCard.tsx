import type { CampusEvent } from '../api/events';

const accents = ['mint', 'lilac', 'peach', 'yellow'];
const symbols = ['✳', '◈', '✦', '◌'];

export default function EventCard({ event, index, onOpen }: { event: CampusEvent; index: number; onOpen: (id: string) => void }) {
  const date = new Date(event.startsAt);
  return <article className="event-card">
    <div className={`event-visual ${accents[index % accents.length]}`}><span className="event-number">0{index + 1} / CAMPUS</span><span className="event-symbol" aria-hidden="true">{symbols[index % symbols.length]}</span><span className="event-category">{event.category}</span></div>
    <div className="event-card-body"><div className="event-meta"><span>{date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'Asia/Ho_Chi_Minh' })}</span><span>{event.remainingSeats} chỗ còn lại</span></div>
      <h3>{event.title}</h3><p>{event.location}</p>
      <button className="text-link" onClick={() => onOpen(event.id)} aria-label={`Xem chi tiết ${event.title}`}>Xem chi tiết <span aria-hidden="true">↗</span></button></div>
  </article>;
}
