type SortOptionEntry = { value: string; displayValue: string };

export default class SortOptions {
  private _data: Record<string, any>;
  private _setter?: (o: Record<string, any>) => void;

  constructor(obj?: Record<string, any>, setter?: (o: Record<string, any>) => void) {
    this._data = obj || {};
    this._setter = setter;
  }
  get(): Record<string, any> {
    return this._data;
  }
  set(obj: Record<string, any>): void {
    this._data = obj || {};
    if (this._setter) this._setter(this._data);
  }
  getFor(dataset: string): any | undefined {
    return this._data && this._data[dataset] ? this._data[dataset] : undefined;
  }
  getSortBy(dataset: string, datasetType: string): SortOptionEntry[] | undefined {
    try {
      return this._data[dataset][datasetType]["sortBy"] as SortOptionEntry[];
    } catch (e) {
      return undefined;
    }
  }
  getSortType(dataset: string, datasetType: string): SortOptionEntry[] | undefined {
    try {
      return this._data[dataset][datasetType]["sortType"] as SortOptionEntry[];
    } catch (e) {
      return undefined;
    }
  }
  setSortBy(dataset: string, datasetType: string, arr: SortOptionEntry[]): void {
    if (!this._data[dataset]) this._data[dataset] = {};
    if (!this._data[dataset][datasetType]) this._data[dataset][datasetType] = {};
    this._data[dataset][datasetType]["sortBy"] = arr;
    if (this._setter) this._setter(this._data);
  }
  setSortType(dataset: string, datasetType: string, arr: SortOptionEntry[]): void {
    if (!this._data[dataset]) this._data[dataset] = {};
    if (!this._data[dataset][datasetType]) this._data[dataset][datasetType] = {};
    this._data[dataset][datasetType]["sortType"] = arr;
    if (this._setter) this._setter(this._data);
  }
}
