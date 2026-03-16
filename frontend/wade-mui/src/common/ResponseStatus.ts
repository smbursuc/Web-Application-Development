export default class ResponseStatus {
  private _value: string | null;
  private _setter?: (v: string | null) => void;

  constructor(value?: string | null, setter?: (v: string | null) => void) {
    this._value = value === undefined ? null : value;
    this._setter = setter;
  }

  get(): string | null { return this._value; }
  set(v: string | null): void { this._value = v; if (this._setter) this._setter(v); }
}
