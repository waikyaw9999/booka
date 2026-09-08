import { Link } from 'react-router-dom';
import type { Hotel } from '../types';
import { hotelDisplayImage } from '../lib/media';

export function HotelCard({ hotel, index = 0 }: { hotel: Hotel; index?: number }) {
  const image = hotelDisplayImage(hotel, index);

  return (
    <Link
      to={`/hotels/${hotel.id}`}
      className="group block overflow-hidden rounded-2xl border border-sand/80 bg-white/60 backdrop-blur-sm transition duration-300 hover:-translate-y-1 hover:border-lagoon/40"
      style={{ animationDelay: `${index * 80}ms` }}
    >
      <div className="relative h-52 overflow-hidden">
        <img
          src={image}
          alt={hotel.name}
          className="h-full w-full object-cover transition duration-700 group-hover:scale-105"
          loading="lazy"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-ink/70 via-ink/10 to-transparent" />
        <h2 className="absolute bottom-4 left-4 right-4 font-display text-2xl font-bold text-white">
          {hotel.name}
        </h2>
      </div>
      <div className="space-y-2 p-5">
        <p className="line-clamp-2 text-sm leading-relaxed text-ink/70">{hotel.description}</p>
        <p className="text-xs font-medium tracking-wide text-moss uppercase">
          {hotel.city}, {hotel.country}
        </p>
      </div>
    </Link>
  );
}
