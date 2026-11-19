import { Link, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api, getErrorMessage } from '../lib/api';
import { hotelDisplayImage } from '../lib/media';
import { money } from '../lib/format';
import { hotelStayPath, parseStaySearch } from '../lib/search';
import { StaySearchBar } from '../components/StaySearchBar';
import { EmptyState, InlineAlert } from '../components/ui';

export function SearchResultsPage() {
  const [searchParams] = useSearchParams();
  const stay = parseStaySearch(searchParams);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['hotel-search', stay.city, stay.checkIn, stay.checkOut, stay.guests],
    queryFn: () =>
      api.searchHotels({
        city: stay.city || undefined,
        checkInDate: stay.checkIn,
        checkOutDate: stay.checkOut,
        numGuests: stay.guests,
      }),
    enabled: !!stay.checkIn && !!stay.checkOut && stay.checkOut > stay.checkIn,
  });

  return (
    <div className="mx-auto max-w-6xl space-y-8 px-4 py-8 sm:py-12">
      <div className="space-y-3">
        <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-moss">
          Step 2 · Properties
        </p>
        <h1 className="font-display text-3xl font-bold text-ink sm:text-4xl">
          Available stays
        </h1>
        <p className="text-ink/60">
          {stay.checkIn} → {stay.checkOut}
          {stay.city ? ` · ${stay.city}` : ''} · {stay.guests} guest
          {stay.guests === 1 ? '' : 's'}
        </p>
      </div>

      <StaySearchBar
        key={`${stay.city}-${stay.checkIn}-${stay.checkOut}-${stay.guests}`}
        initial={stay}
        compact
      />

      {isLoading && (
        <div className="space-y-4">
          {[0, 1, 2].map((i) => (
            <div key={i} className="h-40 animate-pulse rounded-2xl border border-sand/70 bg-white/60" />
          ))}
        </div>
      )}

      {isError && (
        <InlineAlert tone="error">
          {getErrorMessage(error, 'Could not search stays. Please try again.')}
        </InlineAlert>
      )}

      {!isLoading && !isError && data?.length === 0 && (
        <EmptyState
          title="No properties for these dates"
          description="Try different dates, fewer guests, or clear the destination filter."
        />
      )}

      {!isLoading && data && data.length > 0 && (
        <ul className="space-y-4">
          {data.map((hit, index) => {
            const image = hotelDisplayImage(hit.hotel, index);
            return (
              <li key={hit.hotel.id}>
                <Link
                  to={hotelStayPath(hit.hotel.id, stay)}
                  className="group flex flex-col overflow-hidden rounded-2xl border border-sand/80 bg-white/80 transition hover:border-lagoon/40 hover:shadow-[0_18px_40px_-28px_rgba(14,31,26,0.45)] sm:flex-row"
                >
                  <div className="relative h-48 shrink-0 sm:h-auto sm:w-64 md:w-72">
                    <img
                      src={image}
                      alt={hit.hotel.name}
                      className="h-full w-full object-cover transition duration-500 group-hover:scale-[1.03]"
                      loading="lazy"
                    />
                  </div>
                  <div className="flex flex-1 flex-col justify-between gap-4 p-5 sm:p-6">
                    <div>
                      <p className="text-[10px] font-semibold uppercase tracking-[0.14em] text-moss">
                        {hit.hotel.city}, {hit.hotel.country}
                      </p>
                      <h2 className="mt-1 font-display text-2xl font-bold text-ink">
                        {hit.hotel.name}
                      </h2>
                      <p className="mt-2 line-clamp-2 text-sm text-ink/65">{hit.hotel.description}</p>
                      <p className="mt-3 text-xs text-ink/50">
                        {hit.availableRoomTypeCount} room type
                        {hit.availableRoomTypeCount === 1 ? '' : 's'} · {hit.totalAvailableRooms}{' '}
                        room
                        {hit.totalAvailableRooms === 1 ? '' : 's'} free
                      </p>
                    </div>
                    <div className="flex flex-wrap items-end justify-between gap-3">
                      <div>
                        <p className="text-[10px] font-semibold uppercase tracking-[0.12em] text-ink/45">
                          From / night
                        </p>
                        <p className="font-display text-2xl font-bold text-forest">
                          {money(hit.fromNightlyRate)}
                        </p>
                        <p className="text-xs text-ink/50">
                          Stay from {money(hit.fromStaySubtotal)} · {hit.nights} night
                          {hit.nights === 1 ? '' : 's'}
                        </p>
                      </div>
                      <span className="btn-primary pointer-events-none">View rooms</span>
                    </div>
                  </div>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
