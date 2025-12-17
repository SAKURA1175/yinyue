import sys
import json
import re
import urllib.request
import urllib.error

def get_id_from_url(url):
    match = re.search(r'[?&]id=([0-9]+)', url)
    if match:
        return match.group(1)
    return None

def fetch_song_detail(song_id):
    url = f'http://music.163.com/api/song/detail/?id={song_id}&ids=[{song_id}]'
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
        'Referer': 'http://music.163.com/'
    }
    
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req) as response:
            data = json.loads(response.read().decode('utf-8'))
            if data['code'] == 200 and data['songs']:
                return data['songs'][0]
    except Exception as e:
        sys.stderr.write(f"Error fetching song detail: {str(e)}\n")
    return None

def fetch_lyric(song_id):
    url = f'http://music.163.com/api/song/lyric?os=pc&id={song_id}&lv=-1&kv=-1&tv=-1'
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
        'Referer': 'http://music.163.com/'
    }
    
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req) as response:
            data = json.loads(response.read().decode('utf-8'))
            if data['code'] == 200 and 'lrc' in data:
                return data['lrc']['lyric']
    except Exception as e:
        sys.stderr.write(f"Error fetching lyric: {str(e)}\n")
    return ""

def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "No URL provided"}))
        return

    url = sys.argv[1]
    song_id = get_id_from_url(url)
    
    if not song_id:
        # 尝试直接把输入当做 ID
        if url.isdigit():
            song_id = url
        else:
            print(json.dumps({"error": "Invalid URL or ID"}))
            return

    result = {
        "id": song_id,
        "title": "未知",
        "artist": "未知",
        "album": "未知",
        "cover_url": "",
        "lyrics": ""
    }

    song_info = fetch_song_detail(song_id)
    if song_info:
        result['title'] = song_info.get('name', '未知')
        
        artists = song_info.get('artists', [])
        if artists:
            result['artist'] = ", ".join([a['name'] for a in artists])
            
        album = song_info.get('album', {})
        if album:
            result['album'] = album.get('name', '未知')
            result['cover_url'] = album.get('picUrl', '')

        result['lyrics'] = fetch_lyric(song_id)
    
    print(json.dumps(result, ensure_ascii=False))

if __name__ == "__main__":
    main()
