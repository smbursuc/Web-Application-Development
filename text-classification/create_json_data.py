import os
import requests
import json
import sys

# Configurations
folder_path = "E:\\repos\\Web-Application-Development\\WADe\\uploaded-images"  # Replace with the path to your folder
base_url = "http://localhost:8081/api/files/"
api_url = "http://localhost:8081/api/process-image"  # Replace with your processing endpoint
output_file = "processed_data_uri.json"
batch_size = 1  # Adjust batch size as needed

# Function to generate file links
def generate_image_links(folder_path, base_url):
    links = []
    for filename in sorted(os.listdir(folder_path)):
        if filename.lower().endswith(('.png', '.jpg', '.jpeg', '.bmp', '.gif')):
            # print(filename)
            link = f"{base_url}{filename}"  # Directly append the filename to the base URL
            links.append(link)
    # sys.exit(-1)
    return links

# Function to process images in batches
def process_images_in_batches(links, api_url, batch_size):
    processed_data = []
    total_links = len(links)
    
    for i in range(0, total_links, batch_size):
        batch = links[i:i+batch_size]
        payload = {
            "responseType": "processed",
            "imagePaths": batch
        }
        
        try:
            print(f"Processing batch {i // batch_size + 1}...")
            response = requests.post(api_url, json=payload)
            if response.status_code == 200:
                response_data = response.json()
                if "data" in response_data:
                    processed_data.extend(response_data["data"])
                else:
                    print(f"Unexpected response format: {response_data}")
            else:
                print(f"Failed to process batch {i // batch_size + 1}: {response.status_code}, {response.text}")
        except Exception as e:
            print(f"Error processing batch {i // batch_size + 1}: {e}")

    return processed_data

# Main script
if __name__ == "__main__":
    # Generate image links
    print("Generating image links...")
    image_links = generate_image_links(folder_path, base_url)
    print(f"Generated {len(image_links)} image links.")

    # Process images in batches
    print("Processing images in batches...")
    processed_data = process_images_in_batches(image_links, api_url, batch_size)

    # Save processed data to JSON file
    print("Saving processed data to JSON file...")
    output = {"data": processed_data}
    with open(output_file, "w") as json_file:
        json.dump(output, json_file, indent=4)

    print(f"Processed data saved to {output_file}.")
