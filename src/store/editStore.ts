import { create } from 'zustand';
import { Clip } from '../types/clip';

interface EditState {
  clip: Clip | null;
  open: (clip: Clip) => void;
  close: () => void;
}

/**
 * Which clip is being edited, held outside the list.
 *
 * The edit sheet used to be a React Native `<Modal>`, which renders into its
 * own Android window — a window that does not inherit the activity's
 * `adjustResize`, so the keyboard covered it. It is an ordinary view in the
 * main window now, which means it has to be rendered high enough in the tree
 * to sit over the app bar as well as the list. The row that opens it is deep
 * inside the list, so the two are joined here rather than by threading a
 * callback up and a prop back down.
 *
 * The floating overlay has no edit route at all, so nothing there writes here.
 */
export const useEditStore = create<EditState>((set) => ({
  clip: null,
  open: (clip) => set({ clip }),
  close: () => set({ clip: null }),
}));
