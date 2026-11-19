import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api, getErrorMessage } from '../lib/api';
import { money } from '../lib/format';
import { hotelDisplayImage, resolveMediaUrl } from '../lib/media';
import {
  buildStaySearchParams,
  parseStaySearch,
  roomDetailPath,
  searchResultsPath,
} from '../lib/search';
import { StaySearchBar } from '../components/StaySearchBar';
import { BlackoutNotice } from '../components/BlackoutNotice';
import { EmptyState, InlineAlert } from '../components/ui';
import { PublicRagWidget } from '../components/PublicRagWidget';

export function HotelDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const hotelId = Number(id);
  const stay = parseStaySearch(searchParams);
  const hasStay = !!stay.checkIn && !!stay.checkOut && stay.checkOut > stay.checkIn;

  const {
    data: hotel,
    isLoading,
    isError: hotelLoadError,
    error: hotelError,
  } = useQuery({
    queryKey: ['hotel', hotelId],
    queryFn: () => api.getHotel(hotelId),
    enabled: !Number.isNaN(hotelId),
  });

  const { data: blackouts } = useQuery({
    queryKey: ['blackouts', hotelId],
    queryFn: () => api.getBlackoutDates(hotelId),
    enabled: !Number.isNaN(hotelId),
  });

  const {
    data: availability,
    isLoading: availLoading,
    isError: availError,
    error: availErr,
  } = useQuery({
    queryKey: ['availability', hotelId, stay.checkIn, stay.checkOut, stay.guests],
    queryFn: () =>
      api.checkAvailability(hotelId, {
        checkInDate: stay.checkIn,
        checkOutDate: stay.checkOut,
        numGuests: stay.guests,
      }),
    enabled: !Number.isNaN(hotelId) && hasStay,
  });

  if (isLoading) {
    return <p className="py-24 text-center text-ink/50">Loading property…</p>;
  }
  if (hotelLoadError || !hotel) {
    return (
      <div className="mx-auto max-w-lg px-4 py-24">
        <EmptyState
          title="Property not found"
          description={getErrorMessage(hotelError, 'This stay may have been removed.')}
          action={
            <Link to={searchResultsPath(stay)} className="btn-primary inline-flex">
              Back to search
            </Link>
          }
        />
      </div>
    );
  }

  const heroImage = hotelDisplayImage(hotel);
  const gallery = (hotel.images ?? [])
    .map((img) => ({ ...img, resolved: resolveMediaUrl(img.url) }))
    .filter((img) => img.resolved);

  return (
    <div>
      <section className="relative h-[36vh] min-h-[240px] overflow-hidden">
        <img
          src={heroImage}
          alt={hotel.name}
          className="absolute inset-0 h-full w-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-ink via-ink/40 to-ink/20" />
        <div className="relative mx-auto flex h-full max-w-5xl flex-col justify-end px-4 pb-8">
          <Link
            to={searchResultsPath(stay)}
            className="mb-3 text-sm text-white/75 hover:text-white"
          >
            ← Available properties
          </Link>
          <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-white/70">
            Step 3 · Rooms
          </p>
          <h1 className="font-display text-3xl font-bold text-white sm:text-4xl">{hotel.name}</h1>
          <p className="mt-1 text-white/75">
            {hotel.city}, {hotel.country}
          </p>
        </div>
      </section>

      <div className="mx-auto max-w-5xl space-y-8 px-4 py-8 sm:py-10">
        <StaySearchBar
          key={`${stay.city}-${stay.checkIn}-${stay.checkOut}-${stay.guests}`}
          initial={stay}
          compact
          onSubmit={(next) => setSearchParams(buildStaySearchParams(next))}
        />

        {gallery.length > 1 && (
          <section className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
            {gallery.map((img) => (
              <figure key={img.id} className="overflow-hidden rounded-xl">
                <img
                  src={img.resolved!}
                  alt={img.caption ?? hotel.name}
                  className="h-28 w-full object-cover sm:h-32"
                  loading="lazy"
                />
              </figure>
            ))}
          </section>
        )}

        <section className="space-y-2">
          <p className="max-w-2xl leading-relaxed text-ink/75">{hotel.description}</p>
          <p className="text-sm text-ink/55">{hotel.address}</p>
          <div className="flex flex-wrap gap-2 pt-1">
            {(hotel.amenities ?? '')
              .split(',')
              .filter(Boolean)
              .map((item) => (
                <span
                  key={item}
                  className="rounded-lg bg-sand/70 px-3 py-1 text-xs font-medium text-forest"
                >
                  {item.trim()}
                </span>
              ))}
          </div>
        </section>

        {blackouts && <BlackoutNotice blackouts={blackouts} />}

        <section className="space-y-4">
          <div className="flex flex-wrap items-end justify-between gap-3">
            <h2 className="font-display text-2xl font-bold text-ink">Available rooms</h2>
            {hasStay && (
              <p className="text-xs text-ink/45">
                {stay.checkIn} → {stay.checkOut} · {stay.guests} guest
                {stay.guests === 1 ? '' : 's'}
              </p>
            )}
          </div>

          {!hasStay && (
            <InlineAlert tone="warning">
              Choose check-in and check-out dates above to see rooms and prices.
            </InlineAlert>
          )}

          {hasStay && availLoading && (
            <p className="text-sm text-ink/50">Checking availability…</p>
          )}

          {hasStay && availError && (
            <InlineAlert tone="error">
              {getErrorMessage(availErr, 'Could not load rooms for these dates.')}
            </InlineAlert>
          )}

          {hasStay && availability && availability.length === 0 && (
            <EmptyState
              title="No rooms for these dates"
              description="Try different dates or another property."
            />
          )}

          {hasStay && availability && availability.length > 0 && (
            <ul className="divide-y divide-sand/80 overflow-hidden rounded-2xl border border-sand/80 bg-white/70">
              {availability.map((item) => {
                const roomImage = resolveMediaUrl(item.roomType.primaryImageUrl);
                return (
                  <li
                    key={item.roomType.id}
                    className="flex flex-col gap-4 p-5 transition hover:bg-fog/50 md:flex-row md:items-center md:justify-between"
                  >
                    <div className="flex min-w-0 flex-1 gap-4">
                      {roomImage && (
                        <img
                          src={roomImage}
                          alt={item.roomType.name}
                          className="h-20 w-28 shrink-0 rounded-xl object-cover sm:h-24 sm:w-32"
                          loading="lazy"
                        />
                      )}
                      <div className="min-w-0">
                        <h3 className="font-display text-xl font-semibold text-ink">
                          {item.roomType.name}
                        </h3>
                        <p className="mt-1 line-clamp-2 text-sm text-ink/65">
                          {item.roomType.description}
                        </p>
                        <p className="mt-2 text-xs font-medium uppercase tracking-wide text-moss">
                          {item.availableCount} left · {item.nights} night
                          {item.nights === 1 ? '' : 's'} · sleeps {item.roomType.maxOccupancy}
                        </p>
                      </div>
                    </div>
                    <div className="flex shrink-0 items-center gap-5 md:flex-col md:items-end md:gap-2">
                      <div className="md:text-right">
                        <p className="text-xs uppercase tracking-wide text-ink/45">From</p>
                        <p className="font-display text-2xl font-bold text-forest">
                          {money(item.totalPrice)}
                        </p>
                        <p className="text-xs text-ink/50">
                          {money(item.nightlyRate)} / night
                        </p>
                      </div>
                      <Link
                        to={roomDetailPath(hotelId, item.roomType.id, stay)}
                        className="btn-primary inline-flex"
                      >
                        View room
                      </Link>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </section>

        <section>
          <h2 className="mb-4 font-display text-2xl font-bold text-ink">Ask the concierge</h2>
          <PublicRagWidget hotelId={hotelId} />
        </section>
      </div>
    </div>
  );
}
