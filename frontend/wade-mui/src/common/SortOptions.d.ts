type SortOptionEntry = { value: string; displayValue: string };

declare class SortOptions {
  constructor(obj?: Record<string, any>, setter?: (o: Record<string, any>) => void);
  get(): Record<string, any>;
  set(o: Record<string, any>): void;
  getFor(dataset: string): any;
  getSortBy(dataset: string, datasetType: string): SortOptionEntry[] | undefined;
  getSortType(dataset: string, datasetType: string): SortOptionEntry[] | undefined;
  setSortBy(dataset: string, datasetType: string, arr: SortOptionEntry[]): void;
  setSortType(dataset: string, datasetType: string, arr: SortOptionEntry[]): void;
}
export default SortOptions;
