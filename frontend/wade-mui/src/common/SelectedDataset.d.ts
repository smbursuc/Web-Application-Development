declare class SelectedDataset {
  constructor(value: string | undefined, setter?: (v: string) => void);
  get(): string | undefined;
  set(v: string): void;
}
export default SelectedDataset;
