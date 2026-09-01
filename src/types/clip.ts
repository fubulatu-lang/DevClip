export interface Clip {
  id: number;
  title: string | null;
  content: string;
  createdAt: number; // unix ms timestamp
  /**
   * Left over from manual reordering, which is gone. Clips are ordered
   * newest-first everywhere and nothing reads this any more.
   *
   * The column stays: dropping a column in SQLite means rebuilding the table,
   * and there is no upside worth putting a user's history through that. Writes
   * keep filling it so the schema still matches what the native side creates.
   */
  sortOrder: number;
}
