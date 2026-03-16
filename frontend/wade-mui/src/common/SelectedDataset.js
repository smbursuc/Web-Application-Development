export default class SelectedDataset {
  constructor(value, setter) {
    this._value = value;
    this._setter = setter;
  }
  get() {
    return this._value;
  }
  set(value) {
    this._value = value;
    if (this._setter) this._setter(value);
  }
}
