export default class MaxCache {
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

  getFor(dataset: string, dataModel?: string): any {
    if (!this._data) return undefined;
    const ds = this._data[dataset];
    if (!ds) return undefined;
    if (dataModel === undefined) return ds;
    return ds[dataModel];
  }

  setFor(dataset: string, dataModel: string, value: any): void {
    if (!this._data[dataset]) this._data[dataset] = {};
    this._data[dataset][dataModel] = value;
    if (this._setter) this._setter(this._data);
  }
}
