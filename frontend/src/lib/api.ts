import type {
  Hotel,
  AvailableRoomType,
  Booking,
  DashboardStats,
  PageResponse,
  RagChatResponse,
  BlackoutDate,
  RoomType,
} from '../types';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

async function request<T>(
  path: string,
  options: RequestInit = {},
  token?: string | null,
): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(error.message ?? 'Request failed');
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json();
}

export const api = {
  login: (email: string, password: string) =>
    request<{ token: string; userId: number; email: string; fullName: string; role: string; hotelId: number | null }>(
      '/api/v1/auth/login',
      { method: 'POST', body: JSON.stringify({ email, password }) },
    ),

  register: (email: string, password: string, fullName: string) =>
    request<{ token: string; userId: number; email: string; fullName: string; role: string; hotelId: number | null }>(
      '/api/v1/auth/register',
      { method: 'POST', body: JSON.stringify({ email, password, fullName }) },
    ),

  getHotels: () => request<Hotel[]>('/api/v1/hotels'),

  getHotel: (id: number) => request<Hotel>(`/api/v1/hotels/${id}`),

  checkAvailability: (
    hotelId: number,
    body: { checkInDate: string; checkOutDate: string; numGuests: number },
  ) =>
    request<AvailableRoomType[]>(
      `/api/v1/hotels/${hotelId}/availability`,
      { method: 'POST', body: JSON.stringify(body) },
    ),

  createBooking: (body: {
    hotelId: number;
    roomTypeId: number;
    checkInDate: string;
    checkOutDate: string;
    numGuests: number;
    guestEmail: string;
    guestFullName: string;
    guestPhone?: string;
  }) =>
    request<Booking>('/api/v1/bookings', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  getDashboard: (token: string) =>
    request<DashboardStats>('/api/v1/admin/dashboard', {}, token),

  listBookings: (token: string, page = 0) =>
    request<PageResponse<Booking>>(
      `/api/v1/admin/bookings?page=${page}&size=20`,
      {},
      token,
    ),

  updateBookingStatus: (token: string, id: number, status: string) =>
    request<Booking>(
      `/api/v1/admin/bookings/${id}/status`,
      { method: 'PATCH', body: JSON.stringify({ status }) },
      token,
    ),

  publicRagChat: (message: string, hotelId: number | null, history: { role: string; content: string }[]) =>
    request<RagChatResponse>('/api/v1/rag/public/chat', {
      method: 'POST',
      body: JSON.stringify({ message, hotelId, history }),
    }),

  staffRagChat: (token: string, message: string, hotelId: number | null, history: { role: string; content: string }[]) =>
    request<RagChatResponse>(
      '/api/v1/rag/chat',
      { method: 'POST', body: JSON.stringify({ message, hotelId, history }) },
      token,
    ),

  getBlackoutDates: (hotelId: number, from?: string, to?: string) => {
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    const qs = params.toString();
    return request<BlackoutDate[]>(
      `/api/v1/hotels/${hotelId}/blackout-dates${qs ? `?${qs}` : ''}`,
    );
  },

  listAdminBlackoutDates: (token: string) =>
    request<BlackoutDate[]>('/api/v1/admin/blackout-dates', {}, token),

  createBlackoutDate: (
    token: string,
    body: {
      hotelId: number;
      roomTypeId?: number | null;
      startDate: string;
      endDate: string;
      reason: string;
    },
  ) =>
    request<BlackoutDate>(
      '/api/v1/admin/blackout-dates',
      { method: 'POST', body: JSON.stringify(body) },
      token,
    ),

  deleteBlackoutDate: (token: string, id: number) =>
    request<void>(`/api/v1/admin/blackout-dates/${id}`, { method: 'DELETE' }, token),

  getRoomTypes: (hotelId: number) =>
    request<RoomType[]>(`/api/v1/hotels/${hotelId}/room-types`),
};
