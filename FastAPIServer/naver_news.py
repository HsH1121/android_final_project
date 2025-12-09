import json
import urllib.parse
import urllib.request
from typing import List, Dict

from bs4 import BeautifulSoup

class NaverNewsCrawler:
    """네이버 뉴스 검색 OpenAPI 래퍼"""

    def __init__(self, client_id: str, client_secret: str) -> None:
        self.client_id = client_id
        self.client_secret = client_secret

    def _parse_items(self, json_data: dict | None) -> List[Dict[str, str]]:
        if not json_data or "items" not in json_data:
            return []

        results: List[Dict[str, str]] = []
        for item in json_data["items"]:
            title = (
                item.get("title", "")
                .replace("<b>", "")
                .replace("</b>", "")
                .replace("&quot;", '"')
            )
            description = (
                item.get("description", "")
                .replace("<b>", "")
                .replace("</b>", "")
                .replace("&quot;", '"')
            )
            results.append(
                {
                    "title": title,
                    "link": item.get("link", ""),
                    "desc": description,
                }
            )
        return results

    def search(self, keyword: str, display: int = 5) -> List[Dict[str, str]]:
        """키워드 기준 최신 뉴스 검색 (기본 5개)"""
        enc_text = urllib.parse.quote(keyword)
        url = (
            "https://openapi.naver.com/v1/search/news"
            f"?query={enc_text}&display={display}&sort=date"
        )

        request = urllib.request.Request(url)
        request.add_header("X-Naver-Client-Id", self.client_id)
        request.add_header("X-Naver-Client-Secret", self.client_secret)

        try:
            with urllib.request.urlopen(request) as response:
                if response.getcode() == 200:
                    json_data = json.loads(response.read().decode("utf-8"))
                    return self._parse_items(json_data)
                return []
        except Exception as e:  # 단순 로그
            print(f"[NaverNewsCrawler] 오류 발생: {e}")
            return []


def fetch_article_content(url: str, timeout: int = 5) -> str:
    """뉴스 기사 링크에서 본문 텍스트만 최대한 추출"""
    if not url:
        return ""

    try:
        req = urllib.request.Request(
            url,
            headers={
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) "
                    "Chrome/120.0 Safari/537.36"
                )
            },
        )
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            html = resp.read().decode("utf-8", errors="ignore")
    except Exception as e:
        print(f"[fetch_article_content] 요청 실패: {e}")
        return ""

    soup = BeautifulSoup(html, "html.parser")

    # 네이버/일반 뉴스에서 자주 쓰이는 선택자들
    selectors = [
        "#dic_area",  # 네이버 뉴스 본문
        "#newsct_article",
        "#articeBody",
        "#articleBodyContents",
        "article",
    ]

    for sel in selectors:
        node = soup.select_one(sel)
        if node:
            texts = [t.strip() for t in node.stripped_strings]
            return "\n".join(texts)

    # 마지막 폴백: body 전체 텍스트
    if soup.body:
        texts = [t.strip() for t in soup.body.stripped_strings]
        return "\n".join(texts)

    return ""
