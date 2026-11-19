import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import { HotelCard } from '../components/HotelCard';
import { StaySearchBar } from '../components/StaySearchBar';
import { PublicRagWidget } from '../components/PublicRagWidget';
import { EmptyState, InlineAlert } from '../components/ui';

export function HomePage() {
  const { data: hotels, isLoading, error } = useQuery({
    queryKey: ['hotels'],
    queryFn: api.getHotels,
  });

  return (
    <div>
      <section className="relative min-h-[78vh] overflow-hidden sm:min-h-[84vh]">
        <img
          src="https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=2000&q=80"
          alt="Sunlit resort pool and palms"
          className="hero-media absolute inset-0 h-full w-full object-cover object-[center_40%]"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-mist via-mist/80 to-mist/35 sm:hidden" />
        <div className="absolute inset-0 hidden bg-[linear-gradient(90deg,rgba(251,252,251,0.94)_0%,rgba(251,252,251,0.86)_34%,rgba(251,252,251,0.28)_62%,rgba(251,252,251,0)_100%)] sm:block" />
        <div className="absolute inset-0 hidden bg-gradient-to-t from-mist/75 via-transparent to-white/10 sm:block" />
        <div className="pointer-events-none absolute -left-16 top-10 h-56 w-56 rounded-full bg-gold/20 blur-3xl" />
        <div className="pointer-events-none absolute bottom-8 right-[18%] h-40 w-40 rounded-full bg-lagoon/25 blur-3xl" />

        <div className="relative mx-auto flex min-h-[78vh] max-w-6xl flex-col justify-end px-4 pb-14 pt-16 sm:min-h-[84vh] sm:pb-20">
          <p className="animate-rise text-[11px] font-semibold uppercase tracking-[0.2em] text-moss">
            Step 1 · Search
          </p>
          <p className="animate-rise mt-3 font-display text-5xl font-bold tracking-tight text-forest sm:text-7xl md:text-8xl">
            Booka
          </p>
          <h1 className="animate-rise-delay mt-4 max-w-xl font-display text-2xl font-semibold leading-snug text-ink sm:text-3xl md:text-[2.15rem]">
            Find stays by dates, then pick a room.
          </h1>
          <p className="animate-rise-delay-2 mt-4 max-w-lg text-base leading-relaxed text-ink/70 sm:text-lg">
            Search available properties, compare rooms, and confirm — pay at the property on
            arrival.
          </p>
          <div className="animate-rise-delay-2 mt-8 max-w-4xl">
            <StaySearchBar />
          </div>
        </div>
      </section>

      <section id="stays" className="mx-auto max-w-6xl space-y-10 px-4 py-16 sm:py-20">
        <div className="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
          <div className="max-w-2xl">
            <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-moss">
              Browse without dates
            </p>
            <h2 className="mt-2 font-display text-3xl font-bold text-ink sm:text-4xl">
              Featured properties
            </h2>
            <p className="mt-3 text-ink/65">
              Prefer dates first? Use search above. Or open a property and set dates on the rooms
              step.
            </p>
          </div>
          <Link to="/search" className="btn-secondary shrink-0 self-start sm:self-auto">
            Search by dates
          </Link>
        </div>

        {isLoading && (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {[0, 1, 2].map((i) => (
              <div
                key={i}
                className="h-80 animate-pulse rounded-2xl border border-sand/70 bg-white/50"
              />
            ))}
          </div>
        )}
        {error && (
          <InlineAlert tone="error">
            Could not load stays right now. Please refresh and try again.
          </InlineAlert>
        )}

        {!isLoading && !error && hotels?.length === 0 && (
          <EmptyState
            title="No properties yet"
            description="Check back soon — new stays are being prepared."
          />
        )}

        {!isLoading && (
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {hotels?.map((hotel, index) => (
              <HotelCard key={hotel.id} hotel={hotel} index={index} />
            ))}
          </div>
        )}
      </section>

      <section
        id="concierge"
        className="border-t border-sand/70 bg-gradient-to-b from-fog/70 to-transparent"
      >
        <div className="mx-auto max-w-6xl px-4 py-16 sm:py-20">
          <div className="mb-8 max-w-xl">
            <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-gold">
              Concierge
            </p>
            <h2 className="mt-2 font-display text-3xl font-bold text-ink sm:text-4xl">
              Need a hand?
            </h2>
            <p className="mt-3 text-ink/65">
              Ask about amenities, policies, room types, or availability — answers use live property
              data when dates are clear.
            </p>
          </div>
          <PublicRagWidget hotelId={null} />
        </div>
      </section>
    </div>
  );
}
