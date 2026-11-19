import { Link } from 'react-router-dom';
import type { Hotel } from '../types';
import { hotelDisplayImage } from '../lib/media';

export function HotelCard({ hotel, index = 0 }: { hotel: Hotel; index?: number }) {
  const image = hotelDisplayImage(hotel, index);

  return (
    <Link
      to={`/hotels/${hotel.id}`}
      className="group animate-rise block overflow-hidden rounded-2xl border border-sand/70 bg-white/75 shadow-[0_18px_40px_-28px_rgba(14,31,26,0.45)] transition duration-300 hover:-translate-y-1.5 hover:border-lagoon/35 hover:shadow-[0_22px_48px_-24px_rgba(14,31,26,0.5)]"
      style={{ animationDelay: `${index * 70}ms` }}
    >
      <div className="relative h-56 overflow-hidden sm:h-60">
        <img
          src={image}
          alt={hotel.name}
          className="h-full w-full object-cover transition duration-700 group-hover:scale-[1.04]"
          loading="lazy"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-ink/80 via-ink/20 to-transparent" />
        <div className="absolute bottom-0 left-0 right-0 p-5">
          <p className="text-[10px] font-semibold uppercase tracking-[0.16em] text-gold/95">
            {hotel.city}, {hotel.country}
          </p>
          <h2 className="mt-1 font-display text-2xl font-bold leading-tight text-white">
            {hotel.name}
          </h2>
        </div>
      </div>
      <div className="space-y-3 p-5">
        <p className="line-clamp-2 text-sm leading-relaxed text-ink/70">{hotel.description}</p>
        <span className="inline-flex text-xs font-semibold text-moss transition group-hover:text-forest">
          View rooms →
        </span>
      </div>
    </Link>
  );
}
