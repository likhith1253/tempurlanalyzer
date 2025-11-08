package com.sentinelai.guard.service;

import com.google.cloud.storage.*;
import com.google.firebase.cloud.StorageClient;
import com.sentinelai.guard.model.report.ReportData;
import com.sentinelai.guard.model.Report;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
// --- FIX: Added import for FontName ---
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    // --- FIX: Removed 'final' to allow @Value injection ---
    @Value("${firebase.storage.bucket-name:default-bucket}")
    private String bucketName;

    @Value("${app.report.base-url:https://storage.googleapis.com}")
    private String storageBaseUrl;
    
    public String getStorageBaseUrl() {
        return storageBaseUrl;
    }

    private final FirestoreService firestoreService;
    private final FirebaseService firebaseService;

    @Async
    public CompletableFuture<String> generateAndUploadReport(ReportData reportData) {
        try {
            // Generate PDF report
            byte[] pdfBytes = generatePdfReport(reportData);
            
            // Upload to Firebase Storage
            String filePath = uploadToStorage(pdfBytes, reportData);
            
            // Store report metadata in Firestore
            storeReportMetadata(reportData, filePath);
            
            return CompletableFuture.completedFuture(getPublicUrl(filePath));
        } catch (Exception e) {
            log.error("Error generating report", e);
            throw new RuntimeException("Failed to generate report", e);
        }
    }

    // --- FIX: This method had major logic and duplication errors ---
    private byte[] generatePdfReport(ReportData reportData) throws IOException {
        // Use try-with-resources for the document to ensure it's always closed
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Using Standard14Fonts.FontName enum for font selection
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            
            float margin = 50;
            float yPosition = 750; 
            float leading = 20f;
            
            // Add title
            contentStream.beginText();
            contentStream.setFont(titleFont, 18);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("SentinelAI Threat Analysis Report");
            contentStream.endText();
            yPosition -= leading * 1.5f;
            
            // Add report period
            contentStream.beginText();
            contentStream.setFont(headerFont, 12);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText(String.format("Period: %s to %s", 
                reportData.getStartDate(), 
                reportData.getEndDate()));
            contentStream.endText();
            yPosition -= leading * 1.5f;
            
            // Add summary section
            yPosition = addSectionHeader("Summary", contentStream, margin, yPosition, leading, headerFont);
            yPosition = addSummarySection(reportData, contentStream, margin, yPosition, leading, normalFont);
            
            // Add charts (with pagination check)
            if (yPosition < 300) { // Check if space for charts
                contentStream.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page);
                yPosition = 750;
            }
            
            yPosition = addSectionHeader("Threat Distribution", contentStream, margin, yPosition, leading, headerFont);
            yPosition = addCharts(reportData, document, contentStream, margin, yPosition, leading);
            
            // Add top threats section (with pagination check)
            if (yPosition < 200) { // Check if space for first threat
                contentStream.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page);
                yPosition = 750;
            }
            
            yPosition = addSectionHeader("Top Threats", contentStream, margin, yPosition, leading, headerFont);
            // --- FIX: addTopThreats now returns the new stream to handle pagination ---
            contentStream = addTopThreats(reportData, document, contentStream, margin, yPosition, leading, normalFont);
            
            // Close the final stream
            contentStream.close();
            
            // Save to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
            
        } // 'document' is closed automatically by try-with-resources
    }
    
    private float addSectionHeader(String title, PDPageContentStream contentStream, 
                                   float margin, float yPosition, float leading, 
                                   PDType1Font font) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, 14);
        contentStream.setNonStrokingColor(new Color(59, 130, 246)); // Blue color
        contentStream.newLineAtOffset(margin, yPosition);
        contentStream.showText(title);
        contentStream.endText();
        
        // Add underline
        contentStream.setStrokingColor(new Color(59, 130, 246));
        contentStream.setLineWidth(1f);
        contentStream.moveTo(margin, yPosition - 5);
        contentStream.lineTo(margin + 200, yPosition - 5);
        contentStream.stroke();
        
        return yPosition - leading * 1.5f;
    }
    
    private float addSummarySection(ReportData reportData, PDPageContentStream contentStream, 
                                    float margin, float yPosition, float leading, 
                                    PDType1Font font) throws IOException {
        ReportData.ReportSummary summary = reportData.getSummary();
        
        addTextLine(contentStream, String.format("Total URLs Analyzed: %,d", summary.getTotalUrlsAnalyzed()), 
                    margin, yPosition, font, 12);
        yPosition -= leading;
        
        addTextLine(contentStream, String.format("Allowed: %,d (%.1f%%)", 
                    summary.getAllowedCount(), 
                    (summary.getTotalUrlsAnalyzed() > 0) ? 
                        (summary.getAllowedCount() * 100.0 / summary.getTotalUrlsAnalyzed()) : 0), 
                    margin + 20, yPosition, font, 12);
        yPosition -= leading;
        
        addTextLine(contentStream, String.format("Warned: %,d (%.1f%%)", 
                    summary.getWarnedCount(), 
                    (summary.getTotalUrlsAnalyzed() > 0) ? 
                        (summary.getWarnedCount() * 100.0 / summary.getTotalUrlsAnalyzed()) : 0), 
                    margin + 20, yPosition, font, 12);
        yPosition -= leading;
        
        addTextLine(contentStream, String.format("Blocked: %,d (%.1f%%)", 
                    summary.getBlockedCount(), 
                    (summary.getTotalUrlsAnalyzed() > 0) ? 
                        (summary.getBlockedCount() * 100.0 / summary.getTotalUrlsAnalyzed()) : 0), 
                    margin + 20, yPosition, font, 12);
        yPosition -= leading * 1.5f;
        
        addTextLine(contentStream, String.format("Unique Domains: %,d", summary.getUniqueDomains()), 
                    margin, yPosition, font, 12);
        
        return yPosition - leading * 2;
    }
    
    private float addCharts(ReportData reportData, PDDocument document, PDPageContentStream contentStream, 
                            float margin, float yPosition, float leading) throws IOException {
        try {
            // Generate decision distribution chart
            // --- FIX: Removed <String> generic type ---
            DefaultPieDataset decisionDataset = new DefaultPieDataset();
            reportData.getDecisionStats().forEach((decision, count) -> 
                decisionDataset.setValue(decision, count));
                
            JFreeChart decisionChart = ChartFactory.createPieChart(
                "Decision Distribution", 
                decisionDataset, 
                true, true, false);
                
            // Generate domain distribution chart (top 5)
            // --- FIX: Removed <String> generic type ---
            DefaultPieDataset domainDataset = new DefaultPieDataset();
            reportData.getDomainStats().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> domainDataset.setValue(entry.getKey(), entry.getValue()));
                
            JFreeChart domainChart = ChartFactory.createPieChart(
                "Top 5 Domains", 
                domainDataset, 
                true, true, false);
                
            // Convert charts to images and add to PDF
            ByteArrayOutputStream chartOs = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(chartOs, decisionChart, 400, 300);
            
            contentStream.drawImage(
                PDImageXObject.createFromByteArray(
                    document, 
                    chartOs.toByteArray(), 
                    "decision_chart"),
                margin, yPosition - 200, 250, 200);
                
            chartOs = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(chartOs, domainChart, 400, 300);
            
            contentStream.drawImage(
                PDImageXObject.createFromByteArray(
                    document, 
                    chartOs.toByteArray(), 
                    "domain_chart"),
                margin + 260, yPosition - 200, 250, 200);
                
            return yPosition - 250;
        } catch (Exception e) {
            log.warn("Failed to generate charts: {}", e.getMessage());
            return yPosition - leading; // Return current position if charts fail
        }
    }
    
    // --- FIX: Changed return type to PDPageContentStream to handle pagination ---
    private PDPageContentStream addTopThreats(ReportData reportData, PDDocument document, PDPageContentStream contentStream, 
                                        float margin, float yPosition, float leading, 
                                        PDType1Font font) throws IOException {
        int count = 1;
        // Using PDFBox 2.0+ font initialization
        PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        for (ReportData.ThreatAnalysis threat : reportData.getTopThreats()) {
            if (yPosition < 150) {
                contentStream.close();
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page);
                yPosition = 750;
            }
            
            // Threat header
            contentStream.setNonStrokingColor(Color.DARK_GRAY);
            addTextLine(contentStream, 
                        String.format("%d. %s - %s", count++, threat.getThreatType(), threat.getDomain()), 
                        margin, yPosition, boldFont, 12);
            yPosition -= leading;
            
            // URL
            contentStream.setNonStrokingColor(Color.BLACK);
            addTextLine(contentStream, "URL: " + threat.getUrl(), 
                        margin + 20, yPosition, font, 10);
            yPosition -= leading * 0.8f;
            
            // Risk level with color coding
            contentStream.setNonStrokingColor(
                threat.getRiskLevel().equals("High") ? Color.RED : 
                threat.getRiskLevel().equals("Medium") ? Color.ORANGE : 
                Color.GREEN);
                
            addTextLine(contentStream, "Risk: " + threat.getRiskLevel(), 
                        margin + 20, yPosition, font, 10);
            yPosition -= leading * 0.8f;
            
            // Description
            contentStream.setNonStrokingColor(Color.DARK_GRAY);
            // --- FIX: Pass yPosition to addWrappedText ---
            yPosition = addWrappedText(contentStream, "Description: " + threat.getDescription(), 
                                     margin + 20, yPosition, 500, font, 10, leading * 0.8f);
            
            // AI Explanation
            contentStream.setNonStrokingColor(new Color(75, 0, 130)); // Indigo
            // --- FIX: Pass yPosition to addWrappedText ---
            yPosition = addWrappedText(contentStream, "AI Analysis: " + threat.getAiExplanation(), 
                                     margin + 20, yPosition, 500, font, 10, leading * 0.8f);
            
            // Separator
            contentStream.setStrokingColor(Color.LIGHT_GRAY);
            contentStream.setLineWidth(0.5f);
            contentStream.moveTo(margin, yPosition + 5);
            contentStream.lineTo(margin + 500, yPosition + 5);
            contentStream.stroke();
            
            yPosition -= leading;
        }
        
        return contentStream; // --- FIX: Return the (potentially new) content stream ---
    }
    
    private void addTextLine(PDPageContentStream contentStream, String text, 
                             float x, float y, PDType1Font font, float fontSize) 
                             throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }
    
    // --- FIX: Modified to return the new Y position ---
    private float addWrappedText(PDPageContentStream contentStream, String text, 
                                 float x, float y, float maxWidth, 
                                 PDType1Font font, float fontSize, float lineLeading) throws IOException {
        
        float currentY = y;
        java.util.List<String> lines = new java.util.ArrayList<>();
        
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        
        for (String word : words) {
            String testLine = line + (line.length() > 0 ? " " : "") + word;
            float width = 0;
            try {
                width = font.getStringWidth(testLine) / 1000 * fontSize;
            } catch (IOException e) {
                // Handle exception
            }
            
            if (width > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        for (String l : lines) {
            contentStream.newLineAtOffset(x, currentY);
            contentStream.showText(l);
            currentY -= lineLeading;
        }
        contentStream.endText();
        
        return currentY; // Return the new Y position
    }
    
    // --- FIX: This helper method was flawed, removed it and replaced with inline logic in addWrappedText ---
    /*
    private int countLines(String text, int maxLineLength) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (text.length() + maxLineLength - 1) / maxLineLength;
    }
    */
    
    private String uploadToStorage(byte[] content, ReportData reportData) {
        try {
            Storage storage = StorageClient.getInstance().bucket(bucketName).getStorage();
            
            String fileName = String.format("reports/%s/report_%s_%s.pdf",
                reportData.getReportType().name().toLowerCase(),
                reportData.getStartDate().format(DateTimeFormatter.ISO_DATE),
                reportData.getReportId());
                
            BlobId blobId = BlobId.of(bucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("application/pdf")
                .build();
                
            storage.create(blobInfo, content);
            
            log.info("Report uploaded to: {}", fileName);
            return fileName;
        } catch (Exception e) {
            log.error("Failed to upload report to storage", e);
            throw new RuntimeException("Failed to upload report to storage", e);
        }
    }
    
    private void storeReportMetadata(ReportData reportData, String filePath) {
        try {
            // Create a new Report object with the metadata
            Report report = Report.builder()
                .reportId(reportData.getReportId())
                .type(reportData.getReportType().name())
                .startDate(reportData.getStartDate().toString())
                .endDate(reportData.getEndDate().toString())
                .generatedAt(LocalDate.now().toString())
                .generatedBy(reportData.getGeneratedBy())
                .filePath(filePath)
                .downloadUrl(getPublicUrl(filePath))
                .summary(Map.of(
                    "totalUrls", reportData.getSummary().getTotalUrlsAnalyzed(),
                    "allowed", reportData.getSummary().getAllowedCount(),
                    "warned", reportData.getSummary().getWarnedCount(),
                    "blocked", reportData.getSummary().getBlockedCount()
                ))
                .build();
                
            // Save the report using the FirestoreService
            firestoreService.saveReport(report);
        } catch (Exception e) {
            log.error("Failed to store report metadata", e);
            // Don't fail the whole operation if metadata storage fails
        }
    }
    
    private String getPublicUrl(String filePath) {
        return String.format("%s/%s/%s", 
            storageBaseUrl, 
            bucketName, 
            filePath.replace(" ", "%20"));
    }
}