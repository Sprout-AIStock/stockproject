# stockproject

## 환경 변수 / 프로퍼티

필수 키 (local은 `src/main/resources/application-local.properties` 참고):

- `fred.api.key` (FRED)
- `openai.api.key` (OpenAI)
- `api.naver.search.id`, `api.naver.search.secret` (Naver Search)

## 실행

```bash
./gradlew bootRun
```

## 주요 엔드포인트

- `POST /api/market/quad/stance`
- `POST /api/market/quad/banner`
- `GET  /api/market/quad/input/latest`
- `GET  /api/market/quad/stance/latest`
- `GET  /api/market/quad/banner/latest`
- `GET  /api/news/macro`
- `GET  /api/news/theme/{themeName}`

