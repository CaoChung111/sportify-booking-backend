package com.sportify.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Data;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@ApplicationScoped
public class VectorStoreService {
    private static final Logger LOG = Logger.getLogger(VectorStoreService.class);

    @Inject
    GeminiClient geminiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<Document> documents = new ArrayList<>();
    private final List<float[]> embeddings = new ArrayList<>();
    private boolean isEmbeddingInitialized = false;

    @Data
    public static class Document {
        private String id;
        private String category;
        private String title;
        private String content;
    }

    public record SearchResult(Document document, double score) {}

    @PostConstruct
    public void init() {
        loadKnowledgeBase();
    }

    private static final String CACHE_FILE = "target/embeddings-cache.json";
 
    private void loadKnowledgeBase() {
        try (InputStream is = getClass().getResourceAsStream("/knowledge-base.json")) {
            if (is == null) {
                LOG.warn("knowledge-base.json not found in classpath. Skipping Vector Store initialization.");
                return;
            }
            List<Document> loadedDocs = objectMapper.readValue(is, new TypeReference<List<Document>>() {});
            
            Map<String, List<Double>> cache = loadCache();
            boolean hasNewEmbeddings = false;
 
            for (Document doc : loadedDocs) {
                documents.add(doc);
                float[] embedding = null;
                
                if (cache.containsKey(doc.getId())) {
                    List<Double> cachedValues = cache.get(doc.getId());
                    embedding = new float[cachedValues.size()];
                    for (int i = 0; i < cachedValues.size(); i++) {
                        embedding[i] = cachedValues.get(i).floatValue();
                    }
                } else {
                    try {
                        String textToEmbed = String.format("Title: %s\nContent: %s", doc.getTitle(), doc.getContent());
                        embedding = geminiClient.embedContent(textToEmbed);
                        
                        List<Double> doubleList = new ArrayList<>();
                        for (float v : embedding) {
                            doubleList.add((double) v);
                        }
                        cache.put(doc.getId(), doubleList);
                        hasNewEmbeddings = true;
                    } catch (Exception e) {
                        LOG.errorf("Failed to embed document: %s. Detail: %s", doc.getId(), e.getMessage());
                    }
                }
                embeddings.add(embedding);
            }
            
            if (hasNewEmbeddings) {
                saveCache(cache);
            }
            
            isEmbeddingInitialized = embeddings.stream().anyMatch(e -> e != null);
            LOG.infof("Loaded %d documents. Embeddings initialized: %s", documents.size(), isEmbeddingInitialized);
            
        } catch (Exception e) {
            LOG.error("Error loading knowledge base", e);
        }
    }
 
    private Map<String, List<Double>> loadCache() {
        File file = new File(CACHE_FILE);
        if (!file.exists()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(file, new TypeReference<Map<String, List<Double>>>() {});
        } catch (Exception e) {
            LOG.warn("Failed to read embeddings cache, will regenerate", e);
            return new HashMap<>();
        }
    }
 
    private void saveCache(Map<String, List<Double>> cache) {
        try {
            File file = new File(CACHE_FILE);
            file.getParentFile().mkdirs();
            objectMapper.writeValue(file, cache);
            LOG.info("Saved embeddings cache to " + CACHE_FILE);
        } catch (Exception e) {
            LOG.error("Failed to save embeddings cache", e);
        }
    }

    public List<SearchResult> search(String query, int topK) {
        if (!isEmbeddingInitialized) {
            return fallbackSearch(query, topK);
        }

        try {
            float[] queryEmbedding = geminiClient.embedContent(query);
            PriorityQueue<SearchResult> pq = new PriorityQueue<>(
                    (a, b) -> Double.compare(a.score(), b.score())
            );

            for (int i = 0; i < documents.size(); i++) {
                float[] docEmbedding = embeddings.get(i);
                if (docEmbedding != null) {
                    double similarity = cosineSimilarity(queryEmbedding, docEmbedding);
                    pq.offer(new SearchResult(documents.get(i), similarity));
                    if (pq.size() > topK) {
                        pq.poll();
                    }
                }
            }

            List<SearchResult> results = new ArrayList<>();
            while (!pq.isEmpty()) {
                results.add(0, pq.poll());
            }
            return results;
        } catch (Exception e) {
            LOG.error("Embedding search failed, falling back to keyword search", e);
            return fallbackSearch(query, topK);
        }
    }

    private List<SearchResult> fallbackSearch(String query, int topK) {
        PriorityQueue<SearchResult> pq = new PriorityQueue<>(
                (a, b) -> Double.compare(a.score(), b.score())
        );

        String lowerQuery = query.toLowerCase();
        for (Document doc : documents) {
            double score = 0.0;
            if (doc.getTitle() != null && doc.getTitle().toLowerCase().contains(lowerQuery)) {
                score += 0.5;
            }
            if (doc.getContent() != null && doc.getContent().toLowerCase().contains(lowerQuery)) {
                score += 0.5;
            }
            
            if (score > 0) {
                pq.offer(new SearchResult(doc, score));
                if (pq.size() > topK) {
                    pq.poll();
                }
            }
        }

        List<SearchResult> results = new ArrayList<>();
        while (!pq.isEmpty()) {
            results.add(0, pq.poll());
        }
        return results;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += Math.pow(a[i], 2);
            normB += Math.pow(b[i], 2);
        }
        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
