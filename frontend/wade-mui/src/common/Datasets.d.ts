type DatasetEntry = { value: string; displayValue: string };

declare class Datasets {
  constructor(arr?: DatasetEntry[], setter?: (a: DatasetEntry[]) => void);
  get(): DatasetEntry[];
  set(a: DatasetEntry[]): void;
  push(item: DatasetEntry): void;
  length(): number;
  first(): DatasetEntry | undefined;
}
export default Datasets;
