package com.yinyue.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yinyue.dto.ReferenceSearchRequest;
import com.yinyue.entity.CoverReference;
import com.yinyue.repository.CoverReferenceRepository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReferenceSearchService {

    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() { }.getType();

    private final CoverReferenceRepository coverReferenceRepository;
    private final Gson gson = new Gson();

    public ReferenceSearchService(CoverReferenceRepository coverReferenceRepository) {
        this.coverReferenceRepository = coverReferenceRepository;
    }

    public Map<String, Object> search(ReferenceSearchRequest request) {
        int topN = request.getTopN() == null ? 5 : Math.max(1, Math.min(request.getTopN(), 10));
        String targetGenre = readTopGenre(request.getSemanticProfile());
        List<String> targetMoods = readMoodLabels(request.getSemanticProfile());

        List<CoverReference> candidates = isBlank(targetGenre)
                ? coverReferenceRepository.findTop50ByEnabledTrueOrderByIdDesc()
                : coverReferenceRepository.findTop50ByEnabledTrueAndGenreIgnoreCaseOrderByIdDesc(targetGenre);

        if (candidates.isEmpty() && !isBlank(targetGenre)) {
            candidates = coverReferenceRepository.findTop50ByEnabledTrueOrderByIdDesc();
        }

        List<Map<String, Object>> references = candidates.stream()
                .map(item -> toReferenceItem(item, targetGenre, targetMoods))
                .sorted((a, b) -> Double.compare(readScore(b), readScore(a)))
                .limit(topN)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("querySummary", buildQuerySummary(targetGenre, targetMoods));
        response.put("references", references);
        response.put("styleConstraints", buildStyleConstraints(references, targetGenre, targetMoods));
        return response;
    }

    private Map<String, Object> toReferenceItem(CoverReference item, String targetGenre, List<String> targetMoods) {
        double genreScore = !isBlank(targetGenre) && targetGenre.equalsIgnoreCase(item.getGenre()) ? 0.2 : 0.05;
        List<String> moodTags = parseJsonList(item.getMoodTags());
        long moodMatchCount = moodTags.stream().filter(targetMoods::contains).count();
        double moodScore = Math.min(moodMatchCount * 0.08, 0.24);
        double baseScore = 0.55;
        double score = Math.min(baseScore + genreScore + moodScore, 0.98);

        Map<String, Object> result = new HashMap<>();
        result.put("id", item.getId());
        result.put("title", item.getTitle());
        result.put("artist", item.getArtist());
        result.put("score", round(score));
        result.put("genre", item.getGenre());
        result.put("moodTags", moodTags);
        result.put("styleTags", parseJsonList(item.getStyleTags()));
        result.put("colorPalette", parseJsonList(item.getColorPalette()));
        return result;
    }

    private Map<String, Object> buildQuerySummary(String targetGenre, List<String> targetMoods) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("targetGenre", isBlank(targetGenre) ? "any" : targetGenre);
        summary.put("targetMood", targetMoods.isEmpty() ? List.of("any") : targetMoods);
        summary.put("targetEra", "any");
        return summary;
    }

    private Map<String, Object> buildStyleConstraints(List<Map<String, Object>> references,
                                                      String targetGenre,
                                                      List<String> targetMoods) {
        Set<String> styleHints = new LinkedHashSet<>();
        Set<String> paletteHints = new LinkedHashSet<>();

        for (Map<String, Object> ref : references) {
            styleHints.addAll(toStringList(ref.get("styleTags")));
            paletteHints.addAll(toStringList(ref.get("colorPalette")));
        }

        String visualDirection = String.format("%s %s album cover", safeToken(targetGenre, "mainstream"),
                targetMoods.isEmpty() ? "balanced" : String.join("-", targetMoods));

        Map<String, Object> constraints = new HashMap<>();
        constraints.put("visualDirection", visualDirection);
        constraints.put("paletteHints", paletteHints.stream().limit(6).collect(Collectors.toList()));
        constraints.put("compositionHints", styleHints.stream().limit(6).collect(Collectors.toList()));
        constraints.put("textureHints", List.of("album print texture", "subtle grain"));
        constraints.put("forbiddenElements", List.of(
                "band logo reproduction",
                "album title reproduction",
                "iconic character copy"
        ));
        return constraints;
    }

    private String readTopGenre(Map<String, Object> semanticProfile) {
        if (semanticProfile == null) {
            return "";
        }
        Object genreObject = semanticProfile.get("genre");
        if (!(genreObject instanceof List<?> genres) || genres.isEmpty()) {
            return "";
        }
        Object first = genres.get(0);
        if (!(first instanceof Map<?, ?> map)) {
            return "";
        }
        Object label = map.get("label");
        return label instanceof String ? (String) label : "";
    }

    private List<String> readMoodLabels(Map<String, Object> semanticProfile) {
        if (semanticProfile == null) {
            return List.of();
        }
        Object moodObject = semanticProfile.get("mood");
        if (!(moodObject instanceof List<?> moodList)) {
            return List.of();
        }

        return moodList.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(item -> item.get("label"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList());
    }

    private List<String> parseJsonList(String json) {
        if (isBlank(json)) {
            return List.of();
        }
        try {
            List<String> list = gson.fromJson(json, STRING_LIST_TYPE);
            return list == null ? List.of() : list;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList());
    }

    private double readScore(Map<String, Object> item) {
        Object score = item.get("score");
        if (score instanceof Number number) {
            return number.doubleValue();
        }
        return 0.0;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private String safeToken(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
