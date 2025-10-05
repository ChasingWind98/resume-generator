package de.thi.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.thi.resume.dto.ResumeData;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

@Path("/resume")
public class ResumeResource {

    private static final Logger LOG = Logger.getLogger(ResumeResource.class);

    // 1. Dependency Injection: Quarkus stellt uns automatisch eine Instanz unseres Services bereit.
    @Inject
    PdfGenerationService pdfService;

    // 2. Jackson's ObjectMapper, um den JSON-String in unser Java-Objekt umzuwandeln.
    @Inject
    ObjectMapper objectMapper;

    @POST
    @Path("/generate")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("application/pdf")
    // ÄNDERUNG 2: Die Methodensignatur wurde geändert.
    // Wir verwenden @RestForm für jeden Parameter anstelle von @MultipartForm für eine einzelne Klasse.
    public Response generatePdf(
            @RestForm("data") @PartType(MediaType.APPLICATION_JSON) String jsonData,
            @RestForm("photo") FileUpload photo
    ) {
        LOG.info("Received request to generate PDF.");

        java.nio.file.Path tempPhotoPath = null;
        try {
            // ÄNDERUNG 3: Wir greifen jetzt direkt auf den Parameter "jsonData" zu.
            ResumeData resumeData = objectMapper.readValue(jsonData, ResumeData.class);

            // Das Speichern des Fotos funktioniert genauso, nur mit dem direkten "photo" Parameter.
            String originalFileName = photo.fileName();
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            tempPhotoPath = Files.createTempFile("photo-" + UUID.randomUUID(), extension);
            Files.move(photo.uploadedFile(), tempPhotoPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            LOG.info("Uploaded photo saved to: " + tempPhotoPath);

            ResumeData finalData = new ResumeData(
                    resumeData.personalInfo(),
                    resumeData.skills(),
                    resumeData.educationEntries(),
                    resumeData.experienceEntries(),
                    resumeData.projectExperienceEntries(),
                    tempPhotoPath.toAbsolutePath().toString()
            );

            byte[] pdfBytes = pdfService.generatePdf(finalData);
            LOG.info("PDF generated successfully.");

            return Response.ok(pdfBytes)
                    .header("Content-Disposition", "attachment; filename=\"lebenslauf.pdf\"")
                    .build();

        } catch (Exception e) {
            LOG.error("Failed to generate PDF", e);

            return Response.serverError().entity("Failed to generate PDF: " + e.getMessage()).build();
        } finally {
            if (tempPhotoPath != null) {
                try {
                    Files.deleteIfExists(tempPhotoPath);
                    LOG.info("Cleaned up temporary photo: " + tempPhotoPath);
                } catch (IOException e) {
                    LOG.error("Failed to delete temporary photo: " + tempPhotoPath, e);
                }
            }
        }
    }


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }
}
