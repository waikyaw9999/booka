import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../../lib/api';
import { resolveMediaUrl } from '../../lib/media';
import type { PropertyImage } from '../../types';

interface Props {
  token: string;
  scope: 'hotel' | 'room';
  ownerId: number;
  title: string;
  onFlashSuccess: (text: string) => void;
  onFlashError: (err: unknown) => void;
}

export function ImageManager({
  token,
  scope,
  ownerId,
  title,
  onFlashSuccess,
  onFlashError,
}: Props) {
  const queryClient = useQueryClient();
  const [url, setUrl] = useState('');
  const [caption, setCaption] = useState('');
  const [asPrimary, setAsPrimary] = useState(false);
  const queryKey = [scope === 'hotel' ? 'hotel-images' : 'room-images', ownerId];

  const { data: images, isLoading } = useQuery({
    queryKey,
    queryFn: () =>
      scope === 'hotel'
        ? api.listHotelImages(token, ownerId)
        : api.listRoomImages(token, ownerId),
  });

  function invalidate() {
    queryClient.invalidateQueries({ queryKey });
    if (scope === 'hotel') {
      queryClient.invalidateQueries({ queryKey: ['hotel', ownerId] });
      queryClient.invalidateQueries({ queryKey: ['hotels'] });
    } else {
      queryClient.invalidateQueries({ queryKey: ['room-board'] });
    }
  }

  const addMutation = useMutation({
    mutationFn: () =>
      scope === 'hotel'
        ? api.addHotelImage(token, ownerId, {
            url: url.trim(),
            caption: caption.trim() || null,
            primary: asPrimary || undefined,
          })
        : api.addRoomImage(token, ownerId, {
            url: url.trim(),
            caption: caption.trim() || null,
            primary: asPrimary || undefined,
          }),
    onSuccess: () => {
      setUrl('');
      setCaption('');
      setAsPrimary(false);
      invalidate();
      onFlashSuccess('Image added.');
    },
    onError: onFlashError,
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) =>
      scope === 'hotel'
        ? api.uploadHotelImage(token, ownerId, file, caption.trim() || undefined, asPrimary || undefined)
        : api.uploadRoomImage(token, ownerId, file, caption.trim() || undefined, asPrimary || undefined),
    onSuccess: () => {
      setCaption('');
      setAsPrimary(false);
      invalidate();
      onFlashSuccess('Image uploaded.');
    },
    onError: onFlashError,
  });

  const deleteMutation = useMutation({
    mutationFn: (imageId: number) =>
      scope === 'hotel'
        ? api.deleteHotelImage(token, ownerId, imageId)
        : api.deleteRoomImage(token, ownerId, imageId),
    onSuccess: () => {
      invalidate();
      onFlashSuccess('Image removed.');
    },
    onError: onFlashError,
  });

  const primaryMutation = useMutation({
    mutationFn: (imageId: number) =>
      scope === 'hotel'
        ? api.setPrimaryHotelImage(token, ownerId, imageId)
        : api.setPrimaryRoomImage(token, ownerId, imageId),
    onSuccess: () => {
      invalidate();
      onFlashSuccess('Primary image updated.');
    },
    onError: onFlashError,
  });

  function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    if (!url.trim()) {
      onFlashError(new Error('Image URL is required'));
      return;
    }
    addMutation.mutate();
  }

  return (
    <div className="space-y-3 rounded-xl border border-sand/80 bg-fog/40 p-3">
      <div>
        <h3 className="text-sm font-semibold text-ink">{title}</h3>
        <p className="mt-0.5 text-xs text-ink/50">
          Paste an https image URL, or upload a JPEG/PNG/WebP (stored under /uploads).
        </p>
      </div>

      <form onSubmit={handleAdd} className="grid gap-2 sm:grid-cols-[1fr_auto]">
        <input
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="https://…"
          className="input-field"
        />
        <button
          type="submit"
          className="btn-secondary !px-3 !py-2 text-xs"
          disabled={addMutation.isPending}
        >
          {addMutation.isPending ? 'Adding…' : 'Add URL'}
        </button>
        <input
          value={caption}
          onChange={(e) => setCaption(e.target.value)}
          placeholder="Caption (optional)"
          className="input-field sm:col-span-2"
        />
        <label className="flex items-center gap-2 text-xs text-ink/65 sm:col-span-2">
          <input
            type="checkbox"
            checked={asPrimary}
            onChange={(e) => setAsPrimary(e.target.checked)}
          />
          Set as primary
        </label>
        <div className="sm:col-span-2">
          <label className="label-caps">Or upload file</label>
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            className="block w-full text-xs text-ink/60 file:mr-3 file:rounded-lg file:border-0 file:bg-sand file:px-3 file:py-1.5 file:text-xs file:font-medium file:text-ink"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) uploadMutation.mutate(file);
              e.target.value = '';
            }}
            disabled={uploadMutation.isPending}
          />
        </div>
      </form>

      {isLoading ? (
        <p className="text-xs text-ink/45">Loading images…</p>
      ) : !images?.length ? (
        <p className="text-xs text-ink/45">No images yet.</p>
      ) : (
        <ul className="space-y-2">
          {images.map((img: PropertyImage) => {
            const src = resolveMediaUrl(img.url) ?? img.url;
            return (
              <li
                key={img.id}
                className="flex items-center gap-3 rounded-lg border border-sand/70 bg-white/70 p-2"
              >
                <img
                  src={src}
                  alt={img.caption ?? 'Property'}
                  className="h-14 w-20 shrink-0 rounded object-cover"
                />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-xs text-ink/70">{img.caption || img.url}</p>
                  {img.primary && (
                    <span className="mt-0.5 inline-block text-[10px] font-semibold uppercase tracking-wide text-moss">
                      Primary
                    </span>
                  )}
                </div>
                <div className="flex shrink-0 flex-col gap-1">
                  {!img.primary && (
                    <button
                      type="button"
                      className="btn-ghost !px-2 !py-1 text-[10px]"
                      onClick={() => primaryMutation.mutate(img.id)}
                      disabled={primaryMutation.isPending}
                    >
                      Make primary
                    </button>
                  )}
                  <button
                    type="button"
                    className="btn-ghost !px-2 !py-1 text-[10px] text-coral"
                    onClick={() => deleteMutation.mutate(img.id)}
                    disabled={deleteMutation.isPending}
                  >
                    Delete
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
