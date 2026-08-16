# AccountServer

Word Online과 GyeMong의 Auth, Key-Value Server 입니다. 

## 데이터베이스 마이그레이션

계정 서버는 공용 게임 DB와 분리된 자체 PostgreSQL 데이터베이스를 소유한다.
그 스키마의 소스는 이 저장소의 `src/main/resources/db/migration/` 이다.

- 스키마 변경은 `src/main/resources/db/migration/`에 버전이 붙은 Flyway 파일로
  추가한다. 클래스패스에 있으므로 jar 안에 함께 실린다.
- 파일명은 `V<version>_<YYYYMMDD>__<description>.sql` 규칙을 따른다.
  예: `V002_20260901__add_member_last_login.sql`
- 이미 적용된 마이그레이션은 절대 수정하지 않는다. 내용을 고치면 체크섬이 어긋나
  검증이 실패한다. 되돌려야 하면 앞으로 나아가는 마이그레이션을 새로 추가한다.
- 마이그레이션을 적용하는 주체는 애플리케이션이다. CI가 아니다.
  기동 시 `AccountFlywayConfig`가 Flyway를 실행한다. 접속 정보는 기존
  `spring.r2dbc.account.*` 설정에서 파생한다. R2DBC URL의 `r2dbc:` 스킴을
  `jdbc:`로 바꿔 JDBC URL을 만들기 때문에 새로 추가해야 하는 환경 변수는 없다.
  `spring.sql.init.mode`는 여전히 `never`다.

**마이그레이션이 실패하면 계정 서버는 기동하지 않는다.** CI 잡 하나가 빨갛게
되는 것으로 끝나던 실패가 이제는 배포 실패다. 스키마와 코드가 어긋난 채로
서비스가 뜨는 일은 없어지지만, 마이그레이션 하나가 곧 가용성 문제라는 뜻이기도
하다. 마이그레이션을 추가할 때는 그만큼 신중해야 한다.

`db/migration/V001_20260814__baseline.sql`은 이미 존재하는 스키마를 기술하는
베이스라인이다. Flyway는 `_`를 버전 구분자로 읽으므로 이 파일의 버전은 `1`이 아니라
`001.20260814`다. `baselineOnMigrate=true`와 `baselineVersion=001.20260814`로
실행하기 때문에 기존 데이터베이스에서는 적용된 것으로 기록되고 실행되지 않는다.
빈 데이터베이스에는 베이스라인을 잡을 대상이 없으므로 정상적으로 실행된다.

`archive/schema.sql`은 Flyway 도입 이전의 손으로 이어붙인 DDL 기록이다.
추적용으로만 남아 있고 실행되지 않는다.

공용 게임 DB의 마이그레이션 체인은 `database` 저장소가 소유한다. 그 체인을 계정
데이터베이스에 겨누면 안 된다.
