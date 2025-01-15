from flask import Flask, request
import classification
from flask_cors import CORS
import correlation
import json

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

@app.route("/api/clusters/cifar10", methods=["GET"])
def get_clusters():
    name = request.args.get("name")
    sort = request.args.get("sort")
    range = int(request.args.get("range"))
    rangeStart = int(request.args.get("rangeStart"))
    data = classification.get_semantic_zoom(name, sort, range, rangeStart)
    return data

@app.route("/api/predictions/cifar10", methods=["GET"])
def get_predictions():
    with open("hierarchial_structure_uri", 'r') as f:
        data = json.load(f)['data']
    return data

@app.route("/api/correlations/cifar10", methods=["GET"])
def get_correlations():
    range = int(request.args.get("range"))
    rangeStart = int(request.args.get("rangeStart"))
    sort = request.args.get("sort")
    sortType = request.args.get("sortType")
    file_path = "processed_data_uri.json"
    data = correlation.correlation(rangeStart, range + rangeStart, sort, sortType, file_path)
    return data

if __name__ == "__main__":
    app.run(debug=True)
