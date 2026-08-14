# sjf_BE
현재 프론트에서 World 추천과 MediaPipe 합성을 처리하고 있으므로, 백엔드는 추천된 World와 사용자 선택값을 체험 세션으로 저장하고, 캡처된 최종 이미지를 업로드받아 모바일 결과 URL을 반환하는 방식으로 연동하겠습니다. 캡처 이후에는 이미지 업로드 성공 응답의 shareUrl을 QR에 인코딩하도록 수정이 필요합니다. 또한 API 값은 pink/black, light/calm/bold, explore/culture/relax로 맞추겠습니다. GIF가 필수인지, 정지 JPG로 진행할지는 추가 확인이 필요합니다.
