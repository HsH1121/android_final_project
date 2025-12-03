from naver_api import NaverNewsCrawler 

# 네이버 api 발급받은 키를 입력
MY_CLIENT_ID = "_7YgidD1Y8h9wKJ7yMTq"
MY_CLIENT_SECRET = "Jl96jsgzPg"

def print_news_list(news_data):
    #뉴스 리스트 출력 
    if not news_data:
        print("    결과가 없습니다.")
        return

    for i, news in enumerate(news_data, 1):
        print(f"[{i}] {news['title']}")
        print(f"    링크: {news['link']}")
        print("-" * 50)

def show_headlines(crawler):
    
    #헤드라인 뉴스 출력 
    
    print("\n📰 [오늘의 헤드라인 뉴스 Top 3]를 불러옵니다... (새로고침 완료)")
    # 헤드라인 뉴스 키워드 검색 / 현재 : 주요뉴스
    data = crawler.search("주요뉴스", display=3)
    print_news_list(data)

def main():
    # 크롤러 생성
    crawler = NaverNewsCrawler(MY_CLIENT_ID, MY_CLIENT_SECRET)

    # 프로그램 시작 시 헤드라인 뉴스 최초 1회 실행
    show_headlines(crawler)

    # 3. 무한 반복 구간
    while True:
        print("\n" + "="*50)
        print("[명령어 안내] q: 종료 / h: 헤드라인 새로고침")
        keyword = input("검색어 입력: ")

        # [종료 기능]
        if keyword == 'q':
            print("\n프로그램을 종료합니다. 👋")
            break
        
        # 헤드라인 뉴스 새로고침 기능
        elif keyword == 'h':
            show_headlines(crawler)
            continue 

        # 공백 입력 방지
        if keyword.strip() == "":
            print("검색어를 입력해주세요!")
            continue

        # 검색
        print(f"\n🔍 '{keyword}' 관련 뉴스 검색 결과입니다...\n")
        search_news = crawler.search(keyword, display=5)
        print_news_list(search_news)

if __name__ == "__main__":
    main()