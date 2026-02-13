# 테스트 전략

## 테스트 프레임워크

| 도구 | 용도 |
|------|------|
| JUnit 5 | 테스트 프레임워크 |
| Spring Boot Test | 통합 테스트 |
| Mockito | 목(Mock) 객체 |
| AssertJ | 가독성 높은 단언(assertion) |
| H2 (또는 Testcontainers) | 테스트 DB |

## 테스트 계층

### 1. 단위 테스트 (Unit Test)
- **대상**: Service, Util, Domain 로직
- **특징**: 외부 의존성 모두 Mock 처리
- **위치**: 각 모듈의 `src/test/java/`
- **속도**: 빠름 (DB/네트워크 없음)

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 BusinessException 발생")
    void findUser_notFound_throwsException() {
        // given: 존재하지 않는 사용자 ID
        given(userRepository.findById("non-exist")).willReturn(Optional.empty());

        // when & then: BusinessException 발생 확인
        assertThatThrownBy(() -> userService.findById("non-exist"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
```

### 2. 통합 테스트 (Integration Test)
- **대상**: Repository, Security 설정, 전체 흐름
- **특징**: 실제 Spring Context 로딩
- **DB**: H2 인메모리 또는 Testcontainers (MySQL)

```java
@SpringBootTest
@Transactional
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("이메일로 사용자 조회 성공")
    void findByEmail_success() {
        // given: 테스트 사용자 저장
        // when: 이메일로 조회
        // then: 결과 검증
    }
}
```

### 3. API 테스트 (Controller Test)
- **대상**: Controller 엔드포인트
- **특징**: MockMvc로 HTTP 요청/응답 검증
- **검증**: 상태 코드, 응답 구조, 에러 처리

```java
@WebMvcTest(GroupController.class)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GroupService groupService;

    @Test
    @DisplayName("그룹 생성 API - 정상 응답 201")
    void createGroup_success() throws Exception {
        // given: 서비스 응답 Mock
        given(groupService.createGroup(any())).willReturn(groupResponse);

        // when & then: POST 요청 후 201 + ApiResponse 구조 검증
        mockMvc.perform(post("/groups")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("테스트 그룹"));
    }

    @Test
    @DisplayName("그룹 생성 API - 이름 누락 시 400")
    void createGroup_noName_returns400() throws Exception {
        // given: 이름 없는 요청
        // when & then: 400 + COM_002 에러 코드 검증
        mockMvc.perform(post("/groups")
                .contentType(APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("COM_002"));
    }
}
```

## 테스트 네이밍 규칙

### 클래스명
- `{대상클래스}Test` — 단위 테스트
- `{대상클래스}IntegrationTest` — 통합 테스트

### 메서드명
- `{메서드}_{시나리오}_{기대결과}` 패턴
- `@DisplayName`에 한국어로 테스트 의도 작성

```java
// 메서드명: 영어 snake_case
void findByEmail_notFound_throwsException()
void createGroup_duplicateName_returns409()
void login_lockedAccount_returns401()

// @DisplayName: 한국어
@DisplayName("이메일 미존재 시 BusinessException 발생")
@DisplayName("그룹명 중복 시 409 응답")
@DisplayName("잠긴 계정 로그인 시 401 응답")
```

## 테스트 구조 (Given-When-Then)

모든 테스트는 Given-When-Then 패턴으로 작성:

```java
@Test
@DisplayName("설명")
void methodName() {
    // given: 테스트 준비 (데이터, Mock 설정)

    // when: 테스트 대상 실행

    // then: 결과 검증
}
```

- 각 섹션은 주석으로 구분
- given이 복잡하면 `@BeforeEach`나 헬퍼 메서드로 분리

## 모듈별 테스트 범위

### core 모듈
- 도메인 서비스 단위 테스트 (비즈니스 로직)
- Repository 통합 테스트 (쿼리 검증)
- 공통 유틸리티 단위 테스트

### admin-api / user-api 모듈
- Controller API 테스트 (`@WebMvcTest`)
- Security 설정 테스트 (인증/인가 흐름)
- 전체 흐름 통합 테스트 (필요 시)

## 테스트 데이터

### 테스트 픽스처
- `@BeforeEach`에서 공통 테스트 데이터 설정
- 복잡한 데이터는 Builder 패턴 활용
- 테스트 간 데이터 독립성 보장 (`@Transactional` 롤백)

### 테스트 프로필
- `src/test/resources/application-test.properties`
- H2 인메모리 DB 또는 Testcontainers 설정
- 외부 서비스 (Redis 등) Mock 또는 Testcontainers

## 실행 방법

```bash
# 전체 테스트
cd backend && ./gradlew test

# 특정 모듈 테스트
./gradlew :core:test
./gradlew :admin-api:test
./gradlew :user-api:test

# 특정 클래스 테스트
./gradlew :core:test --tests "com.gizzi.core.domain.user.service.UserServiceTest"
```
