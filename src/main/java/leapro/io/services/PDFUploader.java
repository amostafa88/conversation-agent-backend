package leapro.io.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PDFUploader {

    private final VectorStore vectorStore;

    public PDFUploader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void uploadPdf(String pdfFilePath) {
        try {
            File file = new File(pdfFilePath);
            PDDocument document = PDDocument.load(file);
            PDFTextStripper pdfStripper = new PDFTextStripper();

            // Extract text from PDF
            String text = pdfStripper.getText(document);
            document.close();

            // Split text into smaller chunks (optional, recommended for better search)
            List<String> chunks = splitTextIntoChunks(text, 500); // Adjust chunk size

            // Convert chunks into Document objects
            List<Document> documents = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source", file.getName());  // Store filename as metadata
                metadata.put("chunk_id", i);
                documents.add(new Document(chunks.get(i), metadata));
            }

            // Index documents in vector store
            vectorStore.add(documents);
            System.out.println("PDF uploaded successfully!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> splitTextIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=\\.)"); // Split by sentence
        StringBuilder chunk = new StringBuilder();

        for (String sentence : sentences) {
            if (chunk.length() + sentence.length() > chunkSize) {
                chunks.add(chunk.toString());
                chunk.setLength(0); // Reset chunk
            }
            chunk.append(sentence);
        }
        if (chunk.length() > 0) {
            chunks.add(chunk.toString());
        }
        return chunks;
    }


}
