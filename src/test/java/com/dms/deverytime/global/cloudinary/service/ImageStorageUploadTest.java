package com.dms.deverytime.global.cloudinary.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.dms.deverytime.global.exception.DeveryTimeException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImageStorageUploadTest {
    private final Cloudinary cloudinary = mock(Cloudinary.class);
    private final Uploader uploader = mock(Uploader.class);
    private final ImageStorageService storage = new ImageStorageService(cloudinary);
    private final MockMultipartFile image = new MockMultipartFile("file", "a.png", "image/png",
            new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

    @Test
    void uploadsWithReservedPublicIdAndDisablesOverwrite() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("public_id", "posts/reserved", "secure_url", "https://example.com/a.png"));

        var result = storage.uploadReservedPostImage(image, "posts/reserved");

        assertThat(result.profileImagePublicId()).isEqualTo("posts/reserved");
        verify(uploader).upload(any(byte[].class), argThat(options ->
                "posts/reserved".equals(options.get("public_id")) && Boolean.FALSE.equals(options.get("overwrite"))
                        && Integer.valueOf(10).equals(options.get("connect_timeout"))
                        && Integer.valueOf(10).equals(options.get("connection_request_timeout"))
                        && Integer.valueOf(30).equals(options.get("timeout"))));
    }

    @Test
    void rejectsUnexpectedPublicId() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("public_id", "unexpected", "secure_url", "https://example.com/a.png"));

        assertThatThrownBy(() -> storage.uploadReservedPostImage(image, "posts/reserved"))
                .isInstanceOf(DeveryTimeException.class);
    }
}
