export type CampusEvent = {
  id: string; title: string; category: string; description: string; location: string;
  startsAt: string; endsAt: string; capacity: number; registeredCount: number;
  remainingSeats: number; registrationOpen: boolean;
};

export class ApiError extends Error {
  constructor(message: string, readonly status: number) { super(message); this.name = 'ApiError'; }
}

async function requestJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, { ...init, headers: { 'Content-Type': 'application/json', ...init?.headers } });
  const body = await response.json().catch(() => null) as { message?: string } | null;
  if (!response.ok) throw new ApiError(body?.message || 'Yêu cầu chưa thể thực hiện.', response.status);
  return body as T;
}

export function fetchEvents(filters: { query: string; category: string }): Promise<CampusEvent[]> {
  const params = new URLSearchParams();
  if (filters.query.trim()) params.set('query', filters.query.trim());
  if (filters.category) params.set('category', filters.category);
  const suffix = params.size ? `?${params.toString()}` : '';
  return requestJson(`/api/events${suffix}`);
}

export function fetchEvent(id: string): Promise<CampusEvent> { return requestJson(`/api/events/${id}`); }

export function submitRegistration(id: string, data: { fullName: string; email: string }): Promise<{ registrationId: string; status: string }> {
  return requestJson(`/api/events/${id}/registrations`, { method: 'POST', body: JSON.stringify(data) });
}
