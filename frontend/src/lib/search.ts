import type { StaySearchParams } from '../types';

export function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

export function addDaysIso(iso: string, days: number): string {
  const d = new Date(iso + 'T12:00:00');
  d.setDate(d.getDate() + days);
  return d.toISOString().slice(0, 10);
}

export function defaultStaySearch(): StaySearchParams {
  const checkIn = todayIso();
  return {
    city: '',
    checkIn,
    checkOut: addDaysIso(checkIn, 2),
    guests: 2,
  };
}

export function parseStaySearch(params: URLSearchParams): StaySearchParams {
  const defaults = defaultStaySearch();
  const guests = Number(params.get('guests') ?? defaults.guests);
  return {
    city: params.get('city') ?? '',
    checkIn: params.get('checkIn') ?? defaults.checkIn,
    checkOut: params.get('checkOut') ?? defaults.checkOut,
    guests: Number.isFinite(guests) && guests > 0 ? guests : 2,
  };
}

export function buildStaySearchParams(s: StaySearchParams): URLSearchParams {
  const q = new URLSearchParams();
  if (s.city.trim()) q.set('city', s.city.trim());
  q.set('checkIn', s.checkIn);
  q.set('checkOut', s.checkOut);
  q.set('guests', String(s.guests));
  return q;
}

export function staySearchQuery(s: StaySearchParams): string {
  return buildStaySearchParams(s).toString();
}

export function searchResultsPath(s: StaySearchParams): string {
  return `/search?${staySearchQuery(s)}`;
}

export function hotelStayPath(hotelId: number, s: StaySearchParams): string {
  return `/hotels/${hotelId}?${staySearchQuery(s)}`;
}

export function roomDetailPath(
  hotelId: number,
  roomTypeId: number,
  s: StaySearchParams,
): string {
  return `/hotels/${hotelId}/rooms/${roomTypeId}?${staySearchQuery(s)}`;
}
