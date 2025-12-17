import os
import sys
import urllib.request
import time

def download_file(url, dest_path):
    # Check if file exists and get its size
    existing_size = 0
    if os.path.exists(dest_path):
        existing_size = os.path.getsize(dest_path)
        print(f"Found partial file, resuming from {existing_size / 1024 / 1024:.2f} MB")

    print(f"Downloading {url}...")
    print(f"To: {dest_path}")
    
    try:
        req = urllib.request.Request(url)
        req.add_header('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36')
        
        # Add Range header if resuming
        if existing_size > 0:
            req.add_header('Range', f'bytes={existing_size}-')
        
        try:
            response = urllib.request.urlopen(req)
        except urllib.error.HTTPError as e:
            if e.code == 416: # Range Not Satisfiable - likely fully downloaded
                print("File already fully downloaded (server returned 416).")
                return
            else:
                raise e

        total_size = int(response.info().get('Content-Length', -1))
        if total_size != -1:
            total_size += existing_size # Content-Length is the remaining part
        
        # Open in append mode if resuming, write mode if new
        mode = 'ab' if existing_size > 0 else 'wb'
        
        with open(dest_path, mode) as out_file:
            downloaded = existing_size
            block_size = 8192
            start_time = time.time()
            
            while True:
                buffer = response.read(block_size)
                if not buffer:
                    break
                
                downloaded += len(buffer)
                out_file.write(buffer)
                
                # Report progress
                if total_size > 0:
                    percent = downloaded * 100 / total_size
                    # Print every ~10MB or 5 seconds
                    if downloaded % (10 * 1024 * 1024) < block_size:
                         elapsed = time.time() - start_time
                         speed = (downloaded - existing_size) / elapsed / 1024 / 1024 if elapsed > 0 else 0
                         sys.stdout.write(f"\rProgress: {percent:.2f}% ({downloaded / 1024 / 1024:.2f} MB / {total_size / 1024 / 1024:.2f} MB) Speed: {speed:.2f} MB/s")
                         sys.stdout.flush()
    
        print("\nDownload complete!")
        
    except Exception as e:
        print(f"\nError downloading file: {e}")
        # Do not delete file on error to allow resume
        sys.exit(1)

if __name__ == "__main__":
    # 目标目录
    target_dir = r"D:\yinyue\sd-webui\stable-diffusion-webui\models\Stable-diffusion"
    if not os.path.exists(target_dir):
        os.makedirs(target_dir)
        
    # 模型 URL (使用国内镜像)
    model_url = "https://hf-mirror.com/runwayml/stable-diffusion-v1-5/resolve/main/v1-5-pruned-emaonly.safetensors"
    file_name = "v1-5-pruned-emaonly.safetensors"
    dest_path = os.path.join(target_dir, file_name)
    
    # Retry loop
    max_retries = 10
    for i in range(max_retries):
        try:
            download_file(model_url, dest_path)
            break
        except SystemExit:
            print(f"\nDownload failed, retrying in 5 seconds... ({i+1}/{max_retries})")
            time.sleep(5)
