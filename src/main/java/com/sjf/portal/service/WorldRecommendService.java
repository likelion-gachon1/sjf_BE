package com.sjf.portal.service;

import com.sjf.portal.dto.WorldRecommendRequest;
import com.sjf.portal.dto.WorldRecommendResponse;
import org.springframework.stereotype.Service;

@Service
public class WorldRecommendService {

    public WorldRecommendResponse recommend(
            WorldRecommendRequest request
    ) {
        String mood = request.mood().toUpperCase();
        String travelStyle = request.travelStyle().toUpperCase();

        if (mood.equals("CALM") && travelStyle.equals("CULTURE")) {
            return new WorldRecommendResponse(
                    1L,
                    "NEW_YORK",
                    "Sunset Gallery Street",
                    "해 질 무렵 뉴욕의 갤러리 거리",
                    "/worlds/new-york-gallery.jpg",
                    "차분한 분위기와 문화 중심의 여행 스타일을 반영했습니다."
            );
        }

        if (mood.equals("BOLD") && travelStyle.equals("EXPLORE")) {
            return new WorldRecommendResponse(
                    2L,
                    "SHANGHAI",
                    "Neon City Street",
                    "화려한 조명으로 가득한 상하이 도심",
                    "/worlds/shanghai-neon.jpg",
                    "강렬한 분위기와 도시 탐험 스타일을 반영했습니다."
            );
        }

        if (mood.equals("LIGHT") && travelStyle.equals("RELAX")) {
            return new WorldRecommendResponse(
                    3L,
                    "PARIS",
                    "Morning Garden",
                    "따뜻한 햇살이 비치는 파리의 정원",
                    "/worlds/paris-garden.jpg",
                    "밝은 분위기와 여유로운 여행 스타일을 반영했습니다."
            );
        }

        return new WorldRecommendResponse(
                4L,
                "PARIS",
                "MCM City Journey",
                "MCM과 함께 떠나는 파리 도심 여행",
                "/worlds/paris-city.jpg",
                "선택한 제품과 여행 취향을 종합하여 추천했습니다."
        );
    }
}
