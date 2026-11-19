import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api, getErrorMessage } from '../lib/api';
import { money } from '../lib/format';
import { hotelDisplayImage, resolveMediaUrl } from '../lib/media';
import { hotelStayPath, parseStaySearch } from '../lib/search';
import { useBookingStore } from '../stores';
import { InlineAlert } from '../components/ui';
import type { AvailableRoomType } from '../types';

export function RoomDetailPage() {
  const { id, roomTypeId: roomTypeIdParam } = useParams<{ id: string; roomTypeId: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { setDraft } = useBookingStore();
  const hotelId = Number(id);
  const roomTypeId = Number(roomTypeIdParam);
  const stay = parseStaySearch(searchParams);

  const { data: hotel, isLoading: hotelLoading, isError: hotelError } = useQuery({
    queryKey: ['hotel', hotelId],
    queryFn: () => api.getHotel(hotelId),
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
    enabled:
      !Number.isNaN(hotelId) &&
      !!stay.checkIn &&
      !!stay.checkOut &&
      stay.checkOut > stay.checkIn,
  });

  const option: AvailableRoomType | undefined = availability?.find(
    (a) => a.roomType.id === roomTypeId,
  );

  function bookNow() {
    if (!hotel || !option) return;
    setDraft({
      hotelId,
      hotelName: hotel.name,
      checkInDate: stay.checkIn,
      checkOutDate: stay.checkOut,
      numGuests: stay.guests,
      selectedRoomType: option,
    });
    navigate('/book');
  }

  if (hotelLoading || availLoading) {
    return <p className="mx-auto max-w-4xl px-4 py-16 text-ink/50">Loading room…</p>;
  }

  if (hotelError || !hotel) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-16">
        <InlineAlert tone="error">Property not found.</InlineAlert>
        <Link to="/search" className="mt-4 inline-block text-forest underline">
          Back to search
        </Link>
      </div>
    );
  }

  if (availError) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-16">
        <InlineAlert tone="error">
          {getErrorMessage(availErr, 'Could not load availability.')}
        </InlineAlert>
      </div>
    );
  }

  if (!option) {
    return (
      <div className="mx-auto max-w-4xl space-y-4 px-4 py-16">
        <InlineAlert tone="warning">
          This room type is not available for your dates. Pick another room or change dates.
        </InlineAlert>
        <Link to={hotelStayPath(hotelId, stay)} className="btn-secondary inline-flex">
          ← Back to rooms
        </Link>
      </div>
    );
  }

  const image =
    resolveMediaUrl(option.roomType.primaryImageUrl) ||
    hotelDisplayImage(hotel, 0);

  return (
    <div className="mx-auto max-w-5xl space-y-8 px-4 py-8 sm:py-12">
      <div className="space-y-2">
        <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-moss">
          Step 4 · Room detail
        </p>
        <Link
          to={hotelStayPath(hotelId, stay)}
          className="text-sm text-forest underline-offset-4 hover:underline"
        >
          ← {hotel.name} rooms
        </Link>
        <h1 className="font-display text-3xl font-bold text-ink sm:text-4xl">
          {option.roomType.name}
        </h1>
        <p className="text-ink/60">
          {stay.checkIn} → {stay.checkOut} · {stay.guests} guest
          {stay.guests === 1 ? '' : 's'} · {option.nights} night
          {option.nights === 1 ? '' : 's'}
        </p>
      </div>

      <div className="grid gap-8 lg:grid-cols-[1.2fr_0.8fr]">
        <div className="space-y-4">
          <div className="overflow-hidden rounded-2xl border border-sand/80">
            <img src={image} alt={option.roomType.name} className="h-72 w-full object-cover sm:h-96" />
          </div>
          {option.roomType.description && (
            <p className="leading-relaxed text-ink/70">{option.roomType.description}</p>
          )}
          {option.roomType.amenities && (
            <div>
              <p className="label-caps mb-2">Amenities</p>
              <p className="text-sm text-ink/65">{option.roomType.amenities}</p>
            </div>
          )}
          <p className="text-sm text-ink/55">
            Sleeps up to {option.roomType.maxOccupancy} · {option.availableCount} left for these
            dates
          </p>
        </div>

        <aside className="surface-elevated h-fit space-y-4 p-5 sm:p-6">
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-[0.12em] text-ink/45">
              Room subtotal
            </p>
            <p className="font-display text-3xl font-bold text-forest">
              {money(option.totalPrice)}
            </p>
            <p className="mt-1 text-sm text-ink/55">
              {money(option.nightlyRate)} / night · tax added at checkout
            </p>
          </div>
          <button type="button" className="btn-primary w-full" onClick={bookNow}>
            Continue to book
          </button>
          <p className="text-xs leading-relaxed text-ink/50">
            Confirm now · pay at property. No online card charge on this step.
          </p>
        </aside>
      </div>
    </div>
  );
}
