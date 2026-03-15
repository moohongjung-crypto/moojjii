#!/usr/bin/env python3
import os
import requests
import json
from datetime import datetime
from telegram import Bot
from telegram.error import TelegramError

# API 설정
BRAVE_API_KEY = os.getenv('BRAVE_API_KEY')
TELEGRAM_BOT_TOKEN = os.getenv('TELEGRAM_BOT_TOKEN')
TELEGRAM_CHAT_ID = os.getenv('TELEGRAM_CHAT_ID')

# 포트폴리오 회사 리스트
PORTFOLIO_COMPANIES = [
    "한국전력",
    "한화솔루션",
    "DL이앤씨",
    "LG전자",
    "두산에너빌리티",
    "대한항공",
    "HMM",
]

def search_news(company_name):
    """Brave Search API를 사용해 뉴스 검색"""
    url = "https://api.search.brave.com/res/v1/news/search"
    
    headers = {
        "Accept": "application/json",
        "X-Subscription-Token": BRAVE_API_KEY
    }
    
    params = {
        "q": f"{company_name} 주가 뉴스",
        "count": 3
    }
    
    try:
        response = requests.get(url, headers=headers, params=params)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"Error searching {company_name}: {e}")
        return None

def send_telegram_message(message):
    """Telegram으로 메시지 전송"""
    try:
        bot = Bot(token=TELEGRAM_BOT_TOKEN)
        bot.send_message(chat_id=TELEGRAM_CHAT_ID, text=message, parse_mode='HTML')
        print(f"Telegram message sent successfully")
    except TelegramError as e:
        print(f"Error sending Telegram message: {e}")

def save_to_obsidian(content, filename):
    """Obsidian 형식으로 파일 저장"""
    os.makedirs("news_archive", exist_ok=True)
    filepath = f"news_archive/{filename}"
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"Saved to {filepath}")

def format_report(news_data):
    """뉴스 데이터를 형식에 맞게 정렬"""
    report = f"# 📈 포트폴리오 일일 뉴스 리포트\n\n"
    report += f"**날짜**: {datetime.now().strftime('%Y년 %m월 %d일 %H:%M')}\n\n"
    report += f"---\n\n"
    
    for company, data in news_data.items():
        report += f"## {company}\n\n"
        
        if data.get('news'):
            for idx, news in enumerate(data['news'], 1):
                title = news.get('title', 'N/A')
                description = news.get('description', 'N/A')
                url = news.get('url', '#')
                
                report += f"### {idx}. {title}\n"
                report += f"📝 {description}\n"
                report += f"🔗 [기사 보기]({url})\n\n"
        else:
            report += "뉴스 없음\n\n"
        
        report += "---\n\n"
    
    return report

def main():
    print(f"🔍 포트폴리오 뉴스 검색 시작... ({datetime.now()})")
    
    news_data = {}
    
    # 각 회사의 뉴스 검색
    for company in PORTFOLIO_COMPANIES:
        print(f"Searching news for {company}...")
        result = search_news(company)
        
        if result and 'results' in result:
            news_data[company] = {
                'news': result['results'][:3]
            }
        else:
            news_data[company] = {'news': []}
    
    # 보고서 생성
    report = format_report(news_data)
    
    # Obsidian 저장
    filename = f"portfolio_news_{datetime.now().strftime('%Y%m%d')}.md"
    save_to_obsidian(report, filename)
    
    # Telegram 전송 (요약)
    summary = f"📊 오늘의 포트폴리오 뉴스 리포트\n\n"
    summary += f"검색 완료: {len(news_data)}개 회사\n\n"
    
    for company, data in news_data.items():
        if data['news']:
            summary += f"🔸 <b>{company}</b>: {len(data['news'])}개 뉴스\n"
        else:
            summary += f"⚪ <b>{company}</b>: 뉴스 없음\n"
    
    summary += f"\n📁 상세 내용은 Obsidian 저장소 확인\n"
    summary += f"📄 파일: {filename}"
    
    send_telegram_message(summary)
    
    print("✅ 작업 완료!")

if __name__ == "__main__":
    main()
