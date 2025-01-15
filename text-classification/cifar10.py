import os
import requests
import tarfile
from urllib.request import urlretrieve
import pickle
import numpy as np
from PIL import Image

# Define relative paths
dataset_filename = "cifar-10-python.tar.gz"
dataset_folder = "cifar-10-batches-py"
script_root = os.path.dirname(os.path.abspath(__file__))
dataset_path = os.path.join(script_root, dataset_filename)
extracted_path = os.path.join(script_root, dataset_folder)
upload_url = "http://localhost:8081/api/upload-image"

# Step 1: Download CIFAR-10 dataset
cifar10_url = "https://www.cs.toronto.edu/~kriz/cifar-10-python.tar.gz"

if not os.path.exists(dataset_path):
    print("Downloading CIFAR-10 dataset...")
    urlretrieve(cifar10_url, dataset_path)

# Step 2: Extract the dataset
if not os.path.exists(extracted_path):
    print("Extracting CIFAR-10 dataset...")
    with tarfile.open(dataset_path, "r:gz") as tar:
        tar.extractall(path=script_root)

# Step 3: Load CIFAR-10 batches
def load_batch(file):
    with open(file, 'rb') as fo:
        batch = pickle.load(fo, encoding='bytes')
    return batch[b'data'], batch[b'labels']

batch_files = [
    os.path.join(extracted_path, f"data_batch_{i}")
    for i in range(1, 6)  # CIFAR-10 has 5 batches for training
]

# Step 4: Process and upload each image
uploaded_count = 0
total_images = 0

for batch_file in batch_files:
    print(f"Processing batch: {batch_file}")
    images, labels = load_batch(batch_file)
    total_images += len(images)
    
    for i, image_data in enumerate(images):
        # Convert image data to PIL format
        image_array = image_data.reshape(3, 32, 32).transpose(1, 2, 0)
        image = Image.fromarray(image_array)

        # Save image temporarily
        image_file_path = os.path.join(script_root, f"temp_image_{i}.png")
        image.save(image_file_path)

        # Upload image
        try:
            with open(image_file_path, 'rb') as image_file:
                response = requests.post(
                    upload_url,
                    files={"image": (f"temp_image_{i}.png", image_file, "image/png")}
                )
            if response.status_code == 200:
                uploaded_count += 1
                print(f"Uploaded {i + 1}/{len(images)}: {response.text}")
            else:
                print(f"Failed to upload image {i + 1}: {response.status_code}, {response.text}")
        except Exception as e:
            print(f"Error uploading image {i + 1}: {e}")

        # Clean up temporary image file
        if os.path.exists(image_file_path):
            os.remove(image_file_path)

print(f"Uploaded {uploaded_count}/{total_images} images.")
