# 📝 Blog RAG — 내 네이버 블로그 기반 개인 AI 비서

> 내가 직접 쓴 블로그 글을 AI가 학습하여,  
> 자연어 질문으로 내 기록을 빠르게 검색하고 답변받는 개인 AI 비서 시스템

**개인 포트폴리오 프로젝트** | RAG 파이프라인 풀스택 직접 구현

---

## 📌 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 분류 | 개인 포트폴리오 |
| 역할 | 기획 · 백엔드 · 프론트엔드 전담 개발 |
| 목표 | RAG 파이프라인 직접 구현을 통한 AI 엔지니어링 역량 습득 |

---

## 🎯 기획 배경

블로그 포스팅이 취미인데, 글이 쌓일수록 **내 기록을 찾는 데 시간이 오래 걸리는 문제**가 생겼다.

> "작년에 제주도 갔을 때 뭐 먹었더라?"  
> "그 카페 이름이 뭐였지?"

이런 질문이 생길 때마다 블로그를 직접 뒤져야 했고, 포스팅이 많아질수록 점점 불편해졌다.

**→ 내 블로그 글을 AI가 학습하고, 자연어로 물어보면 바로 찾아주는 비서를 직접 만들기로 했다.**

---

## 💡 주요 기능

- 🔍 **블로그 자동 크롤링** — 내 네이버 블로그 포스팅을 자동으로 수집
- 🧠 **벡터 임베딩 저장** — 수집된 글을 AI가 이해할 수 있는 형태로 변환하여 DB 저장
- 💬 **자연어 질의응답** — 질문과 유사한 블로그 글을 검색하여 LLM이 답변 생성
- 📎 **출처 링크 제공** — 답변 근거가 된 블로그 포스팅 URL 함께 반환

---

## 🏗️ 시스템 아키텍처

```
[데이터 수집]
내 네이버 블로그
  → Jsoup 크롤링 (포스팅 목록 + 본문 추출)
  → 텍스트 전처리
         ↓
[임베딩 & 저장]
Ollama (nomic-embed-text)
  → 텍스트를 벡터로 변환
  → PostgreSQL + pgvector 저장
         ↓
[질의응답]
Vue 채팅 UI
  → Spring Boot REST API
  → 질문 벡터 변환
  → pgvector 유사도 검색 (상위 3개)
  → Ollama LLM (llama3.1:8b) 답변 생성
  → 답변 + 출처 URL 반환
```

---

## 🛠️ 기술 스택

### 백엔드
| 기술 | 용도 |
|------|------|
| Spring Boot 4.1 | 백엔드 프레임워크 |
| Spring AI | Ollama LLM · 임베딩 · pgvector 연동 |
| Spring Data JPA | DB ORM |
| PostgreSQL + pgvector | 벡터 유사도 검색 |
| Jsoup | 네이버 블로그 크롤링 |
| Ollama (llama3.1:8b) | 로컬 LLM 답변 생성 |
| Ollama (nomic-embed-text) | 텍스트 임베딩 |

### 프론트엔드
| 기술 | 용도 |
|------|------|
| Vue 3 | 프론트엔드 프레임워크 |
| TypeScript | 타입 안정성 |
| Axios | REST API 통신 |

---

## 📡 API 명세

### 크롤링
| Method | URL | 설명 |
|--------|-----|------|
| POST | /api/crawl | 블로그 포스팅 크롤링 및 벡터DB 저장 |

### 채팅
| Method | URL | 설명 |
|--------|-----|------|
| POST | /api/chat | 자연어 질문 → RAG → LLM 답변 반환 |

**채팅 요청/응답 예시**
```json
// Request
{
  "message": "내가 인계동에서 먹은게 있어?"
}

// Response
{
  "answer": "인계동에서 양왕꼬치와 온면을 드셨습니다.",
  "sources": [
    "https://blog.naver.com/rorngk22/224315868241"
  ]
}
```

---

## 🚀 로컬 실행

### 사전 준비
```bash
# Ollama 설치 및 모델 다운로드
ollama pull llama3.1:8b
ollama pull nomic-embed-text

# PostgreSQL + pgvector 설치 (Mac 기준)
brew install postgresql@17
brew install pgvector
brew services start postgresql@17

# DB 및 테이블 생성
psql postgres
CREATE DATABASE blog_rag;
\c blog_rag
CREATE EXTENSION vector;
CREATE TABLE vector_store (
    id UUID PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding vector(768)
);
```

### 백엔드 실행
```bash
cd blog-rag

# 환경변수 설정
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
export NAVER_CLIENT_ID=your_client_id
export NAVER_CLIENT_SECRET=your_client_secret
export NAVER_BLOG_ID=your_blog_id

./gradlew bootRun
```

### 프론트엔드 실행
```bash
cd blog-rag-front
yarn
yarn dev
```

### 블로그 크롤링 실행
```bash
curl -X POST "http://localhost:8080/api/crawl"
```

---

## 📊 기술적 의사결정

### 왜 Ollama를 선택했나?
- OpenAI, Claude API 대비 **완전 무료** — 비용 걱정 없이 무한 테스트 가능
- 로컬 실행으로 **외부 API 의존성 없음**
- 민감한 블로그 데이터의 **외부 유출 없음**
- "비용 효율적인 설계" 면접 어필 포인트

### 왜 pgvector를 선택했나?
- ChromaDB 등 별도 벡터DB 없이 **PostgreSQL 하나로 통합 관리**
- 인프라 복잡도 감소
- 기존 JPA 생태계와 자연스러운 연동

### 민감 정보 관리
- API 키, DB 계정 정보는 모두 **환경변수로 분리**
- 코드베이스에 민감 정보 미포함

---

## 💬 사용 예시

```
Q: "내가 인계동에서 먹은게 있어?"
A: 인계동에서 양왕꼬치와 온면을 드셨습니다.
   📎 출처: https://blog.naver.com/...

Q: "어디 카페 갔었어?"
A: 수원과 나트랑의 카페를 방문하셨네요.
   📎 출처: https://blog.naver.com/...
```
