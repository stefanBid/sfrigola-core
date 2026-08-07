package com.sb.sfrigola_core.common.service;

import com.sb.sfrigola_core.common.constant.SCGeneralConstants;
import com.sb.sfrigola_core.common.exception.ex.SCFileStorageException;
import com.sb.sfrigola_core.common.interfaces.service_interfaces.ISCFileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SCFileSystemImageStorageService implements ISCFileStorageService {
    private final Environment env;
    private  Path rootDir;

    @PostConstruct
    public void init() {
        var rootDirProperty = env.getProperty(SCGeneralConstants.FILE_STORAGE_ROOT_DIR);
        if (rootDirProperty == null || rootDirProperty.isBlank()) {
            throw new IllegalStateException(SCGeneralConstants.FILE_STORAGE_ROOT_DIR + " is not configured");
        }
        this.rootDir = Path.of(rootDirProperty).toAbsolutePath().normalize();
    }

    @Override
    public String upsetFile(MultipartFile file, String folderPath, String fileName) {
        String extractedExtension = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new SCFileStorageException("upsetFile", fileName, "original filename does not contain an extension"));

        String purifiedBaseName = purifierFileName(fileName)
                .or(() -> purifierFileName(file.getOriginalFilename()))
                .orElseThrow(() -> new SCFileStorageException("upsetFile", fileName, "no usable file name (fileName and original filename both blank)"));
        String fullFileName = purifiedBaseName + extractedExtension;

        Path targetDir = (folderPath != null && !folderPath.isBlank()) ? resolveWithinRoot(rootDir, folderPath) : rootDir;
        Path targetFile = resolveWithinRoot(targetDir, fullFileName);

        if(fileAlreadyExists(targetFile)) {
            log.warn("File '{}' already exists under '{}', it will be overwritten", fullFileName, folderPath);

        }


        try {
            Files.createDirectories(targetDir);
            Path tempFile = Files.createTempFile(targetDir,fullFileName + "_", ".tmp" );
            try {
                file.transferTo(tempFile);
                Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            log.error("Failed to store file '{}' under '{}'", fullFileName, folderPath, e);
            throw new SCFileStorageException("store", fullFileName);
        }

        return (folderPath != null && !folderPath.isBlank()) ? folderPath + "/" + fullFileName : fullFileName;
    }

    @Override
    public void deleteFiles(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new SCFileStorageException("delete", fileName, "fileName is required");
        }
        Path targetFile = resolveWithinRoot(rootDir, fileName);

        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException e) {
            log.error("Failed to delete file '{}'", fileName, e);
            throw new SCFileStorageException("delete", fileName);
        }
    }

    // Guards against path traversal (e.g. "../../etc/passwd") in folderPath/fileName —
    // even though callers are expected to pass server-generated names, never trust them blind.
    private Path resolveWithinRoot(Path base, String segment) {
        Path resolved = base.resolve(segment).normalize();
        if (!resolved.startsWith(base)) {
            throw new SCFileStorageException("resolve", segment);
        }
        return resolved;
    }

    private boolean fileAlreadyExists(Path targetFile) {
        return Files.exists(targetFile);
    }

    private Optional<String> purifierFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        String fileNameWithoutPath = Path.of(fileName).getFileName().toString();
        String fileNameWithoutExtension = fileNameWithoutPath.contains(".") ? fileNameWithoutPath.substring(0, fileNameWithoutPath.lastIndexOf('.')) : fileNameWithoutPath;
        String purified = fileNameWithoutExtension.replaceAll("[^a-zA-Z0-9-_]", "_");
        return Optional.of(purified);
    }
}
