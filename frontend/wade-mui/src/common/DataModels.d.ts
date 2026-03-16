type DataModelEntry = { value: string; displayValue: string };

declare class DataModels {
  constructor(obj?: Record<string, DataModelEntry[]>, setter?: (o: Record<string, DataModelEntry[]>) => void);
  get(): Record<string, DataModelEntry[]>;
  set(o: Record<string, DataModelEntry[]>): void;
  getFor(dataset: string): DataModelEntry[] | undefined;
  keys(): string[];
}
export default DataModels;
