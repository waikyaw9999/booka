/** Resolve image URLs for display (absolute https or same-origin /uploads). */
export function resolveMediaUrl(url: string | null | undefined): string | null {
  if (!url) return null;
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  const base = String(import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');
  if (url.startsWith('/')) {
    return `${base}${url}`;
  }
  return `${base}/${url}`;
}

export const FALLBACK_HOTEL_IMAGES = [
  'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1582719508461-905c673771fd?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=1200&q=80',
  'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=1200&q=80',
];

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
