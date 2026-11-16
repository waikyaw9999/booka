/** Resolve image URLs for display (absolute https, SPA /media, or API /uploads). */
export function resolveMediaUrl(url: string | null | undefined): string | null {
  if (!url) return null;
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  // Bundled guest/demo assets live in frontend/public/media — same origin as the SPA.
  if (url.startsWith('/media/')) {
    return url;
  }
  const base = String(import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');
  if (url.startsWith('/')) {
    return `${base}${url}`;
  }
  return `${base}/${url}`;
}

export const FALLBACK_HOTEL_IMAGES = [
  '/media/hotels/grand-lobby.jpg',
  '/media/hotels/grand-pool.jpg',
  '/media/hotels/harbour-pool.jpg',
  '/media/hotels/harbour-facade.jpg',
  '/media/hotels/mandalay-facade.jpg',
];

export const FALLBACK_ROOM_IMAGES = [
  '/media/rooms/standard.jpg',
  '/media/rooms/deluxe.jpg',
  '/media/rooms/harbour-twin.jpg',
  '/media/rooms/skyline-suite.jpg',
  '/media/rooms/lake-cabin.jpg',
];

export const HERO_IMAGE = '/media/hero.jpg';

export function hotelDisplayImage(
  hotel: { primaryImageUrl?: string | null; images?: { url: string; primary?: boolean }[] },
  index = 0,
): string {
  const primary =
    resolveMediaUrl(hotel.primaryImageUrl)
    ?? resolveMediaUrl(hotel.images?.find((i) => i.primary)?.url)
    ?? resolveMediaUrl(hotel.images?.[0]?.url);
  if (primary) return primary;
  return FALLBACK_HOTEL_IMAGES[index % FALLBACK_HOTEL_IMAGES.length];
}

export function roomDisplayImage(
  room: { primaryImageUrl?: string | null },
  index = 0,
): string {
  return (
    resolveMediaUrl(room.primaryImageUrl)
    ?? FALLBACK_ROOM_IMAGES[index % FALLBACK_ROOM_IMAGES.length]
  );
}
