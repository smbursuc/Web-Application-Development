export default class Results {
  private _objects: any[];
  private _matrix: any;
  private _setObjects?: (o: any[]) => void;
  private _setMatrix?: (m: any) => void;

  constructor(objects?: any[], matrix?: any, setObjects?: (o: any[]) => void, setMatrix?: (m: any) => void) {
    this._objects = Array.isArray(objects) ? objects : [];
    this._matrix = matrix;
    this._setObjects = setObjects;
    this._setMatrix = setMatrix;
  }

  getObjects(): any[] { return this._objects; }
  setObjects(o: any[]): void { this._objects = o || []; if (this._setObjects) this._setObjects(this._objects); }

  getMatrix(): any { return this._matrix; }
  setMatrix(m: any): void { this._matrix = m; if (this._setMatrix) this._setMatrix(this._matrix); }

  setAll(o: { objects?: any[]; matrix?: any }): void {
    if (o.objects !== undefined) this.setObjects(o.objects);
    if (o.matrix !== undefined) this.setMatrix(o.matrix);
  }
}
