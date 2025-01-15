from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity
import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
import json


def prepare_data(start, end, filepath):
    with open(filepath, 'r') as f:
        data = json.load(f)['data']

    if (start != None and end != None):
        data = data[start:end]

    objects = []
    for row in data:
        parts = row.rsplit(" ", 2)
        objects.append(parts[0][:20]) # for long object names reduce the size to not destroy plotly plot

    return objects

def correlation(start, end, sort, sortType, filepath, showplot=False):
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
    objects = prepare_data(start, end, filepath)

    # Step 1: Generate embeddings
    model = SentenceTransformer('all-MiniLM-L6-v2')
    embeddings = model.encode(objects)

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
    with open("similarity_data.json", "w") as f:
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
    correlation(1,500,"highest_probability","strongest_pair","processed_data_uri.json",False)
