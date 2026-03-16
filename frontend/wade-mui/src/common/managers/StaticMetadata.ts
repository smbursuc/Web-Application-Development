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

  getDisplayValue(dataset: string): string | undefined {
    if (!dataset) return undefined;
    const entry = this._data ? this._data[dataset] : undefined;
    return entry && entry.displayValue ? entry.displayValue : undefined;
  }

  getFeatures(dataset: string): any[] {
    if (!dataset) return [];
    const entry = this._data ? this._data[dataset] : undefined;
    return entry && Array.isArray(entry.features) ? entry.features : [];
  }

  getSummary(dataset: string): string | undefined {
    if (!dataset) return undefined;
    const entry = this._data ? this._data[dataset] : undefined;
    return entry && entry.summary ? entry.summary : undefined;
  }

  getSource(dataset: string): string | undefined {
    if (!dataset) return undefined;
    const entry = this._data ? this._data[dataset] : undefined;
    return entry && entry.source ? entry.source : undefined;
  }

  getDataModels(dataset: string, datasetType?: string): Array<{ name: string; displayValue: string }> {
    // Prefer dataset-specific models. New metadata may provide `datasetInfo` mapping
    // datasetType -> ["rdf","json"], so support that shape. Fallback to common
    // `dataModels` when necessary.
    const common = this._data && this._data["common"] ? this._data["common"] : undefined;
    const commonModels = common && Array.isArray(common.dataModels) ? common.dataModels : [];
    const entry = this._data ? this._data[dataset] : undefined;

    // New shapes supported. Prefer dataset-specific datasetInfo over common:
    // - entry.datasetInfo[datasetType] can be an array of strings e.g. ["rdf","json"]
    // - or an array of objects e.g. [{ name: 'json', displayValue: 'JSON' }, ...]
    // - or nested as entry.datasetInfo.datasetTypeAndDataTypes[datasetType]
    // If datasetType is provided, we must verify that this dataset actually supports it.
    // If we have datasetInfo, we use that as the source of truth.
    if (entry && entry.datasetInfo && datasetType) {
      let raw: any[] | undefined = undefined;
      // direct datasetInfo[datasetType] (case-sensitive) or case-insensitive lookup
      if (Array.isArray(entry.datasetInfo[datasetType])) {
        raw = entry.datasetInfo[datasetType];
      } else if (entry.datasetInfo.datasetTypeAndDataTypes) {
        const dtMap = entry.datasetInfo.datasetTypeAndDataTypes;
        if (Array.isArray(dtMap[datasetType])) {
          raw = dtMap[datasetType];
        } else {
          // try case-insensitive key match (e.g. HEATMAP vs heatmap)
          const keys = Object.keys(dtMap || {});
          const matchKey = keys.find((k) => k.toLowerCase() === datasetType.toLowerCase());
          if (matchKey && Array.isArray(dtMap[matchKey])) {
            raw = dtMap[matchKey];
          }
        }
      } else if (Array.isArray(entry.datasetInfo.dataModels)) {
        // allow datasetInfo.dataModels as a fallback shape, but ONLY if we are SURE it's for this type
        // For now, let's assume if it's there AND we don't have explicit type mapping, we allow it.
        raw = entry.datasetInfo.dataModels;
      }

      if (Array.isArray(raw) && raw.length > 0) {
        const result: Array<{ name: string; displayValue: string }> = [];
        for (const item of raw) {
          if (!item) continue;
          // if item is an object with a name/displayValue, use it directly
          if (typeof item === "object" && (item as any).name) {
            const display = (item as any).displayValue ? (item as any).displayValue : (item as any).name;
            result.push({ name: String((item as any).name).toLowerCase(), displayValue: display });
            continue;
          }
          // item is likely a string - normalize to lowercase name and try to enrich from commonModels
          if (typeof item === "string") {
            const name = item.toLowerCase();
            const found = commonModels.find((m: any) => m && String(m.name).toLowerCase() === name);
            if (found) {
              // normalize returned name to lowercase to keep consistency
              result.push({ name: String(found.name).toLowerCase(), displayValue: found.displayValue ? found.displayValue : found.name });
            } else {
              result.push({ name: name, displayValue: item });
            }
          }
        }
        return result;
      }
      
      // If datasetInfo is present but the requested datasetType is not found/empty,
      // it means this dataset DOES NOT support this view.
      return [];
    }

    // If we are here, it means either 'entry' is null or 'datasetInfo' is missing.
    // For default datasets (bsds300, cifar10), they might not have datasetInfo in some states,
    // so we fall back to the old behavior: entry.dataModels or commonModels.
    // BUT we only do this if it's a default dataset or if no datasetType was requested.
    const isDefault = dataset === "bsds300" || dataset === "cifar10";
    if (datasetType && !isDefault && entry && !entry.datasetInfo) {
        // If it's a dynamic dataset and has no datasetInfo, we can't be sure it supports the type.
        // Return empty so it gets filtered out of the view.
        return [];
    }

    // Old shape: entry.dataModels is an array of objects
    const specific = entry && Array.isArray(entry.dataModels) ? entry.dataModels : [];
    // Build deduped list: keep specific order, append common entries not present
    const map = new Map();
    for (const mod of specific) {
      if (mod && mod.name) map.set(String(mod.name).toLowerCase(), mod);
    }
    for (const mod of commonModels) {
      const name = String(mod.name).toLowerCase();
      if (mod && mod.name && !map.has(name)) map.set(name, mod);
    }
    return Array.from(map.values());
  }

  getSortOptions(dataset: string): any {
    // Merge common.sortOptions with dataset-specific sortOptions.
    const common = this._data && this._data["common"] ? this._data["common"] : undefined;
    const commonSort = common && common.sortOptions ? common.sortOptions : {};
    if (!dataset) return { ...commonSort };
    const entry = this._data ? this._data[dataset] : undefined;
    const specific = entry && entry.sortOptions ? entry.sortOptions : {};

    const merged: Record<string, any[]> = {};
    const keys = new Set([...(Object.keys(commonSort || {})), ...(Object.keys(specific || {}))]);
    for (const key of keys) {
      const commonArr = Array.isArray(commonSort[key]) ? commonSort[key] : [];
      const specArr = Array.isArray(specific[key]) ? specific[key] : [];
      // Deduplicate by 'name', prefer dataset-specific ordering/values
      const map = new Map();
      for (const item of specArr) {
        if (item && item.name) map.set(item.name, item);
      }
      for (const item of commonArr) {
        if (item && item.name && !map.has(item.name)) map.set(item.name, item);
      }
      merged[key] = Array.from(map.values());
    }
    return merged;
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
