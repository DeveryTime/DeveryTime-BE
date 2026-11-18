package com.dms.deverytime.global.cloudinary.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dms.deverytime.global.cloudinary.dto.ImageUploadResult;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private final Cloudinary cloudinary;
    private final Tika tika = new Tika();
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    public String newPostImagePublicId() {
        return "posts/" + UUID.randomUUID();
    }

    public ImageUploadResult uploadReservedPostImage(MultipartFile file, String publicId) {
        validateImage(file);
        try {
            Map result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("public_id", publicId, "overwrite", false));
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
            log.error("Cloudinary 이미지 업로드 실패: {}", e.getMessage());
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
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true));
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
