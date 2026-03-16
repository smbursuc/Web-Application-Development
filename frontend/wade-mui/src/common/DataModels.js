export default class DataModels {
  constructor(obj, setter) {
    this._data = obj || {};
    this._setter = setter;
  }
  get() {
    return this._data;
  }
  set(obj) {
    this._data = obj || {};
    if (this._setter) this._setter(this._data);
  }
  getFor(dataset) {
    return this._data && this._data[dataset] ? this._data[dataset] : undefined;
  }
  keys() {
    return Object.keys(this._data || {});
  }
}
