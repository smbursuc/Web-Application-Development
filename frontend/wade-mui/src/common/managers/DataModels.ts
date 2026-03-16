export type DataModelEntry = { value: string; displayValue: string };

export default class DataModels {
  private _data: Record<string, DataModelEntry[]>;
  private _setter?: (o: Record<string, DataModelEntry[]>) => void;

  constructor(obj?: Record<string, DataModelEntry[]>, setter?: (o: Record<string, DataModelEntry[]>) => void) {
    this._data = obj || {};
    this._setter = setter;
  }
  get(): Record<string, DataModelEntry[]> {
    return this._data;
  }
  set(obj: Record<string, DataModelEntry[]>): void {
    this._data = obj || {};
    if (this._setter) this._setter(this._data);
  }
  getFor(dataset: string): DataModelEntry[] | undefined {
    return this._data && this._data[dataset] ? this._data[dataset] : undefined;
  }
  keys(): string[] {
    return Object.keys(this._data || {});
  }
}
