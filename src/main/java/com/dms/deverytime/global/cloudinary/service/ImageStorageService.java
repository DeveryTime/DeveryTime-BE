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

@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private final Cloudinary cloudinary;
    private final Tika tika = new Tika();
    private static final long MAX_IMAGE_SIZE = 4 * 1024 * 1024;

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

    public void delete(String publicId){
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
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
