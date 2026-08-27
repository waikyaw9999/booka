import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthUser, BookingDraft } from '../types';

interface AuthState {
  user: AuthUser | null;
  setUser: (user: AuthUser | null) => void;
  logout: () => void;
  isStaff: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      setUser: (user) => set({ user }),
      logout: () => set({ user: null }),
      isStaff: () => {
        const role = get().user?.role;
        return role === 'STAFF' || role === 'ADMIN';
      },
    }),
    { name: 'booka-auth' },
  ),
);

const defaultDraft: BookingDraft = {
  hotelId: null,
  hotelName: null,
  checkInDate: '',
  checkOutDate: '',
  numGuests: 2,
  selectedRoomType: null,
  guestEmail: '',
  guestFullName: '',
  guestPhone: '',
};

interface BookingState {
  draft: BookingDraft;
  setDraft: (partial: Partial<BookingDraft>) => void;
  clearDraft: () => void;
}

export const useBookingStore = create<BookingState>()(
  persist(
    (set) => ({
      draft: defaultDraft,
      setDraft: (partial) => set((s) => ({ draft: { ...s.draft, ...partial } })),
      clearDraft: () => set({ draft: defaultDraft }),
    }),
    { name: 'booka-booking-draft' },
  ),
);

interface RagChatState {
  publicMessages: import('../types').RagMessage[];
  staffMessages: import('../types').RagMessage[];
  addPublicMessage: (msg: import('../types').RagMessage) => void;
  addStaffMessage: (msg: import('../types').RagMessage) => void;
  clearStaffMessages: () => void;
}

export const useRagStore = create<RagChatState>()((set) => ({
  publicMessages: [],
  staffMessages: [],
  addPublicMessage: (msg) => set((s) => ({ publicMessages: [...s.publicMessages, msg] })),
  addStaffMessage: (msg) => set((s) => ({ staffMessages: [...s.staffMessages, msg] })),
  clearStaffMessages: () => set({ staffMessages: [] }),
}));
