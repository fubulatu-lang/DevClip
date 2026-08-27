export interface Clip {
  id: number;
  title: string | null;
  content: string;
  createdAt: number; // unix ms timestamp
  sortOrder: number; // used for manual drag/up-down reordering
}

export type SortMode =
  | 'manual'
  | 'title-asc'
  | 'title-desc'
  | 'date-asc'
  | 'date-desc';

export type PopupState = 'small' | 'expanded' | 'full';
