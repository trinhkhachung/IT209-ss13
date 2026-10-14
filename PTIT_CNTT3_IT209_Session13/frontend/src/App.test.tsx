import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';

const clubFair = {
  id: '11111111-1111-4111-8111-111111111111',
  title: 'Ngày hội câu lạc bộ sinh viên',
  category: 'Cộng đồng',
  description: 'Khám phá hoạt động và kết nối bạn bè.',
  location: 'Sân trung tâm',
  startsAt: '2026-11-21T08:00:00+07:00',
  endsAt: '2026-11-21T16:00:00+07:00',
  capacity: 120,
  registeredCount: 0,
  remainingSeats: 120,
  registrationOpen: true,
};

const hackathon = {
  id: '22222222-2222-4222-8222-222222222222',
  title: 'Hackathon Sáng tạo Campus',
  category: 'Công nghệ',
  description: 'Xây dựng ý tưởng và nguyên mẫu số.',
  location: 'Phòng hội thảo A2',
  startsAt: '2026-12-10T09:00:00+07:00',
  endsAt: '2026-12-10T18:00:00+07:00',
  capacity: 40,
  registeredCount: 0,
  remainingSeats: 40,
  registrationOpen: true,
};

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('Campus Hub event portal', () => {
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
  });

  it('loads and displays upcoming events with remaining seats', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse([clubFair, hackathon]));

    render(<App />);

    expect(await screen.findByRole('heading', { name: clubFair.title })).toBeInTheDocument();
    expect(screen.getByText('120 chỗ còn lại')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: hackathon.title })).toBeInTheDocument();
  });

  it('combines keyword and category filters in the list request', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse([clubFair, hackathon]))
      .mockResolvedValueOnce(jsonResponse([hackathon]));

    render(<App />);
    await screen.findByRole('heading', { name: clubFair.title });
    fireEvent.change(screen.getByLabelText('Từ khóa'), { target: { value: 'hackathon' } });
    fireEvent.change(screen.getByLabelText('Danh mục'), { target: { value: 'Công nghệ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Tìm sự kiện' }));

    expect(await screen.findByRole('heading', { name: hackathon.title })).toBeInTheDocument();
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    const requestUrl = new URL(String(fetchMock.mock.calls[1][0]), 'http://localhost');
    expect(requestUrl.pathname).toBe('/api/events');
    expect(requestUrl.searchParams.get('query')).toBe('hackathon');
    expect(requestUrl.searchParams.get('category')).toBe('Công nghệ');
  });

  it('registers from event details and confirms without echoing the email', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse([clubFair]))
      .mockResolvedValueOnce(jsonResponse(clubFair))
      .mockResolvedValueOnce(jsonResponse({ registrationId: '33333333-3333-4333-8333-333333333333', status: 'CONFIRMED' }, 201));

    render(<App />);
    await screen.findByRole('heading', { name: clubFair.title });
    fireEvent.click(screen.getByRole('button', { name: `Xem chi tiết ${clubFair.title}` }));
    await screen.findByLabelText('Họ và tên');
    fireEvent.change(screen.getByLabelText('Họ và tên'), { target: { value: 'Nguyễn An' } });
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'an@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký tham gia' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Đăng ký thành công');
    expect(screen.getByRole('status')).not.toHaveTextContent('an@example.com');
    expect(fetchMock.mock.calls[2][1]).toMatchObject({ method: 'POST' });
    expect(JSON.parse(String(fetchMock.mock.calls[2][1].body))).toEqual({
      fullName: 'Nguyễn An',
      email: 'an@example.com',
    });
  });

  it('shows a useful message when an event is full', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse([clubFair]))
      .mockResolvedValueOnce(jsonResponse(clubFair))
      .mockResolvedValueOnce(jsonResponse({ status: 409, code: 'EVENT_FULL', message: 'Sự kiện đã hết chỗ.' }, 409));

    render(<App />);
    await screen.findByRole('heading', { name: clubFair.title });
    fireEvent.click(screen.getByRole('button', { name: `Xem chi tiết ${clubFair.title}` }));
    await screen.findByLabelText('Họ và tên');
    fireEvent.change(screen.getByLabelText('Họ và tên'), { target: { value: 'Nguyễn An' } });
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'an@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký tham gia' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Sự kiện đã hết chỗ.');
  });

  it('shows an empty state when there are no upcoming events', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse([]));

    render(<App />);

    expect(await screen.findByText('Chưa có sự kiện phù hợp')).toBeInTheDocument();
  });

  it('shows a retryable error when events cannot be loaded', async () => {
    fetchMock.mockRejectedValueOnce(new Error('network unavailable'));

    render(<App />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Chưa tải được sự kiện');
    expect(screen.getByRole('button', { name: 'Thử lại' })).toBeInTheDocument();
  });
});
