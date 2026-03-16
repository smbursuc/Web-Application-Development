export default class Datasets {
  constructor(arr, setter) {
    this._data = Array.isArray(arr) ? arr : [];
    this._setter = setter;
  }
  get() {
    return this._data;
  }
  set(arr) {
    this._data = Array.isArray(arr) ? arr : [];
    if (this._setter) this._setter(this._data);
  }
  push(item) {
    this._data.push(item);
    if (this._setter) this._setter(this._data);
  }
  length() {
    return this._data.length;
  }
  first() {
    return this._data && this._data.length > 0 ? this._data[0] : undefined;
  }
}
