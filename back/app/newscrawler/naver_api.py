import urllib.request
import urllib.parse
import json

class NaverNewsCrawler:

    # 생성자 매서드
    def __init__(self, client_id, client_secret):
        self.client_id = client_id         # 내 변수에 저장
        self.client_secret = client_secret # 내 변수에 저장


    def _parse_items(self, json_data):
        if json_data is None or 'items' not in json_data:
            return []
        
        results = []
        for item in json_data['items']:
            title = item.get('title', '').replace('<b>', '').replace('</b>', '').replace('&quot;', '"')
            description = item.get('description', '').replace('<b>', '').replace('</b>', '').replace('&quot;', '"')
            results.append({
                'title': title,
                'link': item.get('link'),
                'desc': description
            })
        return results

    # 뉴스 검색 매서드
    def search(self, keyword, display=5):
        encText = urllib.parse.quote(keyword)
        # 뉴스 검색 api url / 현재 : 뉴스로 고정
        url = f"https://openapi.naver.com/v1/search/news?query={encText}&display={display}&sort=date"

        request = urllib.request.Request(url)
        
        request.add_header("X-Naver-Client-Id", self.client_id)
        request.add_header("X-Naver-Client-Secret", self.client_secret)

        try:
            response = urllib.request.urlopen(request)
            if response.getcode() == 200:
                json_data = json.loads(response.read().decode('utf-8'))
                return self._parse_items(json_data)
            else:
                return None
        except Exception as e:
            print(f"오류 발생: {e}")
            return None