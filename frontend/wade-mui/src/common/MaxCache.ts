export default class MaxCache {
  private _data: Record<string, Record<string, any>>;
  private _setter?: (o: Record<string, Record<string, any>>) => void;

  constructor(obj?: Record<string, Record<string, any>>, setter?: (o: Record<string, Record<string, any>>) => void) {
    this._data = obj || {};
    this._setter = setter;
  }

  get(): Record<string, Record<string, any>> {
    return this._data;
  }

  set(obj: Record<string, Record<string, any>>): void {
    this._data = obj || {};
    if (this._setter) this._setter(this._data);
  }

  getFor(dataset: string): Record<string, any> | undefined {
    return this._data ? this._data[dataset] : undefined;
  }

  setFor(dataset: string, model: string, value: any): void {
    if (!this._data[dataset]) this._data[dataset] = {};
    this._data[dataset][model] = value;
    if (this._setter) this._setter(this._data);
  }
}
