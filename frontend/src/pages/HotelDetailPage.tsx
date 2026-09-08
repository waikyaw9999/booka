import { useState } from 'react';
import { useParams, Link, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api, getErrorMessage } from '../lib/api';
import { hotelDisplayImage, resolveMediaUrl } from '../lib/media';
import { useBookingStore } from '../stores';
import { PublicRagWidget } from '../components/PublicRagWidget';
import { BlackoutNotice } from '../components/BlackoutNotice';
import { EmptyState, InlineAlert } from '../components/ui';

export function HotelDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const hotelId = Number(id);
  const { draft, setDraft } = useBookingStore();
  const [checkIn, setCheckIn] = useState(
    searchParams.get('checkIn') || draft.checkInDate,
  );
  const [checkOut, setCheckOut] = useState(
    searchParams.get('checkOut') || draft.checkOutDate,
  );
  const [numGuests, setNumGuests] = useState(
    Number(searchParams.get('guests')) || draft.numGuests,
  );
  const [searching, setSearching] = useState(false);
  const [availability, setAvailability] = useState<Awaited<ReturnType<typeof api.checkAvailability>> | null>(null);
  const [searchError, setSearchError] = useState('');

  const { data: hotel, isLoading } = useQuery({
    queryKey: ['hotel', hotelId],
    queryFn: () => api.getHotel(hotelId),
    enabled: !Number.isNaN(hotelId),
  });

  const { data: blackouts } = useQuery({
    queryKey: ['blackouts', hotelId],
    queryFn: () => api.getBlackoutDates(hotelId),
    enabled: !Number.isNaN(hotelId),
  });

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    setSearchError('');
    setSearching(true);
    try {
      const result = await api.checkAvailability(hotelId, {
        checkInDate: checkIn,
        checkOutDate: checkOut,
        numGuests,
      });
      setAvailability(result);
      setDraft({ hotelId, hotelName: hotel?.name ?? null, checkInDate: checkIn, checkOutDate: checkOut, numGuests });
    } catch (err) {
      setSearchError(getErrorMessage(err, 'Search failed. Please try again.'));
    } finally {
      setSearching(false);
    }
  }

  function selectRoom(room: NonNullable<typeof availability>[0]) {
    setDraft({ selectedRoomType: room });
  }

  if (isLoading) {
    return <p className="py-24 text-center text-ink/50">Loading stay…</p>;
  }
  if (!hotel) {
    return (
      <div className="mx-auto max-w-lg px-4 py-24">
        <EmptyState title="Hotel not found" description="This property may have been removed." />
      </div>
    );
  }

  const heroImage = hotelDisplayImage(hotel);
  const gallery = (hotel.images ?? [])
    .map((img) => ({ ...img, resolved: resolveMediaUrl(img.url) }))
    .filter((img) => img.resolved);

  return (
    <div>
      <section className="relative h-[42vh] min-h-[280px] overflow-hidden">
        <img
          src={heroImage}
          alt={hotel.name}
          className="absolute inset-0 h-full w-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-ink via-ink/40 to-ink/20" />
        <div className="relative mx-auto flex h-full max-w-4xl flex-col justify-end px-4 pb-10">
          <Link to="/" className="mb-4 text-sm text-white/75 hover:text-white">
            ← All stays
          </Link>
          <h1 className="animate-rise font-display text-4xl font-bold text-white sm:text-5xl">
            {hotel.name}
          </h1>
          <p className="animate-rise-delay mt-2 text-white/75">
            {hotel.city}, {hotel.country}
          </p>
        </div>
      </section>

      <div className="mx-auto max-w-4xl space-y-10 px-4 py-10">
        {gallery.length > 1 && (
          <section className="animate-rise grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
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

        <section className="animate-rise space-y-3">
          <p className="max-w-2xl text-lg leading-relaxed text-ink/75">{hotel.description}</p>
          <p className="text-sm text-ink/55">{hotel.address}</p>
          <div className="flex flex-wrap gap-2 pt-2">
            {(hotel.amenities ?? '').split(',').filter(Boolean).map((item) => (
              <span
                key={item}
                className="rounded-lg bg-sand/70 px-3 py-1 text-xs font-medium text-forest"
              >
                {item.trim()}
              </span>
            ))}
          </div>
          <p className="pt-2 text-sm text-ink/60">
            <span className="font-semibold text-ink">Cancellation:</span> {hotel.cancellationPolicy}
          </p>
        </section>

        {blackouts && <BlackoutNotice blackouts={blackouts} />}

        <section>
          <h2 className="font-display text-2xl font-bold text-ink">Check availability</h2>
          <form
            onSubmit={handleSearch}
            className="surface mt-4 grid items-end gap-4 p-5 md:grid-cols-4"
          >
            <div>
              <label className="label-caps">Check-in</label>
              <input
                type="date"
                required
                value={checkIn}
                onChange={(e) => setCheckIn(e.target.value)}
                className="input-field"
              />
            </div>
            <div>
              <label className="label-caps">Check-out</label>
              <input
                type="date"
                required
                value={checkOut}
                onChange={(e) => setCheckOut(e.target.value)}
                className="input-field"
              />
            </div>
            <div>
              <label className="label-caps">Guests</label>
              <input
                type="number"
                min={1}
                max={10}
                value={numGuests}
                onChange={(e) => setNumGuests(Number(e.target.value))}
                className="input-field"
              />
            </div>
            <button type="submit" disabled={searching} className="btn-primary w-full">
              {searching ? 'Searching…' : 'Check dates'}
            </button>
          </form>
          {searchError && (
            <div className="mt-3">
              <InlineAlert tone="error">{searchError}</InlineAlert>
            </div>
          )}
        </section>

        {availability && (
          <section className="space-y-4 animate-tab">
            <div className="flex items-end justify-between gap-4">
              <h2 className="font-display text-2xl font-bold text-ink">Select a rate</h2>
              <p className="text-xs text-ink/45">
                {checkIn} → {checkOut} · {numGuests} guest{numGuests === 1 ? '' : 's'}
              </p>
            </div>
            {availability.length === 0 ? (
              <EmptyState
                title="No rooms for these dates"
                description="Dates may overlap a blackout period or existing bookings. Try another range."
              />
            ) : (
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
                        <p className="mt-1 text-sm text-ink/65">{item.roomType.description}</p>
                        <p className="mt-2 text-xs font-medium uppercase tracking-wide text-moss">
                          {item.availableCount} available · {item.nights} night
                          {item.nights === 1 ? '' : 's'}
                        </p>
                      </div>
                    </div>
                    <div className="flex shrink-0 items-center gap-5 md:flex-col md:items-end md:gap-2">
                      <div className="md:text-right">
                        <p className="text-xs uppercase tracking-wide text-ink/45">Total stay</p>
                        <p className="font-display text-2xl font-bold text-forest">
                          ${item.totalPrice.toFixed(2)}
                        </p>
                        <p className="text-xs text-ink/50">
                          ${item.nightlyRate.toFixed(2)} / night
                        </p>
                      </div>
                      <Link
                        to="/book"
                        onClick={() => selectRoom(item)}
                        className="btn-primary inline-flex"
                      >
                        Book
                      </Link>
                    </div>
                  </li>
                  );
                })}
              </ul>
            )}
          </section>
        )}

        <section>
          <h2 className="mb-4 font-display text-2xl font-bold text-ink">Ask the concierge</h2>
          <PublicRagWidget hotelId={hotelId} />
        </section>
      </div>
    </div>
  );
}
