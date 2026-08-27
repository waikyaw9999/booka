import { useState } from 'react';
import { useParams, Link, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import { useBookingStore } from '../stores';
import { PublicRagWidget } from '../components/PublicRagWidget';
import { BlackoutNotice } from '../components/BlackoutNotice';

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
      setSearchError(err instanceof Error ? err.message : 'Search failed');
    } finally {
      setSearching(false);
    }
  }

  function selectRoom(room: NonNullable<typeof availability>[0]) {
    setDraft({ selectedRoomType: room });
  }

  if (isLoading) {
    return <p className="py-24 text-center text-ink/50">Loading stay...</p>;
  }
  if (!hotel) {
    return <p className="py-24 text-center text-red-700">Hotel not found</p>;
  }

  return (
    <div>
      <section className="relative h-[42vh] min-h-[280px] overflow-hidden">
        <img
          src="https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1800&q=80"
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
        <section className="animate-rise space-y-3">
          <p className="max-w-2xl text-lg leading-relaxed text-ink/75">{hotel.description}</p>
          <p className="text-sm text-ink/55">{hotel.address}</p>
          <div className="flex flex-wrap gap-2 pt-2">
            {hotel.amenities.split(',').map((item) => (
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
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink/55">
                Check-in
              </label>
              <input
                type="date"
                required
                value={checkIn}
                onChange={(e) => setCheckIn(e.target.value)}
                className="input-field"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink/55">
                Check-out
              </label>
              <input
                type="date"
                required
                value={checkOut}
                onChange={(e) => setCheckOut(e.target.value)}
                className="input-field"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink/55">
                Guests
              </label>
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
              {searching ? 'Searching...' : 'Check dates'}
            </button>
          </form>
          {searchError && <p className="mt-3 text-sm text-red-700">{searchError}</p>}
        </section>

        {availability && (
          <section className="space-y-4">
            <h2 className="font-display text-2xl font-bold text-ink">Available rooms</h2>
            {availability.length === 0 ? (
              <p className="text-ink/60">
                No rooms available for these dates. They may overlap a blackout period or existing bookings.
              </p>
            ) : (
              availability.map((item) => (
                <div
                  key={item.roomType.id}
                  className="surface flex flex-col justify-between gap-4 p-5 md:flex-row md:items-center"
                >
                  <div>
                    <h3 className="font-display text-xl font-semibold text-ink">
                      {item.roomType.name}
                    </h3>
                    <p className="mt-1 text-sm text-ink/65">{item.roomType.description}</p>
                    <p className="mt-2 text-xs font-medium uppercase tracking-wide text-moss">
                      {item.availableCount} room(s) · {item.nights} night(s)
                    </p>
                  </div>
                  <div className="md:text-right space-y-2">
                    <p className="font-display text-2xl font-bold text-forest">
                      ${item.totalPrice.toFixed(2)}
                    </p>
                    <p className="text-xs text-ink/50">
                      ${item.nightlyRate.toFixed(2)}/night before tax
                    </p>
                    <Link
                      to="/book"
                      onClick={() => selectRoom(item)}
                      className="btn-primary inline-flex"
                    >
                      Book now
                    </Link>
                  </div>
                </div>
              ))
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
