export default class StaticMetadata {
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

  keys(): string[] {
    return Object.keys(this._data || {});
  }

  getEntry(name: string): any {
    return this._data ? this._data[name] : undefined;
  }
}
