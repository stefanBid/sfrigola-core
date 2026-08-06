package com.sb.sfrigola_core.common.interfaces.service_interfaces;

import org.springframework.web.multipart.MultipartFile;

/**
 * Cross-cutting file storage contract for the local filesystem today, decoupled from
 * any specific storage backend so it can move to object storage later without callers
 * changing. Does NOT read the security context; all required data is received explicitly.
 * Intended for use by any domain service that needs to persist an uploaded file
 * (e.g. recipe cover, user avatar) and get back a reference to store on the owning entity;
 * this service does not track which entity owns a given file.
 */
public interface ISCFileStorageService {

    /**
     * Stores (or overwrites, if the resolved path already exists) the given file. The
     * stored file's extension always comes from {@code file}'s original filename, lower-cased
     * — never from {@code fileName} — so a stale/incorrect extension on {@code fileName} can
     * never end up on disk. {@code fileName} is sanitized (path stripped, non-alphanumeric
     * characters replaced) before use; if blank or {@code null}, the sanitized original
     * filename is used as a fallback base name instead.
     *
     * @param file       the file content to persist; caller is responsible for prior validation
     *                    (e.g. {@code com.sb.sfrigola_core.common.annotations.validations.image.ValidImage})
     * @param folderPath namespace/subdirectory under the storage root (e.g. {@code "avatars"}, {@code "recipe-covers"});
     *                    may be blank/{@code null} to store directly under the storage root
     * @param fileName   the desired base name, without extension (any extension present is ignored);
     *                    may be blank/{@code null} to fall back to the file's own original filename
     * @return the relative reference ({@code folderPath + "/" + sanitizedFileName + extension}) to
     *         persist and later pass to {@link #deleteFiles}
     * @throws com.sb.sfrigola_core.common.exception.ex.SCFileStorageException if {@code file}'s original
     *         filename has no extension, if neither {@code fileName} nor the original filename yields a
     *         usable base name, or if the file cannot be written
     */
    String upsetFile(MultipartFile file, String folderPath, String fileName);

    /**
     * Deletes a previously stored file. No-op if it does not exist.
     *
     * @param fileName the relative reference previously returned by {@link #upsetFile}; must not be blank/{@code null}
     * @throws com.sb.sfrigola_core.common.exception.ex.SCFileStorageException if {@code fileName} is blank/{@code null},
     *         or if the file exists but cannot be deleted
     */
    void deleteFiles(String fileName);
}
