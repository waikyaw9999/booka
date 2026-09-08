import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, getErrorMessage } from '../../lib/api';
import type { RoomType } from '../../types';
import { EmptyState, InlineAlert, StatusPill } from '../ui';
import { ImageManager } from './ImageManager';
import { resolveMediaUrl } from '../../lib/media';

interface Props {
  token: string;
  hotelId: number;
}

export function PropertyInventory({ token, hotelId }: Props) {
  const queryClient = useQueryClient();
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [hotelAmenities, setHotelAmenities] = useState('');
  const [editingRoomTypeId, setEditingRoomTypeId] = useState<number | null>(null);
  const [rtName, setRtName] = useState('');
  const [rtAmenities, setRtAmenities] = useState('');
  const [rtBaseRate, setRtBaseRate] = useState('');

  const [roomTypeId, setRoomTypeId] = useState('');
  const [roomNumber, setRoomNumber] = useState('');
  const [floor, setFloor] = useState('');
  const [notes, setNotes] = useState('');
  const [showAddRoom, setShowAddRoom] = useState(false);
  const [imagesRoomId, setImagesRoomId] = useState<number | null>(null);

  const { data: hotel, isLoading: hotelLoading } = useQuery({
    queryKey: ['hotel', hotelId],
    queryFn: () => api.getHotel(hotelId),
  });

  const { data: roomTypes, isLoading: typesLoading } = useQuery({
    queryKey: ['room-types', hotelId],
    queryFn: () => api.getRoomTypes(hotelId),
  });

  const { data: rooms, isLoading: roomsLoading } = useQuery({
    queryKey: ['room-board'],
    queryFn: () => api.listRoomBoard(token),
  });

  useEffect(() => {
    if (hotel) {
      setHotelAmenities(hotel.amenities ?? '');
    }
  }, [hotel]);

  useEffect(() => {
    if (roomTypes && roomTypes.length > 0 && !roomTypeId) {
      setRoomTypeId(String(roomTypes[0].id));
    }
  }, [roomTypes, roomTypeId]);

  function flashSuccess(text: string) {
    setError('');
    setSuccess(text);
  }

  function flashError(err: unknown) {
    setSuccess('');
    setError(getErrorMessage(err, 'Unable to save. Please try again.'));
  }

  const saveHotelMutation = useMutation({
    mutationFn: () => api.updateHotel(token, hotelId, { amenities: hotelAmenities }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['hotel', hotelId] });
      flashSuccess('Hotel amenities saved.');
    },
    onError: flashError,
  });

  const saveRoomTypeMutation = useMutation({
    mutationFn: (id: number) =>
      api.updateRoomType(token, id, {
        name: rtName.trim(),
        amenities: rtAmenities,
        baseRate: Number(rtBaseRate),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['room-types', hotelId] });
      setEditingRoomTypeId(null);
      flashSuccess('Room type updated.');
    },
    onError: flashError,
  });

  const createRoomMutation = useMutation({
    mutationFn: () =>
      api.createRoom(token, {
        roomTypeId: Number(roomTypeId),
        roomNumber: roomNumber.trim(),
        floor: floor === '' ? null : Number(floor),
        notes: notes.trim() || null,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['room-board'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      setRoomNumber('');
      setFloor('');
      setNotes('');
      setShowAddRoom(false);
      flashSuccess('Room added to inventory.');
    },
    onError: flashError,
  });

  function startEditRoomType(rt: RoomType) {
    setEditingRoomTypeId(rt.id);
    setRtName(rt.name);
    setRtAmenities(rt.amenities ?? '');
    setRtBaseRate(String(rt.baseRate));
    setError('');
    setSuccess('');
  }

  function handleSaveRoomType(e: React.FormEvent) {
    e.preventDefault();
    if (editingRoomTypeId == null) return;
    if (!rtName.trim()) {
      flashError(new Error('Room type name is required'));
      return;
    }
    const rate = Number(rtBaseRate);
    if (!Number.isFinite(rate) || rate <= 0) {
      flashError(new Error('Base rate must be greater than zero'));
      return;
    }
    saveRoomTypeMutation.mutate(editingRoomTypeId);
  }

  function handleAddRoom(e: React.FormEvent) {
    e.preventDefault();
    if (!roomTypeId || !roomNumber.trim()) {
      flashError(new Error('Room type and room number are required'));
      return;
    }
    createRoomMutation.mutate();
  }

  const roomTypeName = (id: number) =>
    roomTypes?.find((rt) => rt.id === id)?.name ?? `Type #${id}`;

  return (
    <div className="space-y-5">
      {error && <InlineAlert tone="error">{error}</InlineAlert>}
      {success && <InlineAlert tone="success">{success}</InlineAlert>}

      <section className="ops-panel space-y-3">
        <div>
          <h2 className="font-display text-lg font-semibold text-ink">Hotel amenities</h2>
          <p className="mt-1 text-sm text-ink/50">
            Comma-separated list shown on the guest property page
            {hotel ? ` for ${hotel.name}` : ''}.
          </p>
        </div>
        {hotelLoading ? (
          <p className="text-sm text-ink/45">Loading hotel…</p>
        ) : (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              saveHotelMutation.mutate();
            }}
            className="space-y-3"
          >
            <div>
              <label className="label-caps">Amenities</label>
              <textarea
                value={hotelAmenities}
                onChange={(e) => setHotelAmenities(e.target.value)}
                rows={3}
                placeholder="WiFi, Pool, Gym, Restaurant"
                className="input-field min-h-[5rem] resize-y"
              />
            </div>
            <button
              type="submit"
              className="btn-primary"
              disabled={saveHotelMutation.isPending}
            >
              {saveHotelMutation.isPending ? 'Saving…' : 'Save amenities'}
            </button>
          </form>
        )}
      </section>

      <section className="ops-panel space-y-3">
        <div>
          <h2 className="font-display text-lg font-semibold text-ink">Property images</h2>
          <p className="mt-1 text-sm text-ink/50">
            Shown on the guest home cards and hotel detail page. Primary image is the hero.
          </p>
        </div>
        <ImageManager
          token={token}
          scope="hotel"
          ownerId={hotelId}
          title="Hotel gallery"
          onFlashSuccess={flashSuccess}
          onFlashError={flashError}
        />
      </section>

      <section className="ops-panel space-y-3">
        <div>
          <h2 className="font-display text-lg font-semibold text-ink">Room types</h2>
          <p className="mt-1 text-sm text-ink/50">
            Edit name, base rate, and amenities for each category.
          </p>
        </div>

        {typesLoading ? (
          <p className="text-sm text-ink/45">Loading room types…</p>
        ) : !roomTypes?.length ? (
          <EmptyState
            title="No room types"
            description="Create room types via admin API before adding rooms."
          />
        ) : (
          <ul className="divide-y divide-sand/70">
            {roomTypes.map((rt) => (
              <li key={rt.id} className="py-3">
                {editingRoomTypeId === rt.id ? (
                  <form onSubmit={handleSaveRoomType} className="grid gap-3 sm:grid-cols-2">
                    <div>
                      <label className="label-caps">Name</label>
                      <input
                        value={rtName}
                        onChange={(e) => setRtName(e.target.value)}
                        className="input-field"
                        required
                      />
                    </div>
                    <div>
                      <label className="label-caps">Base rate (USD)</label>
                      <input
                        type="number"
                        min="0.01"
                        step="0.01"
                        value={rtBaseRate}
                        onChange={(e) => setRtBaseRate(e.target.value)}
                        className="input-field"
                        required
                      />
                    </div>
                    <div className="sm:col-span-2">
                      <label className="label-caps">Amenities</label>
                      <input
                        value={rtAmenities}
                        onChange={(e) => setRtAmenities(e.target.value)}
                        placeholder="WiFi, TV, Air Conditioning"
                        className="input-field"
                      />
                    </div>
                    <div className="flex flex-wrap gap-2 sm:col-span-2">
                      <button
                        type="submit"
                        className="btn-primary"
                        disabled={saveRoomTypeMutation.isPending}
                      >
                        {saveRoomTypeMutation.isPending ? 'Saving…' : 'Save room type'}
                      </button>
                      <button
                        type="button"
                        className="btn-ghost"
                        onClick={() => setEditingRoomTypeId(null)}
                      >
                        Cancel
                      </button>
                    </div>
                  </form>
                ) : (
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                    <div className="min-w-0">
                      <p className="font-medium text-ink">
                        {rt.name}{' '}
                        <span className="text-sm font-normal text-ink/45">
                          · ${Number(rt.baseRate).toFixed(0)}/night · max {rt.maxOccupancy}
                        </span>
                      </p>
                      <p className="mt-1 text-sm text-ink/55">
                        {rt.amenities?.trim() ? rt.amenities : 'No amenities listed'}
                      </p>
                    </div>
                    <button
                      type="button"
                      className="btn-secondary shrink-0 !px-3 !py-1.5 text-xs"
                      onClick={() => startEditRoomType(rt)}
                    >
                      Edit
                    </button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="ops-panel space-y-3">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="font-display text-lg font-semibold text-ink">Rooms</h2>
            <p className="mt-1 text-sm text-ink/50">
              Physical inventory for this property ({rooms?.length ?? 0} rooms).
            </p>
          </div>
          <button
            type="button"
            className="btn-primary shrink-0"
            onClick={() => {
              setShowAddRoom((v) => !v);
              setError('');
              setSuccess('');
            }}
          >
            {showAddRoom ? 'Close form' : 'Add room'}
          </button>
        </div>

        {showAddRoom && (
          <form onSubmit={handleAddRoom} className="grid gap-3 border-t border-sand/70 pt-3 sm:grid-cols-2">
            <div>
              <label className="label-caps">Room type</label>
              <select
                required
                value={roomTypeId}
                onChange={(e) => setRoomTypeId(e.target.value)}
                className="input-field"
              >
                <option value="">Select type</option>
                {roomTypes?.map((rt) => (
                  <option key={rt.id} value={rt.id}>
                    {rt.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="label-caps">Room number</label>
              <input
                required
                value={roomNumber}
                onChange={(e) => setRoomNumber(e.target.value)}
                placeholder="e.g. 105"
                className="input-field"
              />
            </div>
            <div>
              <label className="label-caps">Floor (optional)</label>
              <input
                type="number"
                value={floor}
                onChange={(e) => setFloor(e.target.value)}
                placeholder="e.g. 1"
                className="input-field"
              />
            </div>
            <div>
              <label className="label-caps">Notes (optional)</label>
              <input
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Corner unit, quiet side…"
                className="input-field"
              />
            </div>
            <div className="sm:col-span-2">
              <button
                type="submit"
                className="btn-primary"
                disabled={createRoomMutation.isPending}
              >
                {createRoomMutation.isPending ? 'Adding…' : 'Add to inventory'}
              </button>
            </div>
          </form>
        )}

        {roomsLoading ? (
          <p className="text-sm text-ink/45">Loading rooms…</p>
        ) : !rooms?.length ? (
          <EmptyState
            title="No rooms yet"
            description="Add rooms so Front Office and availability have inventory to sell."
          />
        ) : (
          <div className="overflow-x-auto border-t border-sand/70 pt-1">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Room</th>
                  <th>Type</th>
                  <th>Floor</th>
                  <th>Status</th>
                  <th>Notes</th>
                  <th>Images</th>
                </tr>
              </thead>
              <tbody>
                {rooms.map((r) => (
                  <tr key={r.id}>
                    <td className="font-medium text-ink">
                      <span className="inline-flex items-center gap-2">
                        {r.primaryImageUrl && (
                          <img
                            src={resolveMediaUrl(r.primaryImageUrl) ?? r.primaryImageUrl}
                            alt=""
                            className="h-8 w-10 rounded object-cover"
                          />
                        )}
                        #{r.roomNumber}
                      </span>
                    </td>
                    <td className="text-sm">{r.roomTypeName || roomTypeName(r.roomTypeId)}</td>
                    <td className="text-sm text-ink/65">{r.floor ?? '—'}</td>
                    <td>
                      <StatusPill label={r.status} />
                    </td>
                    <td className="max-w-[14rem] truncate text-xs text-ink/50">
                      {r.notes?.trim() ? r.notes : '—'}
                    </td>
                    <td>
                      <button
                        type="button"
                        className="btn-secondary !px-2 !py-1 text-xs"
                        onClick={() =>
                          setImagesRoomId((id) => (id === r.id ? null : r.id))
                        }
                      >
                        {imagesRoomId === r.id ? 'Hide' : 'Manage'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {imagesRoomId != null && (
              <div className="border-t border-sand/70 pt-3">
                <ImageManager
                  token={token}
                  scope="room"
                  ownerId={imagesRoomId}
                  title={`Room #${rooms.find((r) => r.id === imagesRoomId)?.roomNumber ?? imagesRoomId} images`}
                  onFlashSuccess={flashSuccess}
                  onFlashError={flashError}
                />
              </div>
            )}
          </div>
        )}
      </section>
    </div>
  );
}
