package com.example.controller;

import com.example.model.ImportLog;
import com.example.repository.ImportLogRepository;
import com.example.service.ImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/import")
public class ImportController {

    private static final String UPLOAD_DIR = System.getProperty("java.io.tmpdir");

    @Autowired
    private ImportService importService;

    @Autowired
    private ImportLogRepository importLogRepository;

    @GetMapping
    public String showImportForm() {
        return "import-form";
    }

    @PostMapping("/upload")
    public String uploadFileAndRedirect(@RequestParam("file") MultipartFile file, RedirectAttributes attributes) {
        if (file.isEmpty()) {
            attributes.addFlashAttribute("error", "Пожалуйста, выберите файл для загрузки.");
            return "redirect:/import";
        }

        try {
            String tempFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR, tempFileName);
            Files.write(path, file.getBytes());

            attributes.addFlashAttribute("tempFileName", tempFileName);
            attributes.addFlashAttribute("originalFileName", file.getOriginalFilename());
            return "redirect:/import/mapping";

        } catch (IOException e) {
            attributes.addFlashAttribute("error", "Не удалось сохранить временный файл: " + e.getMessage());
            return "redirect:/import";
        }
    }
    @GetMapping("/mapping")
    public String showMappingPage(Model model) {
        if (!model.containsAttribute("tempFileName")) {
            return "redirect:/import";
        }

        String tempFileName = (String) model.asMap().get("tempFileName");
        String originalFileName = (String) model.asMap().get("originalFileName");
        File tempFile = new File(UPLOAD_DIR, tempFileName);

        try {
            MultipartFile multipartFile = new MockMultipartFile(tempFileName, Files.readAllBytes(tempFile.toPath()));
            Map<String, String> suggestedMappings = importService.suggestMappings(multipartFile);

            model.addAttribute("mappings", suggestedMappings);
            model.addAttribute("targetFields", importService.getTargetFields());
            model.addAttribute("tempFileName", tempFileName);
            model.addAttribute("fileName", originalFileName);

            return "confirm-mapping";

        } catch (Exception e) {
            model.addAttribute("error", "Ошибка при анализе файла: " + e.getMessage());
            return "import-form";
        }
    }

    @PostMapping("/execute")
    public String executeImport(@RequestParam("tempFileName") String tempFileName,
                                @RequestParam Map<String, String> mappings,
                                RedirectAttributes attributes) {

        File tempFile = new File(UPLOAD_DIR, tempFileName);
        if (!tempFile.exists()) {
            attributes.addFlashAttribute("error", "Файл для импорта не найден. Возможно, сессия истекла.");
            return "redirect:/import/history";
        }

        try {
            Path path = Paths.get(tempFile.getAbsolutePath());
            byte[] fileBytes = Files.readAllBytes(path);
            MultipartFile multipartFile = new MockMultipartFile(tempFileName, fileBytes);
            Map<String, String> confirmedMappings = mappings.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("map."))
                    .collect(java.util.stream.Collectors.toMap(
                            e -> e.getKey().substring(4),
                            Map.Entry::getValue
                    ));

            importService.importHumansFromCsv(multipartFile, confirmedMappings);
            attributes.addFlashAttribute("success", "Импорт успешно завершен!");
            return "redirect:/import/history";

        } catch (Exception e) {
            attributes.addFlashAttribute("error", "Ошибка импорта: " + e.getMessage());
            return "redirect:/import/history";

        } finally {
            try {
                if (tempFile != null && tempFile.exists()) {
                    Files.deleteIfExists(tempFile.toPath());
                }
            } catch (IOException e) {
                System.err.println("Не удалось удалить временный файл: " + tempFile.getAbsolutePath());
                e.printStackTrace();
            }
        }
    }


    @GetMapping("/history")
    public String showHistory(Model model) {
        List<ImportLog> logs = importLogRepository.findAllByOrderByIdDesc();
        model.addAttribute("logs", logs);
        return "import-history";
    }

    private static class MockMultipartFile implements MultipartFile {
        private final String name;
        private final byte[] content;
        public MockMultipartFile(String name, byte[] content) { this.name = name; this.content = content; }
        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return "text/csv"; }
        @Override public boolean isEmpty() { return content == null || content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() throws IOException { return content; }
        @Override public java.io.InputStream getInputStream() throws IOException { return new java.io.ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws IOException, IllegalStateException { Files.write(dest.toPath(), content); }
    }
}