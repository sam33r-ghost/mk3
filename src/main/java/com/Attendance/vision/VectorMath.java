package com.Attendance.vision;

public class VectorMath {

    /**
     * Compares two embeddings. Returns a value between -1.0 and 1.0.
     * Closer to 1.0 means it is the exact same person.
     */
    public static double calculateCosineSimilarity(float[] liveEmbedding, float[] dbEmbedding) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < liveEmbedding.length; i++) {
            dotProduct += liveEmbedding[i] * dbEmbedding[i];
            normA += Math.pow(liveEmbedding[i], 2);
            normB += Math.pow(dbEmbedding[i], 2);
        }

        if (normA == 0 || normB == 0) return 0.0;

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}