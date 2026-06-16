# Domain Decisions

## Image Analysis Result

- `uploaded_images` stores the uploaded image metadata, owner member, selected crop, and analysis status.
- `image_analysis_results` is the canonical table for crop image disease prediction output.
- Do not reintroduce the legacy `disease_analysis` table for the active image analysis API.
- Image analysis data should stay focused on the image prediction result: disease name, confidence, severity, summary, recommendation, and raw response.

## Future Policy And Support Recommendations

- Agricultural policy, subsidy, education, and young-farmer support recommendations should be modeled as separate domains.
- Do not mix policy or support recommendation records into `image_analysis_results`.
- Recommended future tables may include `support_policies`, `member_support_recommendations`, and `external_support_api_logs`.
- These recommendations can use `member`, `farm_profile`, and `user_crop` data as personalization inputs.

## FastAPI Disease Predictor

- The current MVP only supports grape disease analysis.
- Spring Boot checks the selected crop before calling FastAPI.
- FastAPI keeps a grape-only dummy predictor structure so it can be replaced by a real grape model later.
