import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import { HotelCard } from '../components/HotelCard';
import { PublicRagWidget } from '../components/PublicRagWidget';
import { EmptyState, InlineAlert } from '../components/ui';
import { HERO_IMAGE } from '../lib/media';

export function HomePage() {
  const { data: hotels, isLoading, error } = useQuery({
    queryKey: ['hotels'],
    queryFn: api.getHotels,
  });

  return (
    <div>
      <section className="relative min-h-[90vh] overflow-hidden text-white">
        <img
          src={HERO_IMAGE}
          alt="Hotel lobby atmosphere"
          className="hero-media absolute inset-0 h-full w-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-ink/78 via-ink/42 to-ink/15" />
        <div className="absolute inset-0 bg-gradient-to-t from-ink/45 via-transparent to-ink/18" />

        <div className="relative mx-auto flex min-h-[90vh] max-w-6xl flex-col justify-end px-4 pb-16 pt-28 sm:pb-24">
          <p className="animate-rise font-display text-5xl font-bold tracking-tight sm:text-7xl md:text-8xl">
            Booka
          </p>
          <h1 className="animate-rise-delay mt-5 max-w-xl font-display text-2xl font-semibold leading-snug sm:text-3xl md:text-[2.15rem]">
            Stays that feel settled the moment you arrive.
          </h1>
          <p className="animate-rise-delay-2 mt-4 max-w-lg text-base leading-relaxed text-white/78 sm:text-lg">
            Browse calm properties across Myanmar, check live availability, and confirm without the
            noise — pay at the property on arrival.
          </p>
          <div className="animate-rise-delay-2 mt-9 flex flex-wrap gap-3">
            <a href="#stays" className="btn-primary">
              Explore stays
            </a>
            <a
              href="#concierge"
              className="inline-flex items-center justify-center rounded-xl border border-white/30 bg-white/10 px-5 py-2.5 text-sm font-semibold text-white backdrop-blur-sm transition hover:bg-white/18"
            >
              Ask concierge
            </a>
          </div>
        </div>
      </section>

      <section id="stays" className="mx-auto max-w-6xl space-y-10 px-4 py-16 sm:py-20">
        <div className="flex flex-col gap-6 sm:flex-row sm:items-end sm:justify-between">
          <div className="max-w-2xl">
            <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-moss">
              Property discovery
            </p>
            <h2 className="mt-2 font-display text-3xl font-bold text-ink sm:text-4xl">
              Featured stays
            </h2>
            <p className="mt-3 text-ink/65">
              Handpicked properties with transparent rates, live inventory, and clear cancellation
              policies.
            </p>
          </div>
          <p className="max-w-xs text-sm leading-relaxed text-ink/50 sm:text-right">
            Live inventory · Confirm now, pay at property · Concierge help
          </p>
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
