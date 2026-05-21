# AI Mafia Game — Tech Stack PPT

기술 스택 + 아키텍처 중심 발표 자료. Reveal.js + Three.js + GSAP.

## 실행

별도 빌드 없이 정적 HTML. **로컬 서버**만 띄우면 됨 (ES module + CDN 때문에 `file://`로 열면 일부 기능 작동 안 함).

### Python 3
```bash
cd docs/ppt
python3 -m http.server 8080
# 브라우저: http://localhost:8080
```

### Node (npx)
```bash
cd docs/ppt
npx serve -p 8080
```

### VS Code Live Server
`docs/ppt/index.html` 우클릭 → "Open with Live Server"

## 조작

| 키 | 동작 |
|----|------|
| ← / → / Space | 슬라이드 이동 |
| ESC | 오버뷰 모드 (모든 슬라이드 한눈에) |
| F | 풀스크린 |
| S | speaker notes (이번 PPT는 비활성) |
| B | 배경 3D 인텐시티 토글 |
| ? | 단축키 도움말 |

## 슬라이드 구성 (13장)

1. Title (3D 인트로)
2. Overview
3. Architecture
4. Tech Stack
5. Java 21 핵심 (records / sealed interface)
6. JavaFX Reactive Binding
7. Maven Multi-Module
8. Network · DB
9. Event Sourcing
10. **Why CSP4SDG** — 알고리즘 비교 + 선택 이유
11. CSP in Action (도메인 축소 시각화)
12. Inference Visualization
13. Roadmap (Phase 2 / 3)

## 디자인 시스템

- **폰트**: Pretendard Variable (sans), Fraunces (serif accent), JetBrains Mono (code)
- **타입 스케일**: 1.25 ratio modular (12 / 15 / 17 / 22 / 40 / 72 / 104 px)
- **색상**: 다크 베이스 (#06060b) + 진영 팔레트 (mafia / citizen / police / doctor / psycho) + warm gold accent
- **토큰**: `assets/style.css` 상단 `:root` 블록에 의미 기반 토큰 정의

## 디렉토리

```
docs/ppt/
├── index.html         ⇢ 13개 슬라이드 마크업
├── assets/
│   ├── style.css      ⇢ 디자인 토큰 + 슬라이드 스타일
│   └── main.js        ⇢ Three.js 배경 + GSAP 진입 애니메이션 + Reveal init
└── README.md
```

## 의존성 (모두 CDN)

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| Reveal.js | 4.6.1 | 슬라이드 프레임워크 |
| Three.js  | 0.160.0 | 배경 3D 네트워크 |
| GSAP      | 3.12.5 | 슬라이드 진입 애니메이션 |
| Pretendard | 1.3.9 | 본문 한글/영문 sans |
| Fraunces  | latest | 강조 italic serif |
| JetBrains Mono | latest | 코드 폰트 |
