export type DatasetEntry = { value: string; displayValue: string };

export default class Datasets {
  private _data: DatasetEntry[];
  private _setter?: (a: DatasetEntry[]) => void;

  constructor(arr?: DatasetEntry[], setter?: (a: DatasetEntry[]) => void) {
    this._data = Array.isArray(arr) ? arr : [];
    this._setter = setter;
  }
  get(): DatasetEntry[] {
    return this._data;
  }
  set(arr: DatasetEntry[]): void {
    this._data = Array.isArray(arr) ? arr : [];
    if (this._setter) this._setter(this._data);
  }
  push(item: DatasetEntry): void {
    this._data.push(item);
    if (this._setter) this._setter(this._data);
  }
  length(): number {
    return this._data.length;
  }
  first(): DatasetEntry | undefined {
    return this._data && this._data.length > 0 ? this._data[0] : undefined;
  }
}
