사용자 맞춤형 생활 정보 대시보드

JavaScript를 중심으로 뉴스, 날씨, 증권, 일정 등 여러 생활 정보를 한 화면에서 확인하고, 사용자가 원하는 방식으로 위젯을 구성할 수 있도록 만든 개인화 대시보드입니다.

5인 팀 프로젝트로 진행했으며, 프런트엔드와 백엔드 기능을 Spring Boot 애플리케이션 안에서 통합했습니다. 단순한 정보 조회를 넘어 회원 인증, 일정 관리, 실시간 상담, 게시판 및 관리자 기능까지 하나의 서비스 흐름으로 구현했습니다.

프로젝트 정보
항목	내용
개발 형태	5인 팀 프로젝트
개발 기간	2026.07.06 ~ 2026.07.31
담당 역할	팀장 · 프런트엔드 · 기능 통합
주요 담당	메인 대시보드, 위젯 시스템, JavaScript 모듈화, CSS, 외부 API 연동, Git 병합 관리

프로젝트 목표

- 수업에서 학습한 JavaScript를 실제 서비스 형태로 활용
- 여러 사이트에 흩어진 생활 정보를 하나의 화면으로 통합
- 사용자가 원하는 위젯을 선택하고 자유롭게 구성할 수 있는 대시보드 제공
- 로그인 이후 일정, 위젯 상태 등 개인화된 정보 관리
- 게시판, 실시간 상담, 관리자 기능을 연결하여 실제 서비스 운영 흐름 경험

주요 기능

위젯 대시보드

- 사용자가 필요한 위젯을 선택하여 대시보드 구성
- 드래그 기반 위젯 순서 변경
- 설정 모드와 일반 이용 모드 분리
- 전체 화면을 다시 만들지 않고 변경된 위젯만 갱신
- 위젯별 독립 상태 유지
- 반응형 레이아웃과 라이트·다크 모드

생활 정보 위젯

- 현재 날씨와 주간 예보
- 카테고리별 뉴스 조회
- 증권 종목 및 지수 정보와 차트
- 현재 시간과 일출·일몰 정보
- 도시별 세계시간
- 계산기, 메모 및 일정 위젯

회원과 개인화

- 회원가입과 로그인
- 이메일 인증과 비밀번호 재설정
- Kakao·Naver OAuth2 소셜 로그인
- 소셜 로그인 중복 가입 방지 및 연동 해제 처리
- 회원 정보 조회·수정과 회원 탈퇴
- 사용자별 일정과 설정 정보 저장

캘린더

- 일정 등록·조회·수정·삭제
- 로그인 사용자별 일정 관리
- Google Calendar 연동
- 캘린더 화면의 라이트·다크 모드

게시판과 문의

- 공지사항 및 문의 게시판
- 회원·비회원 작성 정책 분리
- 게시글과 답변 관리
- 제목·작성자 검색과 페이징
- 이미지 업로드 및 조회
- 비회원 문의글 정리 정책

실시간 상담

- WebSocket 기반 1:1 상담
- 채팅방과 메시지 이력 관리
- 읽음 상태와 미확인 메시지 처리
- 비회원 접근 제한과 인증 흐름 분리
- 상담 및 문의 발생 시 Kakao 알림 연동

관리자

- 회원 조회, 상태 변경 및 계정 관리
- 게시글·문의 및 상담방 관리
- 방문 기록과 일별 통계
- 외부 서비스 상태 확인
- 관리자 전용 접근 제어

담당 업무

팀장 및 통합

- 프로젝트 주제와 기능 범위 조율
- 팀원별 담당 영역 분배와 진행 상황 점검
- 기능별 브랜치 병합 및 충돌 대응
- 메인 화면과 개별 기능의 통합
- 회의록, 발표 자료 및 최종 프로젝트 정리

메인 대시보드 및 프런트엔드

- 위젯형 메인 페이지 구조 설계 및 구현
- 위젯 추가·이동·설정 인터랙션 구현
- 화면 전체 렌더링 방식을 위젯 단위 부분 렌더링 구조로 개선
- JavaScript 파일을 상태, 이벤트, 렌더러, 위젯 단위로 분리
- 공통 CSS와 위젯별 CSS 구조 정리
- 뉴스, 증권, 세계시간 등 위젯 구현 및 통합

외부 API 연동

- 뉴스 조회 백엔드와 프런트엔드 연결
- 증권 데이터 조회 및 차트 위젯 구성
- 날씨와 시간 관련 데이터의 대시보드 통합
- API 응답의 누락·중복·형식 차이에 대한 필터링과 예외 처리

문제 해결 경험

전체 렌더링 시 위젯 상태 초기화

위젯의 위치나 설정을 변경할 때 대시보드 전체를 다시 렌더링하면서 개별 위젯의 상태가 초기화되는 문제가 있었습니다.

위젯마다 독립적인 렌더링 함수를 두고 변경된 영역만 갱신하도록 구조를 분리했습니다. 이를 통해 다른 위젯의 입력값과 화면 상태를 유지하면서 필요한 부분만 업데이트할 수 있게 했습니다.

외부 API마다 다른 데이터 형식

뉴스, 날씨, 증권 API의 응답 구조와 데이터 품질이 서로 달랐고 일부 응답에는 누락이나 중복 데이터가 포함되었습니다.

서비스 계층에서 필요한 데이터만 정제하고, 프런트엔드에서는 위젯에 필요한 형태로 다시 가공했습니다. 오류가 발생해도 전체 대시보드가 중단되지 않도록 위젯 단위의 예외 처리와 대체 응답을 적용했습니다.

소셜 로그인 가입 시점과 중복 가입

OAuth 인증 직후 회원 정보를 바로 저장하면 약관 동의 이전에 계정이 생성되거나 동일 사용자가 중복 가입될 가능성이 있었습니다.

소셜 인증 결과를 임시 가입 상태로 보관하고, 약관 동의가 완료된 시점에 정식 회원으로 저장하도록 흐름을 분리했습니다. 가입 취소 시에는 Kakao 연결 해제 처리를 추가했습니다.

비회원 상담 접근 오류

인증 정보가 없는 사용자가 1:1 상담 화면에 접근할 때 채팅방과 사용자 정보를 생성하는 과정에서 오류가 발생했습니다.

상담 기능의 접근 조건을 명확히 하고, 인증된 사용자만 채팅방을 생성할 수 있도록 정책과 화면 흐름을 정리했습니다.

기술 스택

Frontend

- HTML5
- CSS3
- Vanilla JavaScript
- Thymeleaf
- Swiper
- Chart.js

Backend

- Java 25
- Spring Boot 4.0.7
- Spring Web MVC
- Spring Security
- OAuth2 Client
- Spring WebSocket
- Spring Mail
- Spring Data Redis
- MyBatis

Database and External Services

- Oracle Database
- Redis
- Google Calendar API
- Kakao·Naver OAuth2
- Kakao Map 및 알림 API
- 뉴스·날씨·증권 공공 API

Collaboration

- Git
- GitHub
- 기능별 브랜치 기반 협업
- 회의록을 이용한 역할 및 진행 상황 관리

시스템 구성

```text
Browser
├─ Thymeleaf / HTML / CSS / JavaScript
├─ Widget state and partial rendering
└─ WebSocket client
│
▼
Spring Boot
├─ Spring MVC controllers
├─ Spring Security / OAuth2
├─ Service layer
├─ WebSocket chat
└─ MyBatis mappers
│          │
▼          ▼
Oracle DB    Redis
│
└─ External APIs
News · Weather · Stock · Calendar · Kakao
```

프로젝트 구조

```text
src/main/java/com/siyan1234/itproject2nd/
├─ admin/       # 관리자 대시보드와 회원·게시판·상담 관리
├─ board/       # 공지사항과 문의 게시판
├─ calendar/    # 일정과 Google Calendar 연동
├─ chat/        # WebSocket 상담과 Kakao 알림
├─ member/      # 회원가입, 로그인, OAuth2 인증
├─ mypage/      # 회원 정보와 활동 내역
├─ News/        # 뉴스 API
├─ stock/       # 증권 API
├─ weather/     # 날씨 API
├─ time/        # 현재 시간과 일출·일몰
├─ map/         # Kakao 지도
└─ cookie/      # 쿠키 동의와 방문 기록

src/main/resources/
├─ mapper/      # MyBatis SQL Mapper
├─ static/      # JavaScript, CSS, 이미지
├─ templates/   # Thymeleaf 화면
└─ application-secret.yaml.example

database/
├─ 01_schema_latest.sql
├─ 02_seed_data.sql
└─ migrations/
```

로컬 실행

필요 환경

- Java 25
- Oracle Database
- Redis

저장소 복제

git clone https://github.com/neukdol30-ai/2026-school-project-2nd.git
cd 2026-school-project-2nd

데이터베이스 준비

Oracle에서 다음 파일을 순서대로 확인합니다.

1. database/01_schema_latest.sql
2. 필요한 경우 database/02_seed_data.sql

database/00_reset_local.sql은 기존 로컬 데이터를 제거할 수 있으므로 초기화가 필요한 경우에만 사용합니다.

환경변수 설정

공개 저장소에는 실제 인증 정보와 API 키를 포함하지 않습니다. 다음 환경변수를 실행 환경에 설정합니다.
환경변수	용도
PROJECT_DB_URL	Oracle JDBC URL
PROJECT_DB_USERNAME	Oracle 사용자명
PROJECT_DB_PASSWORD	Oracle 비밀번호
KAKAO_CLIENT_ID	Kakao OAuth2 Client ID
KAKAO_CLIENT_SECRET	Kakao OAuth2 Client Secret
NAVER_OAUTH_CLIENT_ID	Naver OAuth2 Client ID
NAVER_OAUTH_CLIENT_SECRET	Naver OAuth2 Client Secret
NAVER_NEWS_CLIENT_ID	Naver News API Client ID
NAVER_NEWS_CLIENT_SECRET	Naver News API Client Secret
DATA_GO_SERVICE_KEY	공공데이터 API 서비스 키

Kakao 알림과 지도 기능을 사용하려면 application.yaml에 기재된 추가 환경변수도 설정해야 합니다.

애플리케이션 실행

bash gradlew bootRun

실행 후 다음 주소로 접속합니다.

http://localhost:8080

현재 제한사항

- 외부 API와 OAuth2 기능을 사용하려면 각 서비스의 인증 정보가 필요합니다.
- Oracle과 Redis를 별도로 준비해야 하므로 즉시 실행 가능한 배포 환경은 제공하지 않습니다.
- 자동화된 테스트와 배포 파이프라인보다 기능 구현과 팀 통합에 중점을 둔 학원 팀 프로젝트입니다.
- 일부 기능은 외부 API의 응답 상태와 호출 제한에 영향을 받을 수 있습니다.

배운 점

- 기능 구현 전에 데이터 흐름과 상태 변화 범위를 먼저 정의해야 한다는 점
- 화면 전체를 다시 그리는 방식보다 변경 범위를 제한한 렌더링이 상태 유지에 유리하다는 점
- 분업뿐 아니라 브랜치 병합, 공통 규칙 및 인수인계가 프로젝트 완성도에 직접 영향을 준다는 점
- 외부 API는 성공 응답뿐 아니라 누락, 중복, 형식 차이와 장애 상황까지 고려해야 한다는 점
- 인증과 회원 저장 시점은 기능 편의보다 데이터 일관성과 사용자 동의를 우선해야 한다는 점

Repository

- GitHub: https://github.com/neukdol30-ai/2026-school-project-2nd
