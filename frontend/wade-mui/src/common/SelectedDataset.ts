export default class SelectedDataset {
  private _value: string | undefined;
  private _setter?: (v: string) => void;

  constructor(value?: string, setter?: (v: string) => void) {
    this._value = value;
    this._setter = setter;
  }
  get(): string | undefined {
    return this._value;
  }
  set(value: string): void {
    this._value = value;
    if (this._setter) this._setter(value);
  }
}
