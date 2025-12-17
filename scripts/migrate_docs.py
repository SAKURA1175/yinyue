import os
import shutil
import hashlib
import datetime

# Configuration
SOURCE_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS_ROOT = os.path.join(SOURCE_ROOT, "docs")
MAPPING = {
    "NETEASE_FEATURE.md": "technical",
    "DEV_PLAN.md": "technical",
    "音乐.md": "design",
    "AI_MODULE_README.md": "technical",
    "启动指南.txt": "operations",
    "【快速开始】3分钟快速部署指南.txt": "operations",
    "SD部署说明.txt": "operations"
}

def calculate_md5(file_path):
    hash_md5 = hashlib.md5()
    with open(file_path, "rb") as f:
        for chunk in iter(lambda: f.read(4096), b""):
            hash_md5.update(chunk)
    return hash_md5.hexdigest()

def migrate_docs():
    print(f"Starting documentation migration from {SOURCE_ROOT} to {DOCS_ROOT}...")
    
    migrated_files = []
    
    # 1. Scan and Move Files
    for filename, category in MAPPING.items():
        # Search in root and backend/ (special case for AI_MODULE_README.md)
        src_path = os.path.join(SOURCE_ROOT, filename)
        if not os.path.exists(src_path):
            # Check backend root for AI readme
            if filename == "AI_MODULE_README.md":
                src_path = os.path.join(SOURCE_ROOT, "backend", filename)
            
        if os.path.exists(src_path):
            dest_dir = os.path.join(DOCS_ROOT, category)
            if not os.path.exists(dest_dir):
                os.makedirs(dest_dir)
            
            dest_path = os.path.join(dest_dir, filename)
            
            # Calculate MD5 before move
            md5_hash = calculate_md5(src_path)
            
            # Copy file (preserve metadata)
            shutil.copy2(src_path, dest_path)
            print(f"Migrated: {filename} -> {category}/ (MD5: {md5_hash})")
            
            migrated_files.append({
                "name": filename,
                "category": category,
                "md5": md5_hash,
                "date": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            })
        else:
            print(f"Warning: Source file {filename} not found.")

    # 2. Generate Index (README.md)
    index_path = os.path.join(DOCS_ROOT, "README.md")
    with open(index_path, "w", encoding="utf-8") as f:
        f.write("# 项目文档索引 (Documentation Index)\n\n")
        f.write(f"Generated on: {datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        
        current_cat = ""
        # Group by category
        from itertools import groupby
        migrated_files.sort(key=lambda x: x["category"])
        
        for category, items in groupby(migrated_files, key=lambda x: x["category"]):
            f.write(f"## {category.capitalize()}\n")
            for item in items:
                f.write(f"- [{item['name']}]({category}/{item['name']}) (MD5: `{item['md5']}`)\n")
            f.write("\n")
            
    print(f"Index generated at {index_path}")
    print("Migration completed.")

if __name__ == "__main__":
    migrate_docs()
