export type UserRole = 'GUEST' | 'STAFF' | 'ADMIN';

export interface AuthUser {
  userId: number;
  email: string;
  fullName: string;
  role: UserRole;
  hotelId: number | null;
  token: string;
}

export interface Hotel {
  id: number;
  name: string;
  description: string;
  address: string;
  city: string;
  country: string;
  amenities: string;
  cancellationPolicy: string;
}

export interface RoomType {
  id: number;
  hotelId: number;
  name: string;
  description: string;
  maxOccupancy: number;
  baseRate: number;
  amenities: string;
}

export interface AvailableRoomType {
  roomType: RoomType;
  availableCount: number;
  totalPrice: number;
  nightlyRate: number;
  nights: number;
}

export interface Booking {
  id: number;
  hotelId: number;
  hotelName: string;
  roomId: number;
  roomNumber: string;
  roomTypeName: string;
  guestId: number;
  guestName: string;
  guestEmail: string;
  checkInDate: string;
  checkOutDate: string;
  numGuests: number;
  totalPrice: number;
  status: BookingStatus;
  createdAt: string;
}

export type BookingStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'CHECKED_IN'
  | 'CHECKED_OUT'
  | 'CANCELLED';

export interface DashboardStats {
  totalRooms: number;
  occupiedRooms: number;
  availableRooms: number;
  maintenanceRooms: number;
  upcomingCheckIns: number;
  upcomingCheckOuts: number;
  activeBookings: number;
}

export interface RagCitation {
  documentTitle: string;
  section: string | null;
  hotelId: number | null;
}

export interface AvailabilityOption {
  hotelId: number;
  hotelName: string;
  roomTypeId: number;
  roomTypeName: string;
  availableCount: number;
  totalPrice: number;
  nightlyRate: number;
  nights: number;
  checkInDate: string;
  checkOutDate: string;
}

export interface BlackoutOption {
  id: number;
  hotelId: number;
  roomTypeId: number | null;
  roomTypeName: string;
  startDate: string;
  endDate: string;
  reason: string;
}

export interface PropertySummary {
  totalRooms: number;
  occupiedRooms: number;
  availableRooms: number;
  maintenanceRooms: number;
  upcomingCheckIns: number;
  upcomingCheckOuts: number;
  activeBookings: number;
}

export interface BookingPeriodSummary {
  periodLabel: string;
  fromDate: string;
  toDate: string;
  totalBookings: number;
  activeBookings: number;
  cancelledBookings: number;
  confirmed: number;
  checkedIn: number;
  checkedOut: number;
  pending: number;
}

export interface RagChatResponse {
  answer: string;
  citations: RagCitation[];
  ragAvailable: boolean;
  intent?: string;
  availability?: AvailabilityOption[];
  hotelPath?: string | null;
  blackouts?: BlackoutOption[];
  propertySummary?: PropertySummary | null;
  bookingSummary?: BookingPeriodSummary | null;
}

export interface RagMessage {
  role: 'user' | 'assistant';
  content: string;
  citations?: RagCitation[];
  availability?: AvailabilityOption[];
  hotelPath?: string | null;
  blackouts?: BlackoutOption[];
  propertySummary?: PropertySummary | null;
  bookingSummary?: BookingPeriodSummary | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface BookingDraft {
  hotelId: number | null;
  hotelName: string | null;
  checkInDate: string;
  checkOutDate: string;
  numGuests: number;
  selectedRoomType: AvailableRoomType | null;
  guestEmail: string;
  guestFullName: string;
  guestPhone: string;
}

export interface BlackoutDate {
  id: number;
  hotelId: number;
  roomTypeId: number | null;
  roomTypeName: string | null;
  startDate: string;
  endDate: string;
  reason: string;
  createdAt: string;
}
