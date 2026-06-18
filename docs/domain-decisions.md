# 도메인 설계 결정 사항

## MVP용 ERD 개선 방향

- `member`는 서비스의 기준 사용자 테이블이며, 농업 프로필, 농장, 업로드 이미지, 분석 결과, 리포트의 소유 기준으로 사용한다.
- `farm_profile.main_crop_id`는 문자열이 아니라 `crop.id`를 참조한다. 회원 맞춤 정책, 교육, 보조금 추천에서 대표 작물을 안정적으로 재사용하기 위함이다.
- `farms.crop_summary`는 제거한다. 농장별 실제 재배 작물은 `farm_crops`가 담당하고, 농장 메모는 `farms.notes`에만 남긴다.
- `user_crop`은 회원의 관심 작물, 즐겨찾기 작물, 기본 선택 작물에 가깝게 유지한다. 특정 농장에서 실제 재배 중인 작물은 `farm_crops`로 관리한다.
- `uploaded_images.crop_id`는 병충해 분석 모델 선택 기준이므로 필수로 본다. `farm_id`는 농장 등록 없이도 이미지 업로드가 가능하도록 선택값으로 유지한다.

## RAG 문서 구조

- `document_assets`는 업로드된 원본 문서 파일의 메타데이터를 저장한다.
- `document_chunks`는 RAG 검색 단위인 chunk 메타데이터를 저장하며, `document_assets`와 1:N 관계를 가진다.
- `document_chunks.embedding`은 pgvector 기반 벡터 저장을 위한 컬럼이다. 현재 MVP 코드는 Spring AI `VectorStore`를 통해 실제 벡터 검색 흐름을 검증하고, chunk 메타데이터는 별도 테이블에 저장한다.

## 외부 연동 로그

- `external_api_log.member_id`와 `external_api_log.analysis_report_id`는 nullable FK로 둔다. 회원 맞춤 정책/시장 정보 호출과 리포트 생성 과정의 추적성을 확보하기 위함이다.
- `tool_call_log`는 MCP Client와 LLM Tool Calling 흐름을 보여주기 위한 선택 로그 테이블이다. 실제 도구 호출 기능이 커질 때 request/response payload와 성공 여부를 저장한다.

## 이미지 분석 결과

- `uploaded_images`는 업로드된 이미지의 메타데이터, 소유 회원, 선택된 작물, 분석 상태를 저장한다.
- `image_analysis_results`는 작물 이미지 병충해 예측 결과를 저장하는 기준 테이블이다.
- 현재 이미지 분석 API에서는 기존의 `disease_analysis` 테이블을 다시 사용하지 않는다.
- 이미지 분석 데이터는 이미지 예측 결과에만 집중한다. 주요 항목은 병명, 신뢰도, 심각도, 요약, 추천 조치, 원본 응답이다.

## 향후 정책 및 지원 추천

- 농업 정책, 보조금, 교육, 청년농 지원 추천은 별도의 도메인으로 분리해서 모델링한다.
- 정책 또는 지원 추천 기록을 `image_analysis_results`에 섞어 저장하지 않는다.
- 향후 추가를 고려할 수 있는 테이블로는 `support_policies`, `member_support_recommendations`, `external_support_api_logs` 등이 있다.
- 이러한 추천 기능은 `member`, `farm_profile`, `user_crop` 데이터를 개인화 입력값으로 활용할 수 있다.

## FastAPI 병충해 예측기

- 현재 MVP는 포도 병충해 분석만 지원한다.
- Spring Boot는 FastAPI를 호출하기 전에 사용자가 선택한 작물을 먼저 확인한다.
- FastAPI는 현재 포도 전용 더미 예측기 구조를 유지하며, 이후 실제 포도 병충해 모델로 교체할 수 있도록 설계한다.
