import requests
import tarfile
import io

# URL of the tar.gz archive to download (change this to your archive URL)
archive_url = "https://www2.eecs.berkeley.edu/Research/Projects/CS/vision/bsds/BSDS300-images.tgz"
# URL to which images will be uploaded
upload_url = "http://localhost:8081/api/upload-image"

def download_archive(url):
    print(f"Downloading archive from {url} …")
    response = requests.get(url)
    response.raise_for_status()  # abort if download fails
    # Wrap the binary content in a BytesIO object so it can be processed in memory
    return io.BytesIO(response.content)

def extract_and_upload_images(archive_file, upload_url):
    # Open the tar.gz archive from the file-like object
    with tarfile.open(fileobj=archive_file, mode="r:") as tar:
        # Get a list of all members (files/directories) in the archive
        members = tar.getmembers()
        image_index = 0
        for member in members:
            # Process only files (skip directories, etc.)
            if not member.isfile():
                continue
            # Check for image file extensions (adjust as needed)
            if not member.name.lower().endswith((".png", ".jpg", ".jpeg")):
                continue

            print(f"Processing file: {member.name}")
            # Extract the file as a file-like object
            extracted_file = tar.extractfile(member)
            if extracted_file is None:
                continue

            image_data = extracted_file.read()
            # Wrap the image bytes in a BytesIO object
            image_file = io.BytesIO(image_data)
            image_file.seek(0)  # Ensure we're at the start of the file

            # Prepare the files payload for the POST request
            files = {
                "image": (f"temp_image_{image_index}.png", image_file, "image/png")
            }
            print(f"Uploading image index {image_index} …")
            response = requests.post(upload_url, files=files)
            if response.status_code == 200:
                print(f"Upload successful for image index {image_index}")
            else:
                print(f"Upload failed for image index {image_index}: {response.status_code} - {response.text}")
            image_index += 1

def main():
    try:
        archive_file = download_archive(archive_url)
        extract_and_upload_images(archive_file, upload_url)
    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    main()
