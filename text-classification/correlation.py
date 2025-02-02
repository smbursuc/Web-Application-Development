from sentence_transformers import SentenceTransformer, util
from sklearn.metrics.pairwise import cosine_similarity
import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import json
import torch


def prepare_data(start, end, filepath):
    with open(filepath, 'r') as f:
        data = json.load(f)['data']

    if (start != None and end != None):
        if (end > len(data)):
            data = data[start:len(data)]
        else:
            data = data[start:end]

    objects = []
    for row in data:
        parts = row.rsplit(" ", 2)
        objects.append(parts[0][:20]) # for long object names reduce the size to not destroy plotly plot

    return objects

def correlation(dataset, start, end, sort, sortType, showplot=False):
    """
    Calculate the semantic similarity of objects and visualize it as a heatmap.

    :param start: Start index for objects
    :param end: End index for objects
    :param sort: Sorting criteria ("highest_probability" or "lowest_probability")
    :param sortType: Sorting type ("average_similarity" or "strongest_pair")
    :param filepath: Path to the file containing object data
    :param showplot: Whether to display a heatmap plot
    :return: A dictionary containing the objects and their similarity matrix
    """
    filepath = f"processed_data_uri_{dataset}.json"

    objects = prepare_data(start, end, filepath)

    # Step 1: Generate embeddings
    model = SentenceTransformer('all-mpnet-base-v2')
    embeddings = model.encode(objects, convert_to_tensor=True)

    # Normalize embeddings (so cosine similarity becomes simply the dot product)
    embeddings_norm = torch.nn.functional.normalize(embeddings, p=2, dim=1)

    # Compute cosine similarity using SentenceTransformer's utility function
    similarity_matrix = util.cos_sim(embeddings_norm, embeddings_norm)

    # Step 2: Calculate cosine similarity
    similarity_matrix = cosine_similarity(embeddings)

    # Step 3: Sort objects if requested
    if sortType:
        if sortType == "average_similarity":
            # Calculate average similarity
            sort_metric = similarity_matrix.mean(axis=1)
        elif sortType == "strongest_pair":
            # Calculate strongest similarity
            sort_metric = similarity_matrix.max(axis=1)
        else:
            raise ValueError("Invalid sortType option. Use 'average_similarity' or 'strongest_pair'.")

        # Determine sort direction
        reverse = sort == "highest_probability"

        # Get sorted indices
        sorted_indices = sort_metric.argsort()[::-1] if reverse else sort_metric.argsort()

        # Sort objects and similarity matrix
        objects = [objects[i] for i in sorted_indices]
        similarity_matrix = similarity_matrix[sorted_indices][:, sorted_indices]

    # If sortType is None, do not sort
    else:
        sorted_indices = range(len(objects))  # Keep the original order

    # JSONify the data
    similarity_data = {
        "objects": objects,
        "matrix": similarity_matrix.tolist()  # Convert NumPy array to a Python list
    }

    # Save hierarchical JSON for inspection
    f_data = json.dumps(similarity_data, indent=4)
    with open("similarity_data_bsds300.json", "w") as f:
        f.write(f_data)

    if showplot:
        # Create a DataFrame for visualization
        similarity_df = pd.DataFrame(similarity_matrix, index=objects, columns=objects)

        # Plot the heatmap
        plt.figure(figsize=(12, 10))
        sns.heatmap(similarity_df, annot=False, cmap="coolwarm", xticklabels=True, yticklabels=True)
        plt.title(f"Semantic Similarity Heatmap (Sorted by {sortType} {sort})")
        plt.show()

    return similarity_data

if __name__ == "__main__":
    correlation("bsds300",0,9999,"highest_probability","strongest_pair",False)
