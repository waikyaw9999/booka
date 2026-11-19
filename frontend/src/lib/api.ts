import type {
  Hotel,
  AvailableRoomType,
  Booking,
  DashboardStats,
  PageResponse,
  RagChatResponse,
  BlackoutDate,
  RoomType,
  RoomBoardItem,
  MaintenanceTicket,
  Folio,
  CheckoutSession,
  GuestSummary,
  GuestProfile,
  VipTier,
  NightAuditPreview,
  NightAuditRecord,
  GuestRequestItem,
  GuestRequestCategory,
  GuestRequestStatus,
  PaymentMethod,
  HousekeepingStatus,
  TicketPriority,
  TicketStatus,
  FolioLineType,
  PropertyImage,
} from '../types';
import { normalizeHttpError, normalizeNetworkError } from './errors';

export { getErrorMessage, AppError } from './errors';

const API_BASE = String(import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

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

  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  } catch (err) {
    throw normalizeNetworkError(err);
  }

  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: response.statusText }));
    throw normalizeHttpError(response.status, body, response.statusText);
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

  registerProperty: (body: {
    hotelName: string;
    description?: string;
    address: string;
    city: string;
    country: string;
    amenities?: string;
    cancellationPolicy?: string;
    managerEmail: string;
    managerPassword: string;
    managerFullName: string;
  }) =>
    request<{ token: string; userId: number; email: string; fullName: string; role: string; hotelId: number | null }>(
      '/api/v1/auth/register-property',
      { method: 'POST', body: JSON.stringify(body) },
    ),

  listStaff: (token: string) =>
    request<
      { id: number; email: string; fullName: string; role: string; hotelId: number | null }[]
    >('/api/v1/admin/staff', {}, token),

  inviteStaff: (
    token: string,
    body: { email: string; password: string; fullName: string },
  ) =>
    request<{ id: number; email: string; fullName: string; role: string; hotelId: number | null }>(
      '/api/v1/admin/staff',
      { method: 'POST', body: JSON.stringify(body) },
      token,
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

  getBooking: (id: number) => request<Booking>(`/api/v1/bookings/${id}`),

  getBookingFolio: (id: number) => request<Folio>(`/api/v1/bookings/${id}/folio`),

  createCheckoutSession: (bookingId: number) =>
    request<CheckoutSession>(`/api/v1/bookings/${bookingId}/checkout-session`, {
      method: 'POST',
    }),

  getDashboard: (token: string) =>
    request<DashboardStats>('/api/v1/admin/dashboard', {}, token),

  listBookings: (token: string, page = 0) =>
    request<PageResponse<Booking>>(
      `/api/v1/admin/bookings?page=${page}&size=100`,
      {},
      token,
    ),

  getBookingByCode: (token: string, code: string) =>
    request<Booking>(
      `/api/v1/admin/bookings/by-code/${encodeURIComponent(code.trim())}`,
      {},
      token,
    ),

  updateBookingStatus: (token: string, id: number, status: string) =>
    request<Booking>(
      `/api/v1/admin/bookings/${id}/status`,
      { method: 'PATCH', body: JSON.stringify({ status }) },
      token,
    ),

  checkInBooking: (token: string, id: number, force = false) =>
    request<Booking>(
      `/api/v1/admin/bookings/${id}/check-in`,
      { method: 'POST', body: JSON.stringify({ force }) },
      token,
    ),

  checkOutBooking: (token: string, id: number) =>
    request<Booking>(`/api/v1/admin/bookings/${id}/check-out`, { method: 'POST' }, token),

  assignRoom: (token: string, bookingId: number, roomId: number) =>
    request<Booking>(
      `/api/v1/admin/bookings/${bookingId}/assign-room`,
      { method: 'POST', body: JSON.stringify({ roomId }) },
      token,
    ),

  createWalkIn: (
    token: string,
    body: {
      hotelId: number;
      roomTypeId: number;
      roomId?: number | null;
      checkInDate: string;
      checkOutDate: string;
      numGuests: number;
      guestEmail: string;
      guestFullName: string;
      guestPhone?: string;
    },
  ) =>
    request<Booking>(
      '/api/v1/admin/reservations/walk-in',
      { method: 'POST', body: JSON.stringify(body) },
      token,
    ),

  listRoomBoard: (token: string) =>
    request<RoomBoardItem[]>('/api/v1/admin/rooms', {}, token),

  updateHousekeeping: (
    token: string,
    roomId: number,
    housekeepingStatus: HousekeepingStatus,
    force = false,
  ) =>
    request<RoomBoardItem>(
      `/api/v1/admin/rooms/${roomId}/housekeeping`,
      { method: 'PATCH', body: JSON.stringify({ housekeepingStatus, force }) },
      token,
    ),

  listMaintenanceTickets: (token: string) =>
    request<MaintenanceTicket[]>('/api/v1/admin/maintenance-tickets', {}, token),

  createMaintenanceTicket: (
    token: string,
    body: {
      hotelId: number;
      roomId: number;
      title: string;
      description?: string;
      priority: TicketPriority;
      reportedBy?: string;
      markRoomOutOfOrder?: boolean;
    },
  ) =>
    request<MaintenanceTicket>(
      '/api/v1/admin/maintenance-tickets',
      { method: 'POST', body: JSON.stringify(body) },
      token,
    ),

  updateMaintenanceTicket: (
    token: string,
    id: number,
    body: { status?: TicketStatus; priority?: TicketPriority; description?: string },
  ) =>
    request<MaintenanceTicket>(
      `/api/v1/admin/maintenance-tickets/${id}`,
      { method: 'PATCH', body: JSON.stringify(body) },
      token,
    ),

  getAdminFolio: (token: string, bookingId: number) =>
    request<Folio>(`/api/v1/admin/folios/${bookingId}`, {}, token),

  postFolioPayment: (
    token: string,
    bookingId: number,
    body: { amount: number; method: PaymentMethod; reference?: string },
  ) =>
    request<Folio>(
      `/api/v1/admin/folios/${bookingId}/payments`,
      { method: 'POST', body: JSON.stringify(body) },
      token,
    ),

  markNoShow: (token: string, id: number, retainOneNight: boolean) =>
    request<Booking>(
      `/api/v1/admin/bookings/${id}/no-show`,
      { method: 'POST', body: JSON.stringify({ retainOneNight }) },
      token,
    ),

  searchGuests: (token: string, q: string) =>
    request<GuestSummary[]>(
      `/api/v1/admin/guests?q=${encodeURIComponent(q)}`,
      {},
      token,
    ),

  getGuest: (token: string, id: number) =>
    request<GuestProfile>(`/api/v1/admin/guests/${id}`, {}, token),

  updateGuest: (
    token: string,
    id: number,
    body: { nationality: string; vipTier: VipTier; notes: string; blacklisted: boolean },
  ) =>
    request<GuestProfile>(
      `/api/v1/admin/guests/${id}`,
      { method: 'PATCH', body: JSON.stringify(body) },
      token,
    ),

  listGuestRequests: (token: string) =>
    request<GuestRequestItem[]>('/api/v1/admin/guest-requests', {}, token),

  createGuestRequest: (
    token: string,
    body: { bookingId: number; category: GuestRequestCategory; note: string },
  ) =>
    request<GuestRequestItem>(
      '/api/v1/admin/guest-requests',
      { method: 'POST', body: JSON.stringify(body) },
      token,
    ),

  updateGuestRequest: (token: string, id: number, status: GuestRequestStatus) =>
    request<GuestRequestItem>(
      `/api/v1/admin/guest-requests/${id}`,
      { method: 'PATCH', body: JSON.stringify({ status }) },
      token,
    ),

  getNightAudit: (token: string) =>
    request<NightAuditPreview>('/api/v1/admin/night-audit', {}, token),

  closeNightAudit: (token: string, body: { note?: string; retainNoShowNight: boolean }) =>
    request<NightAuditRecord>(
      '/api/v1/admin/night-audit/close',
      { method: 'POST', body: JSON.stringify(body) },
      token,
    ),

  postFolioCharge: (
    token: string,
    bookingId: number,
    body: { type: FolioLineType; description: string; amount: number; externalRef?: string },
  ) =>
    request<Folio>(
      `/api/v1/admin/folios/${bookingId}/charges`,
      { method: 'POST', body: JSON.stringify(body) },
      token,
    ),

  publicRagChat: (message: string, hotelId: number | null, history: { role: string; content: string }[]) =>
    request<RagChatResponse>('/api/v1/rag/public/chat', {
      method: 'POST',
      body: JSON.stringify({ message, hotelId, history }),
    }),

  staffRagChat: (
    token: string,
    message: string,
    hotelId: number | null,
    history: { role: string; content: string }[],
  ) =>
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

  updateHotel: (
    token: string,
    id: number,
    body: { amenities?: string; cancellationPolicy?: string; description?: string },
  ) =>
    request<Hotel>(`/api/v1/admin/hotels/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(body),
    }, token),

  updateRoomType: (
    token: string,
    id: number,
    body: {
      name?: string;
      description?: string;
      maxOccupancy?: number;
      baseRate?: number;
      amenities?: string;
    },
  ) =>
    request<RoomType>(`/api/v1/admin/room-types/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(body),
    }, token),

  createRoom: (
    token: string,
    body: {
      roomTypeId: number;
      roomNumber: string;
      floor?: number | null;
      notes?: string | null;
    },
  ) =>
    request<{
      id: number;
      roomTypeId: number;
      roomNumber: string;
      status: string;
      floor: number | null;
      notes: string | null;
    }>('/api/v1/admin/rooms', {
      method: 'POST',
      body: JSON.stringify(body),
    }, token),

  listHotelImages: (token: string, hotelId: number) =>
    request<PropertyImage[]>(`/api/v1/admin/hotels/${hotelId}/images`, {}, token),

  addHotelImage: (
    token: string,
    hotelId: number,
    body: { url: string; caption?: string | null; sortOrder?: number; primary?: boolean },
  ) =>
    request<PropertyImage>(`/api/v1/admin/hotels/${hotelId}/images`, {
      method: 'POST',
      body: JSON.stringify(body),
    }, token),

  deleteHotelImage: (token: string, hotelId: number, imageId: number) =>
    request<void>(`/api/v1/admin/hotels/${hotelId}/images/${imageId}`, { method: 'DELETE' }, token),

  setPrimaryHotelImage: (token: string, hotelId: number, imageId: number) =>
    request<PropertyImage>(
      `/api/v1/admin/hotels/${hotelId}/images/${imageId}/primary`,
      { method: 'POST' },
      token,
    ),

  uploadHotelImage: (
    token: string,
    hotelId: number,
    file: File,
    caption?: string,
    primary?: boolean,
  ) => uploadImage(`/api/v1/admin/hotels/${hotelId}/images/upload`, token, file, caption, primary),

  listRoomImages: (token: string, roomId: number) =>
    request<PropertyImage[]>(`/api/v1/admin/rooms/${roomId}/images`, {}, token),

  addRoomImage: (
    token: string,
    roomId: number,
    body: { url: string; caption?: string | null; sortOrder?: number; primary?: boolean },
  ) =>
    request<PropertyImage>(`/api/v1/admin/rooms/${roomId}/images`, {
      method: 'POST',
      body: JSON.stringify(body),
    }, token),

  deleteRoomImage: (token: string, roomId: number, imageId: number) =>
    request<void>(`/api/v1/admin/rooms/${roomId}/images/${imageId}`, { method: 'DELETE' }, token),

  setPrimaryRoomImage: (token: string, roomId: number, imageId: number) =>
    request<PropertyImage>(
      `/api/v1/admin/rooms/${roomId}/images/${imageId}/primary`,
      { method: 'POST' },
      token,
    ),

  uploadRoomImage: (
    token: string,
    roomId: number,
    file: File,
    caption?: string,
    primary?: boolean,
  ) => uploadImage(`/api/v1/admin/rooms/${roomId}/images/upload`, token, file, caption, primary),
};

async function uploadImage(
  path: string,
  token: string,
  file: File,
  caption?: string,
  primary?: boolean,
): Promise<PropertyImage> {
  const form = new FormData();
  form.append('file', file);
  if (caption) form.append('caption', caption);
  if (primary != null) form.append('primary', String(primary));

  const headers: Record<string, string> = { Authorization: `Bearer ${token}` };
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, { method: 'POST', headers, body: form });
  } catch (err) {
    throw normalizeNetworkError(err);
  }
  if (!response.ok) {
    const body = await response.json().catch(() => ({ message: response.statusText }));
    throw normalizeHttpError(response.status, body, response.statusText);
  }
  return response.json();
}
