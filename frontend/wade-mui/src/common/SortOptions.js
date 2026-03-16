export default class SortOptions {
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
  getSortBy(dataset, datasetType) {
    try {
      return this._data[dataset][datasetType]["sortBy"];
    } catch (e) {
      return undefined;
    }
  }
  getSortType(dataset, datasetType) {
    try {
      return this._data[dataset][datasetType]["sortType"];
    } catch (e) {
      return undefined;
    }
  }
  setSortBy(dataset, datasetType, arr) {
    if (!this._data[dataset]) this._data[dataset] = {};
    if (!this._data[dataset][datasetType]) this._data[dataset][datasetType] = {};
    this._data[dataset][datasetType]["sortBy"] = arr;
    if (this._setter) this._setter(this._data);
  }
  setSortType(dataset, datasetType, arr) {
    if (!this._data[dataset]) this._data[dataset] = {};
    if (!this._data[dataset][datasetType]) this._data[dataset][datasetType] = {};
    this._data[dataset][datasetType]["sortType"] = arr;
    if (this._setter) this._setter(this._data);
  }
}
