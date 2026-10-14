import type { CampusEvent } from '../api/events';
import EventCard from '../components/EventCard';
import EventFilters from '../components/EventFilters';

export default function EventListPage({ events, query, category, loading, error, onFilter, onRetry, onOpen }: {
  events: CampusEvent[]; query: string; category: string; loading: boolean; error: string;
  onFilter: (filters: { query: string; category: string }) => void; onRetry: () => void; onOpen: (id: string) => void;
}) {
  return <section className="content-width events-section" id="events">
    <div className="section-heading"><div><p className="eyebrow">LỊCH CAMPUS / 2026</p><h2>Chọn cuộc vui<br /><em>tiếp theo.</em></h2></div><p className="section-note">Một buổi chiều mới, một ý tưởng mới,<br />hay một người bạn mới — bắt đầu từ đây.</p></div>
    <EventFilters query={query} category={category} onSubmit={onFilter} />
    {loading ? <div className="state-panel" role="status">Đang tìm những điều thú vị…</div>
      : error ? <div className="state-panel"><p role="alert">{error}</p><button className="button button-dark" onClick={onRetry}>Thử lại</button></div>
        : events.length === 0 ? <div className="state-panel empty-state"><span aria-hidden="true">✳</span><h3>Chưa có sự kiện phù hợp</h3><p>Thử đổi từ khóa hoặc danh mục để khám phá thêm nhé.</p></div>
          : <div className="event-grid">{events.map((event, index) => <EventCard key={event.id} event={event} index={index} onOpen={onOpen} />)}</div>}
  </section>;
}
