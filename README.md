# AccountServer

Word Online과 GyeMong의 Auth, Key-Value Server 입니다. 

## 데이터베이스 마이그레이션

계정 서버는 공용 게임 DB와 분리된 자체 PostgreSQL 데이터베이스를 소유한다.
그 스키마의 소스는 이 저장소의 `migration/` 이다.

- 스키마 변경은 `migration/`에 버전이 붙은 Flyway 파일로 추가한다.
- 파일명은 `V<next>_<YYYYMMDD>__<description>.sql` 규칙을 따른다.
  예: `V002_20260901__add_member_last_login.sql`
- 이미 적용된 마이그레이션은 절대 수정하지 않는다. 되돌려야 하면 앞으로 나아가는
  마이그레이션을 새로 추가한다.
- 마이그레이션을 적용하는 주체는 CI다. 애플리케이션이 아니다.
  `deploy` 브랜치에 푸시되면 `.github/workflows/migration.yml`이 Flyway를 Docker
  컨테이너로 실행한다. `build.gradle`에 Flyway 의존성은 없고,
  `spring.sql.init.mode`는 `never`다.

`migration/V001_20260814__baseline.sql`은 이미 존재하는 스키마를 기술하는
베이스라인이다. 워크플로가 `-baselineOnMigrate=true -baselineVersion=1`로 실행하므로
기존 데이터베이스에서는 적용된 것으로 기록되고 실행되지 않는다.

`archive/schema.sql`은 Flyway 도입 이전의 손으로 이어붙인 DDL 기록이다.
추적용으로만 남아 있고 실행되지 않는다.

공용 게임 DB의 마이그레이션 체인은 `database` 저장소가 소유한다. 그 체인을 계정
데이터베이스에 겨누면 안 된다.
