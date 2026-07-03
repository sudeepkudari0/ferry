package com.example.mobileagent.llm.local;

/**
 * Represents a downloadable local LLM model.
 * Contains metadata about the model including its download URL, 
 * file size, quantization info, and recommended RAM requirements.
 */
public class LocalModel {

    private final String id;
    private final String displayName;
    private final String description;
    private final String downloadUrl;
    private final String fileName;
    private final long fileSizeBytes;
    private final String quantization;
    private final int recommendedRamGb;
    private final String parameterCount;

    private LocalModel(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.downloadUrl = builder.downloadUrl;
        this.fileName = builder.fileName;
        this.fileSizeBytes = builder.fileSizeBytes;
        this.quantization = builder.quantization;
        this.recommendedRamGb = builder.recommendedRamGb;
        this.parameterCount = builder.parameterCount;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getFileName() { return fileName; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getQuantization() { return quantization; }
    public int getRecommendedRamGb() { return recommendedRamGb; }
    public String getParameterCount() { return parameterCount; }

    /** Returns human-readable file size string (e.g., "1.5 GB") */
    public String getFormattedSize() {
        if (fileSizeBytes < 1024) return fileSizeBytes + " B";
        if (fileSizeBytes < 1024 * 1024) return String.format("%.1f KB", fileSizeBytes / 1024.0);
        if (fileSizeBytes < 1024L * 1024 * 1024) return String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024));
        return String.format("%.1f GB", fileSizeBytes / (1024.0 * 1024 * 1024));
    }

    public static class Builder {
        private String id;
        private String displayName;
        private String description = "";
        private String downloadUrl;
        private String fileName;
        private long fileSizeBytes;
        private String quantization = "Q4_K_M";
        private int recommendedRamGb = 4;
        private String parameterCount;

        public Builder(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public Builder description(String v) { this.description = v; return this; }
        public Builder downloadUrl(String v) { this.downloadUrl = v; return this; }
        public Builder fileName(String v) { this.fileName = v; return this; }
        public Builder fileSizeBytes(long v) { this.fileSizeBytes = v; return this; }
        public Builder quantization(String v) { this.quantization = v; return this; }
        public Builder recommendedRamGb(int v) { this.recommendedRamGb = v; return this; }
        public Builder parameterCount(String v) { this.parameterCount = v; return this; }

        public LocalModel build() { return new LocalModel(this); }
    }
}
