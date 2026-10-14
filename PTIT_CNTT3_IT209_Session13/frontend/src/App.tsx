import { useCallback, useEffect, useState } from 'react';
import { ApiError, fetchEvent, fetchEvents, submitRegistration, type CampusEvent } from './api/events';
import EventDetailPage from './pages/EventDetailPage';
import EventListPage from './pages/EventListPage';
import './styles.css';

export default function App() {
  const [events, setEvents] = useState<CampusEvent[]>([]);
  const [filters, setFilters] = useState({ query: '', category: '' });
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [selectedEvent, setSelectedEvent] = useState<CampusEvent | null>(null);
  const [detailError, setDetailError] = useState('');
  const [registering, setRegistering] = useState(false);
  const [registrationMessage, setRegistrationMessage] = useState('');
  const [registrationError, setRegistrationError] = useState('');

  const loadEvents = useCallback(async (nextFilters: { query: string; category: string }) => {
    setLoading(true);
    setLoadError('');
    try {
      setEvents(await fetchEvents(nextFilters));
    } catch {
      setLoadError('Chưa tải được sự kiện. Vui lòng kiểm tra kết nối rồi thử lại.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void loadEvents({ query: '', category: '' }); }, [loadEvents]);

  async function openEvent(id: string) {
    setSelectedEvent(null);
    setDetailError('');
    setRegistrationMessage('');
    setRegistrationError('');
    try {
      setSelectedEvent(await fetchEvent(id));
    } catch {
      setDetailError('Không tải được thông tin sự kiện. Vui lòng thử lại.');
    }
  }

  async function register(fullName: string, email: string) {
    if (!selectedEvent) return;
    setRegistering(true);
    setRegistrationError('');
    setRegistrationMessage('');
    let registered = false;
    try {
      await submitRegistration(selectedEvent.id, { fullName, email });
      registered = true;
      setRegistrationMessage('Đăng ký thành công! Ban tổ chức sẽ gửi thông tin cần thiết qua email.');
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) setRegistrationError(error.message);
      else setRegistrationError('Chưa gửi được đăng ký. Vui lòng thử lại sau.');
    } finally {
      setRegistering(false);
    }
    if (registered) {
      try {
        const refreshed = await fetchEvent(selectedEvent.id);
        setSelectedEvent(refreshed);
        setEvents((current) => current.map((event) => event.id === refreshed.id ? { ...event, ...refreshed } : event));
      } catch {
      }
    }
  }

  return (
    <div className="site-shell">
      <header className="topbar">
        <a className="brand" href="#home" onClick={() => setSelectedEvent(null)} aria-label="Campus Hub trang chủ">
          <span className="brand-mark" aria-hidden="true">C</span><span>campus<span className="brand-light">hub</span></span>
        </a>
        <nav aria-label="Điều hướng chính"><a href="#events" onClick={() => setSelectedEvent(null)}>Khám phá</a><a href="#about">Về Campus Hub</a></nav>
        <a className="topbar-cta" href="#events" onClick={() => setSelectedEvent(null)}>Tìm sự kiện <span aria-hidden="true">↗</span></a>
      </header>

      <main id="home">
        {selectedEvent ? (
          <EventDetailPage event={selectedEvent} onBack={() => setSelectedEvent(null)} onRegister={register}
            registering={registering} successMessage={registrationMessage} errorMessage={registrationError} />
        ) : detailError ? (
          <section className="content-width state-panel"><p role="alert">{detailError}</p><button className="button button-dark" onClick={() => setDetailError('')}>Quay lại danh sách</button></section>
        ) : (
          <>
            <section className="hero">
              <div className="hero-copy"><p className="eyebrow"><span className="live-dot" /> NHỊP SỐNG CAMPUS</p>
                <h1>Điều hay ho<br />đang <em>chờ bạn.</em></h1>
                <p className="hero-description">Tìm hoạt động mới, gặp những người cùng sở thích và tạo nên câu chuyện đáng nhớ của riêng bạn.</p>
                <a className="button button-light" href="#events">Khám phá sự kiện <span aria-hidden="true">↓</span></a>
              </div>
              <div className="hero-art" aria-label="Minh họa các hoạt động sinh viên" role="img">
                <div className="art-sun" /><div className="art-orbit orbit-one" /><div className="art-orbit orbit-two" />
                <div className="art-note note-one">✳</div><div className="art-note note-two">✦</div>
                <div className="art-card"><span className="art-card-tag">MEET · MAKE · MOVE</span><strong>Make<br />your<br /><i>moment.</i></strong><span className="art-card-bottom">CAMPUS LIFE / 2026</span></div>
                <span className="art-caption">01 — 04 / MÙA SỰ KIỆN</span>
              </div>
              <div className="hero-index">01 <span /> 03</div>
            </section>

            <EventListPage events={events} query={filters.query} category={filters.category} loading={loading} error={loadError}
              onFilter={(next) => { setFilters(next); void loadEvents(next); }} onRetry={() => void loadEvents(filters)} onOpen={openEvent} />
            <section className="about-band" id="about"><div className="content-width about-inner"><p className="eyebrow">CAMPUS HUB / DÀNH CHO BẠN</p><h2>Không chỉ là một<br />sự kiện. Là <em>điểm bắt đầu.</em></h2><p>Campus Hub giúp bạn bắt nhịp với những hoạt động đang diễn ra và tìm thấy cộng đồng của mình.</p></div></section>
          </>
        )}
      </main>
      <footer className="footer"><a className="brand" href="#home"><span className="brand-mark" aria-hidden="true">C</span><span>campus<span className="brand-light">hub</span></span></a><span>Gặp nhau ngoài lớp học. © 2026 Campus Hub</span></footer>
    </div>
  );
}
