package leapro.io.controllers;
import leapro.io.services.PDFUploader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "http://localhost:8081") // Adjust based on frontend URL
public class FileUploadController {

    private static final String UPLOAD_DIR = "uploads"; // Change if needed

    @Autowired
    VectorStore vectorStore;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        List<String> uploadedFiles = new ArrayList<>();

        // Ensure upload directory exists
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        for (MultipartFile file : files) {
            try {
                // Validate file type
                if (!isValidFileType(file)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Invalid file type: " + file.getOriginalFilename());
                }

                // Save file
                Path filePath = Path.of(UPLOAD_DIR, file.getOriginalFilename());
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // ### Send to Index
                PDFUploader uploader = new PDFUploader(vectorStore);

                // Provide the path to the PDF file
                uploader.uploadPdf(UPLOAD_DIR+"/"+file.getOriginalFilename());


                uploadedFiles.add(file.getOriginalFilename());
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error uploading file: " + file.getOriginalFilename());
            }
        }

        return ResponseEntity.ok("Uploaded successfully: " + String.join(", ", uploadedFiles));
    }

    private boolean isValidFileType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null &&
                (contentType.equals("application/pdf") ||
                        contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }
}