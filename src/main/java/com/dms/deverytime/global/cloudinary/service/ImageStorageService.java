package com.dms.deverytime.global.cloudinary.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dms.deverytime.global.cloudinary.dto.ImageUploadResult;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private final Cloudinary cloudinary;
    private final Tika tika = new Tika();
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int RESPONSE_TIMEOUT_SECONDS = 30;

    public String newPostImagePublicId() {
        return "posts/" + UUID.randomUUID();
    }

    public ImageUploadResult uploadReservedPostImage(MultipartFile file, String publicId) {
        validateImage(file);
        try {
            Map result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("public_id", publicId, "overwrite", false,
                            "connect_timeout", CONNECT_TIMEOUT_SECONDS, "connection_request_timeout", CONNECT_TIMEOUT_SECONDS,
                            "timeout", RESPONSE_TIMEOUT_SECONDS));
            if (!publicId.equals(result.get("public_id"))) {
                throw new DeveryTimeException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }
            return ImageUploadResult.of(result.get("secure_url").toString(), publicId);
        } catch (IOException | RuntimeException e) {
            throw new DeveryTimeException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    public ImageUploadResult upload(MultipartFile file, String folder){

        Map uploadResult;
        validateImage(file);

        try {
            uploadResult = cloudinary.uploader().upload(
                    file.getBytes(), ObjectUtils.asMap("folder", folder));

        } catch (IOException | RuntimeException e) {
            throw new DeveryTimeException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        return ImageUploadResult.of(uploadResult.get("secure_url").toString(),
                uploadResult.get("public_id").toString());
    }

    public void delete(String publicId) {
        deleteIfPresent(publicId);
    }

    // 업로드 결과가 불확실할 때 not found는 아직 업로드 중일 가능성이 있다.
    public boolean deleteUploadCandidate(String publicId) {
        return deleteIfPresent(publicId);
    }

    private boolean deleteIfPresent(String publicId) {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true, "connect_timeout", CONNECT_TIMEOUT_SECONDS,
                    "connection_request_timeout", CONNECT_TIMEOUT_SECONDS, "timeout", RESPONSE_TIMEOUT_SECONDS));
            if ("ok".equals(result.get("result"))) return true;
            if ("not found".equals(result.get("result"))) return false;
            throw new DeveryTimeException(ErrorCode.IMAGE_DELETE_FAILED);
        } catch (IOException | RuntimeException e) {
            throw new DeveryTimeException(ErrorCode.IMAGE_DELETE_FAILED);
        }
    }

    public void validateImage(MultipartFile file){
        if (file == null || file.isEmpty())
            throw new DeveryTimeException(ErrorCode.INVALID_IMAGE_FILE);

        if (file.getSize() > MAX_IMAGE_SIZE)
            throw new DeveryTimeException(ErrorCode.IMAGE_FILE_TOO_LARGE);

        try {
            String detectedType = tika.detect(file.getInputStream());

            if (!(detectedType.equals("image/jpeg") || detectedType.equals("image/png")
                    || detectedType.equals("image/webp")))
                throw new DeveryTimeException(ErrorCode.INVALID_IMAGE_FILE);

        } catch (IOException e) {
            throw new DeveryTimeException(ErrorCode.INVALID_IMAGE_TYPE);
        }
    }
}
