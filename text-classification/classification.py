from sklearn.cluster import AgglomerativeClustering
from sentence_transformers import SentenceTransformer
import numpy as np
import pandas as pd
import json
import plotly.express as px
import random
from sklearn.preprocessing import normalize
import hdbscan
from groq import Groq

dataset = "bsds300"

client = Groq(api_key="your_api_key")

# Function to parse JSON in batches
def parse_json_in_batches(file_path, batch_size=1000):
    with open(file_path, 'r') as f:
        data = json.load(f)['data']
    for i in range(0, len(data), batch_size):
        batch = data[i:i + batch_size]
        objects_with_probabilities = []
        for entry in batch:
            parts = entry.rsplit(' ', 2)
            obj_name = parts[0]
            probability = float(parts[1])
            uri = parts[2]
            objects_with_probabilities.append((obj_name, probability, uri))
        yield objects_with_probabilities

def process_batch_old(objects_with_probabilities, cluster_offset=0):
    """
    Processes a batch of objects with probabilities and creates a hierarchical structure.

    :param objects_with_probabilities: List of tuples (object_name, probability)
    :param cluster_offset: Offset to ensure global uniqueness of cluster IDs
    :return: Hierarchical JSON for the batch and the number of clusters
    """
    # Extract objects and probabilities
    objects = [item[0] for item in objects_with_probabilities]
    probabilities = [item[1] for item in objects_with_probabilities]
    uris = [item[2] for item in objects_with_probabilities]

    # Step 1: Generate semantic embeddings
    model = SentenceTransformer('all-MiniLM-L6-v2')  # Lightweight and pretrained model
    embeddings = model.encode(objects)

    # Step 2: Perform hierarchical clustering
    clustering = AgglomerativeClustering(n_clusters=None, distance_threshold=1.5, linkage='ward')
    clusters = clustering.fit_predict(embeddings)

    # Step 3: Prepare data for hierarchical JSON structure
    data = pd.DataFrame({
        'Object': objects,
        'Probability': probabilities,
        'URIs': uris,
        'Cluster': clusters
    })

    # Apply global cluster offset
    data['GlobalCluster'] = data['Cluster'] + cluster_offset

    # Create hierarchical JSON with unique cluster names
    tree = {'name': 'Root', 'children': []}

    # Assign objects to their respective clusters
    cluster_mapping = {}
    for global_cluster_id, group in data.groupby('GlobalCluster'):
        cluster_name = f"Cluster {global_cluster_id}"  # Unique cluster name
        cluster_node = cluster_mapping.get(global_cluster_id, {'name': cluster_name, 'children': []})
        cluster_mapping[global_cluster_id] = cluster_node

        # Add objects to the cluster's children
        for _, row in group.iterrows():
            cluster_node['children'].append({
                'name': row['Object'],
                'Probability': row['Probability'],
                'URI': row['URIs']
            })

    # Add all unique clusters to the tree
    tree['children'].extend(cluster_mapping.values())
    return tree, len(cluster_mapping)

def process_batch_hdb(objects_with_probabilities, cluster_offset=0):
    """
    Processes a batch of objects with probabilities and creates a hierarchical structure using HDBSCAN for clustering.
    
    :param objects_with_probabilities: List of tuples (object_name, probability, uri)
    :param cluster_offset: Offset to ensure global uniqueness of cluster IDs
    :return: Hierarchical JSON for the batch and the number of clusters
    """
    # Extract objects, probabilities, and URIs
    objects = [item[0] for item in objects_with_probabilities]
    probabilities = [item[1] for item in objects_with_probabilities]
    uris = [item[2] for item in objects_with_probabilities]

    # Step 1: Generate semantic embeddings (using a lightweight pretrained model)
    model = SentenceTransformer('all-MiniLM-L6-v2')
    embeddings = model.encode(objects)

    # Step 2: Normalize embeddings so that Euclidean distance approximates cosine distance
    embeddings_norm = normalize(embeddings, norm='l2')

    # Step 3: Cluster using HDBSCAN.
    # You can adjust min_cluster_size and other parameters as needed.
    clusterer = hdbscan.HDBSCAN(min_cluster_size=3, metric='euclidean')
    clusters = clusterer.fit_predict(embeddings_norm)
    
    # Option: Handle noise points (labeled as -1) by assigning them to a dedicated "noise" cluster.
    # For example, you could replace -1 with a unique new cluster number:
    noise_label = max(clusters) + 1
    clusters = [noise_label if c == -1 else c for c in clusters]

    # Step 4: Prepare a DataFrame to help build the hierarchical structure
    data = pd.DataFrame({
        'Object': objects,
        'Probability': probabilities,
        'URI': uris,
        'Cluster': clusters
    })
    
    # Apply global cluster offset for unique cluster IDs across batches
    data['GlobalCluster'] = data['Cluster'] + cluster_offset

    # Create hierarchical JSON structure
    tree = {'name': 'Root', 'children': []}
    cluster_mapping = {}
    
    for global_cluster_id, group in data.groupby('GlobalCluster'):
        cluster_name = f"Cluster {global_cluster_id}"  # Unique cluster name
        cluster_node = cluster_mapping.get(global_cluster_id, {'name': cluster_name, 'children': []})
        cluster_mapping[global_cluster_id] = cluster_node

        for _, row in group.iterrows():
            cluster_node['children'].append({
                'name': row['Object'],
                'Probability': row['Probability'],
                'URI': row['URI']
            })
    
    tree['children'].extend(cluster_mapping.values())
    num_clusters = len(cluster_mapping)
    return tree, num_clusters


def cluster_with_groq(object_names):
    """
    Uses Groq's API to group a list of object names into clusters.
    Returns a dictionary mapping each object name to a cluster number (an integer starting at 0).
    """
    # Format object names as a proper JSON list to avoid misinterpretation
    formatted_objects = json.dumps(object_names, ensure_ascii=False)

    # Construct the prompt
    prompt = (
        "You are a highly skilled AI that clusters objects into semantically meaningful groups. "
        "Each object is **one** entity, even if it contains commas (e.g., 'megalith, megalithic structure' is one object, not two). "
        "Cluster the following objects into groups and return a **valid JSON object** that can be parsed with json.loads(). "
        "Your response should strictly be a JSON dictionary where: \n"
        "- Each **key** is the object name (string). \n"
        "- Each **value** is the cluster number (integer starting at 0). \n"
        "- Do not include extra text, explanations, or formatting. \n"
        "- Be **daring with clusters** as long as they make sense semantically. \n"
        "- Avoid single-object clusters unless absolutely necessary. \n\n"
        f"Objects to cluster (JSON format):\n{formatted_objects}"
    )
    try:
        response = client.chat.completions.create(
            model="llama-3.3-70b-versatile",  # Choose an appropriate model
            messages=[{"role": "user", "content": prompt}],
            temperature=0.0
        )
        # Extract the assistant's reply
        assistant_message = response.choices[0].message.content

        print(assistant_message)

        # with open("groq_response.json", "w") as file:
        #     json.dump(assistant_message, file, indent=4)

        # Attempt to parse the returned JSON
        cluster_mapping = json.loads(assistant_message)
    except Exception as e:
        print("Error calling Groq API or parsing its response:", e)
        cluster_mapping = {}
    return cluster_mapping

def process_batch(objects_with_probabilities, cluster_offset=0):
    """
    Processes a batch of objects with probabilities and creates a hierarchical structure
    using Groq's API for clustering.

    :param objects_with_probabilities: List of tuples (object_name, probability, uri)
    :param cluster_offset: Offset to ensure global uniqueness of cluster IDs
    :return: Hierarchical JSON for the batch and the number of clusters
    """
    # Extract objects, probabilities, and URIs
    objects = [item[0] for item in objects_with_probabilities]
    probabilities = [item[1] for item in objects_with_probabilities]
    uris = [item[2] for item in objects_with_probabilities]

    cache = None
    with open("groq_response.json", "r") as file:
        try:
            cache = json.load(file)
        except Exception as e:
            print("Error parsing json")

    if cache:
        print("Used cache for response")
        groq_clusters = cache
    else:
        # Call the Groq-based clustering function
        print("Calling Groq API to cluster objects...")
        groq_clusters = cluster_with_groq(objects)

    # If clustering fails, assign all objects to a default cluster
    if not groq_clusters:
        print("Groq API returned no clustering information. Assigning all objects to Cluster 0.")
        groq_clusters = {obj: 0 for obj in objects}

    # Build a DataFrame from the assignments
    data = pd.DataFrame({
        'Object': objects,
        'Probability': probabilities,
        'URI': uris,
        'Cluster': [groq_clusters.get(obj, -1) for obj in objects]
    })

    # Handle any unassigned clusters by assigning a new cluster number
    if -1 in data['Cluster'].values:
        noise_label = data['Cluster'].max() + 1
        data['Cluster'] = data['Cluster'].apply(lambda x: noise_label if x == -1 else x)

    # Apply a global cluster offset for unique cluster IDs across batches
    data['GlobalCluster'] = data['Cluster'] + cluster_offset

    # Create a hierarchical JSON structure
    tree = {'name': 'Root', 'children': []}
    cluster_mapping = {}
    for global_cluster_id, group in data.groupby('GlobalCluster'):
        cluster_name = f"Cluster {global_cluster_id}"
        cluster_node = cluster_mapping.get(global_cluster_id, {'name': cluster_name, 'children': []})
        cluster_mapping[global_cluster_id] = cluster_node

        for _, row in group.iterrows():
            cluster_node['children'].append({
                'name': row['Object'],
                'Probability': row['Probability'],
                'URI': row['URI']
            })

    tree['children'].extend(cluster_mapping.values())
    num_clusters = len(cluster_mapping)
    return tree, num_clusters

def semantic_zoom_with_batches(file_path, batch_size=1000, showplot=False):
    hierarchical_json = {'name': 'Root', 'children': []}
    cluster_offset = 0  # Start global cluster numbering

    for batch_index, objects_with_probabilities in enumerate(parse_json_in_batches(file_path, batch_size)):
        print(f"Processing batch {batch_index + 1}...")

        # Process batch with current cluster offset
        batch_tree, num_clusters = process_batch(objects_with_probabilities, cluster_offset)

        # Update the global cluster offset
        cluster_offset += num_clusters

        # Merge batch clusters into the global hierarchical structure
        hierarchical_json['children'].extend(batch_tree['children'])

        # Optional visualization
        if showplot:
            data = pd.DataFrame({
                'Object': [item['name'] for cluster in batch_tree['children'] for item in cluster['children']],
                'Parent': [cluster['name'] for cluster in batch_tree['children'] for _ in cluster['children']],
                'Probability': [item['Probability'] for cluster in batch_tree['children'] for item in cluster['children']]
            })

            fig = px.treemap(
                data,
                path=['Parent', 'Object'],
                values='Probability',
                title=f"Semantic Zoom Tree Map - Batch {batch_index + 1}"
            )
            fig.update_traces(root_color="lightgrey")
            fig.update_layout(margin=dict(t=50, l=25, r=25, b=25))
            fig.show()

    # Convert hierarchical JSON to string
    tree_json = json.dumps(hierarchical_json, indent=4)

    return tree_json


def get_semantic_zoom(dataset, name, sort, range, rangeStart):
    """
    Reads hierarchical JSON data from the file and returns a limited number of clusters.

    :param file_path: Path to the hierarchical JSON file
    :param max_clusters: Maximum number of clusters to include in the returned data
    :return: Modified hierarchical JSON data as a Python dictionary
    """
    try:
        file_path = f"hierarchical_structure_uri_{dataset}.json"
        with open(file_path, 'r') as file:
            data = json.load(file)

        # Ensure the data has the expected structure
        if "children" in data and isinstance(data["children"], list):
            if name:
                for cluster in data["children"]:
                    if cluster["name"] == name:
                        return {
                            "name": data["name"],
                            "children": [cluster]
                        }

            if sort:
                if sort == "highest_probability":
                    reverse = True
                else:
                    reverse = False

                # Sort clusters by average probability
                def calculate_average_probability(cluster):
                    if "children" in cluster and isinstance(cluster["children"], list):
                        probabilities = [
                            child.get("Probability", 0.0)
                            for child in cluster["children"]
                        ]
                        return sum(probabilities) / len(probabilities) if probabilities else 0.0
                    return 0.0

                data["children"].sort(
                    key=calculate_average_probability, reverse=reverse
                )

            if not (range is None and rangeStart is None):
                if range > 100:
                    print("Range too big.")
                    return None
                
                nrClusters = len(data["children"])
                if rangeStart < 1 and rangeStart > nrClusters:
                    print("Range start out of range.")
                    return None
                
                rangeEnd = rangeStart + range
                if rangeEnd > nrClusters:
                    print("Invalid range chosen.")
                    return None
                
                limited_clusters = data["children"][rangeStart:rangeEnd]
                return {
                    "name": data["name"],
                    "children": limited_clusters
                }
            else:
                return {
                    "name": data["name"],
                    "children": data["children"]
                }
        else:
            print("Error: Unexpected JSON structure. 'children' key missing or not a list.")
            return None


    except FileNotFoundError:
        print(f"Error: The file {file_path} does not exist.")
        return None
    except json.JSONDecodeError as e:
        print(f"Error decoding JSON: {e}")
        return None

if __name__ == "__main__":
    # File path to your JSON file
    file_path = f"processed_data_uri_{dataset}.json"

    # Perform semantic zoom with batch processing and show plots
    hierarchical_json = semantic_zoom_with_batches(file_path, 5000, showplot=False)

    # Save hierarchical JSON for inspection
    with open(f"hierarchical_structure_uri_{dataset}.json", "w") as f:
        f.write(hierarchical_json)

    print(f"Processing complete. Hierarchical structure saved to hierarchical_structure_{dataset}.json.")
