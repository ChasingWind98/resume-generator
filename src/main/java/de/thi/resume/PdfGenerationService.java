package de.thi.resume;

import de.thi.resume.dto.EducationEntry;
import de.thi.resume.dto.ExperienceEntry;
import de.thi.resume.dto.ProjectExperienceEntry;
import de.thi.resume.dto.ResumeData;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;


@ApplicationScoped
public class PdfGenerationService {
    private static final Logger LOG = Logger.getLogger(PdfGenerationService.class);
    /**
     * 生成pdf
     *
     * @param resumeData 总数据
     * @return byte数组
     */
    public byte[] generatePdf(ResumeData resumeData) throws IOException, InterruptedException {
        // 1. Lade das LaTeX-Template als String aus den Ressourcen
        String template = loadTemplate();
        // 2. Ersetze die einfachen Platzhalter
        template = replacePlaceholders(template, resumeData);
        // 3. Generiere die dynamischen Blöcke (Listen) und ersetze deren Platzhalter
        template = generateListBlock(template, resumeData);
        // 4. Temporäre Dateien erstellen und LaTeX-Prozess ausführen
        Path tempDir = Files.createTempDirectory("resume-");
        Path texFile = tempDir.resolve("resume.tex");
        Files.writeString(texFile, template, StandardCharsets.UTF_8);


        try {
            // 5. Führe den xelatex-Befehl aus
            boolean success = executeLatexCommand(texFile, tempDir);
            if (!success) {
                // Wenn ein Fehler auftritt, werfen wir eine Exception.
                // Im Log kann man dann die Details aus der .log-Datei des Compilers nachlesen.
                throw new IOException("LaTeX compilation failed. Check logs in " + tempDir);
            }

            // 6. Lese die erzeugte PDF-Datei als byte[]
            Path pdfFile = tempDir.resolve("resume.pdf");
            return Files.readAllBytes(pdfFile);

        } finally {
            // 7. Aufräumen: Lösche das temporäre Verzeichnis und seinen gesamten Inhalt
            if (Files.exists(tempDir)) {
                try {
                    // Gehe durch alle Dateien im Verzeichnis und lösche sie.
                    // Danach kann das leere Verzeichnis gelöscht werden.
                    Files.walk(tempDir)
                            .sorted(java.util.Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(java.io.File::delete);
                } catch (IOException e) {
                    // Logge einen Fehler, falls das Aufräumen fehlschlägt, aber lasse die Anwendung nicht abstürzen.
                    LOG.error("Failed to clean up temporary directory: " + tempDir, e);
                }
            }
        }
    }

    /**
     * lade das LaTeX-Template
     */
    private String loadTemplate() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/template.tex")) {
            if (inputStream != null) {
                // Lese den Inhalt der Datei in einen einzigen String
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new IOException("Template not found");
        }
    }

    /**
     * 模板替换
     *
     * @param template
     * @param resumeData
     * @return
     */
    private String replacePlaceholders(String template, ResumeData resumeData) {
        return template
                .replace("\\VAR{name}", sanitize(resumeData.personalInfo().name()))
                .replace("\\VAR{titel}", sanitize(resumeData.personalInfo().title()))
                .replace("\\VAR{photo_path}", sanitize(resumeData.photoPath()))
                .replace("\\VAR{telefon}", sanitize(resumeData.personalInfo().telefon()))
                .replace("\\VAR{email}", sanitize(resumeData.personalInfo().email()))
                .replace("\\VAR{adresse}", sanitize(resumeData.personalInfo().adresse()))
                .replace("\\VAR{ueber_mich_text}", sanitize(resumeData.personalInfo().ueberMichText()));

    }

    /**
     * 这里是处理列表数据
     *
     * @param template
     * @param data
     * @return
     */
    private String generateListBlock(String template, ResumeData data) {
        return template
                .replace("\\VAR{kenntnisse_block}", generateKenntnisseBlock(data.skills()))
                .replace("\\VAR{ausbildung_block}", generateAusbildungBlock(data.educationEntries()))
                .replace("\\VAR{berufserfahrung_block}", generateBerufserfahrungBlock(data.experienceEntries()))
                .replace("\\VAR{projekterfahrung_block}", generateProjekterfahrungBlock(data.projectExperienceEntries()));

    }


    /**
     * 生成技能块
     *
     * @param skills
     * @return
     */
    private String generateKenntnisseBlock(List<String> skills) {
        return skills.stream()
                .map(skill -> "\\skill{" + sanitize(skill) + "}")
                .collect(Collectors.joining("\n"));
    }

    /**
     * 生成学历块
     *
     * @param ausbildung
     * @return
     */
    private String generateAusbildungBlock(List<EducationEntry> ausbildung) {
        StringBuilder sb = new StringBuilder();
        for (EducationEntry entry : ausbildung) {
            sb.append("\\textbf{\\textcolor{graytext}{").append(sanitize(entry.title())).append("}}\\hfill ")
                    .append(sanitize(entry.zeitraum())).append("\\\\\n");
            sb.append("\\textit{").append(sanitize(entry.universitaet())).append("}\\\\[2mm]\n");
            sb.append("\\begin{itemize}\n");
            sb.append("    \\item \\textcolor{graytext}{").append(sanitize(entry.beschreibung())).append("}\n");
            sb.append("\\end{itemize}\n");
            sb.append("\\vspace{4mm}\n\n");
        }
        return sb.toString();
    }

    /**
     * 创建项目经验块
     *
     * @param berufserfahrung
     * @return
     */
    private String generateBerufserfahrungBlock(List<ExperienceEntry> berufserfahrung) {
        StringBuilder sb = new StringBuilder();
        for (ExperienceEntry entry : berufserfahrung) {
            sb.append("\\textbf{\\textcolor{graytext}{").append(sanitize(entry.title())).append("}}\\hfill ")
                    .append(sanitize(entry.zeitraum())).append("\\\\\n");
            sb.append("\\textit{").append(sanitize(entry.firma())).append("}\\\\[2mm]\n");
            sb.append("\\begin{itemize}\n");
            for (String desc : entry.beschreibung()) {
                sb.append("    \\item \\textcolor{graytext}{").append(sanitize(desc)).append("}\n");
            }
            sb.append("\\end{itemize}\n");
            sb.append("\\vspace{4mm}\n\n");
        }
        return sb.toString();
    }

    /**
     * 生成项目经验块
     *
     * @param projekterfahrung
     * @return
     */
    private String generateProjekterfahrungBlock(List<ProjectExperienceEntry> projekterfahrung) {
        StringBuilder sb = new StringBuilder();
        for (ProjectExperienceEntry entry : projekterfahrung) {
            sb.append("\\textbf{\\textcolor{graytext}{").append(sanitize(entry.projektTitel())).append("}}\\hfill ")
                    //暂时不处理时间
//                    .append(sanitize(entry.zeitraum())).append("\\\\\n");
                    .append("\\\\\n");
            sb.append("\\textit{\\textcolor{graytext}{Technologien: ").append(sanitize(entry.technologien())).append("}}\\\\[2mm]\n");
            sb.append("\\begin{itemize}\n");
            for (String desc : entry.beschreibung()) {
                sb.append("    \\item \\textcolor{graytext}{").append(sanitize(desc)).append("}\n");
            }
            // Füge den GitHub-Link hinzu, wenn vorhanden
            if (entry.link() != null && !entry.link().isBlank()) {
                sb.append("    \\item \\faGithub\\ \\href{").append(sanitize(entry.link())).append("}{Projekt auf GitHub}\n");
            }
            sb.append("\\end{itemize}\n");
            sb.append("\\vspace{4mm}\n\n");
        }
        return sb.toString();
    }

    /**
     * 运行Latex命令
     *
     * @param texFile
     * @param tempDir
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private boolean executeLatexCommand(Path texFile, Path tempDir) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "pdflatex", // <--- GEÄNDERT VON "xelatex"
                "-interaction=nonstopmode",
                "-output-directory=" + tempDir.toAbsolutePath(),
                texFile.toAbsolutePath().toString()
        );
        // DIESE ZEILE IST DIE LÖSUNG:
        // Leitet die Standard-Ausgabe (stdout) und die Fehler-Ausgabe (stderr)
        // des xelatex-Prozesses direkt in die Konsole unserer Quarkus-Anwendung um.
        processBuilder.inheritIO();

        // Prozess starten
        Process process = processBuilder.start();
        int exitCode = process.waitFor(); // Warten, bis der Prozess beendet ist

        // Wenn der Exit-Code nicht 0 ist, ist ein Fehler aufgetreten.
        // In einer echten Anwendung würden Sie hier die Log-Datei auslesen und den Fehler protokollieren.
        return exitCode == 0;
    }

    /**
     * Bereinigt einen String für die Verwendung in LaTeX.
     * Ersetzt null durch einen leeren String und escaped spezielle LaTeX-Zeichen.
     */
    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\textbackslash{}")
                .replace("&", "\\&")
                .replace("%", "\\%")
                .replace("$", "\\$")
                .replace("#", "\\#")
                .replace("_", "\\_")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("~", "\\textasciitilde{}")
                .replace("^", "\\textasciicircum{}");
    }
}
