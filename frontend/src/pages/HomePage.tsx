import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import { HotelCard } from '../components/HotelCard';
import { PublicRagWidget } from '../components/PublicRagWidget';

export function HomePage() {
  const { data: hotels, isLoading, error } = useQuery({
    queryKey: ['hotels'],
    queryFn: api.getHotels,
  });

  return (
    <div>
      <section className="relative min-h-[88vh] overflow-hidden text-white">
        <img
          src="https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=2000&q=80"
          alt="Hotel lobby atmosphere"
          className="hero-media absolute inset-0 h-full w-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-ink/85 via-ink/55 to-ink/25" />
        <div className="absolute inset-0 bg-gradient-to-t from-ink/50 via-transparent to-ink/20" />

        <div className="relative mx-auto flex min-h-[88vh] max-w-6xl flex-col justify-end px-4 pb-16 pt-28 sm:pb-24">
          <p className="animate-rise font-display text-5xl font-bold tracking-tight sm:text-7xl md:text-8xl">
            Booka
          </p>
          <h1 className="animate-rise-delay mt-4 max-w-xl font-display text-2xl font-semibold leading-tight sm:text-3xl">
            Stays that feel settled the moment you arrive.
          </h1>
          <p className="animate-rise-delay-2 mt-4 max-w-md text-base text-white/80 sm:text-lg">
            Browse calm properties, check live availability, and book without the noise.
          </p>
          <div className="animate-rise-delay-2 mt-8">
            <a href="#stays" className="btn-primary">
              Explore stays
            </a>
          </div>
        </div>
      </section>

      <section id="stays" className="mx-auto max-w-6xl px-4 py-16 space-y-10">
        <div className="max-w-2xl">
          <h2 className="font-display text-3xl font-bold text-ink sm:text-4xl">Featured stays</h2>
          <p className="mt-3 text-ink/65">
            Handpicked properties with transparent rates and clear cancellation policies.
          </p>
        </div>

        {isLoading && <p className="text-ink/50">Loading stays...</p>}
        {error && (
          <p className="text-red-700">
            Could not load hotels. Make sure the backend is running on port 8080.
          </p>
        )}

        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {hotels?.map((hotel, index) => (
            <HotelCard key={hotel.id} hotel={hotel} index={index} />
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-20">
        <div className="mb-6 max-w-xl">
          <h2 className="font-display text-3xl font-bold text-ink">Need a hand?</h2>
          <p className="mt-2 text-ink/65">
            Ask about amenities, policies, room types, or month availability — answers use live data across all properties.
          </p>
        </div>
        <PublicRagWidget hotelId={null} />
      </section>
    </div>
  );
}
