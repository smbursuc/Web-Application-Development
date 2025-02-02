from flask import Flask, request
import classification
from flask_cors import CORS
import correlation
import json

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

@app.route("/api/clusters/<dataset>", methods=["GET"])
def get_clusters(dataset):
    name = request.args.get("name")
    sort = request.args.get("sort")
    range = int(request.args.get("range"))
    rangeStart = int(request.args.get("rangeStart"))
    data = classification.get_semantic_zoom(dataset, name, sort, range, rangeStart)
    return data

@app.route("/api/predictions/cifar10", methods=["GET"])
def get_predictions():
    with open("hierarchial_structure_uri", 'r') as f:
        data = json.load(f)['data']
    return data

@app.route("/api/correlations/<dataset>", methods=["GET"])
def get_correlations(dataset):
    range = int(request.args.get("range"))
    rangeStart = int(request.args.get("rangeStart"))
    sort = request.args.get("sort")
    sortType = request.args.get("sortType")
    data = correlation.correlation(dataset, rangeStart, range + rangeStart, sort, sortType)
    return data

@app.route("/api/metadata/<dataset>/<representation>", methods=["GET"])
def get_metadata(dataset, representation):
    metadata = {}
    size = 0
    if representation == "heatmap":
        data = correlation.correlation(dataset, None, None, None, None)
        size = len(data["objects"])
    elif representation == "clusters":
        data = classification.get_semantic_zoom(dataset, None, None, None, None)
        size = len(data["children"])
    metadata["size"] = size
    print(size)
    return metadata

if __name__ == "__main__":
    app.run(debug=True)
