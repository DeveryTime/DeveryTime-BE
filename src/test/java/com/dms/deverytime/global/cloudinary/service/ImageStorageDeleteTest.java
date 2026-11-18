package com.dms.deverytime.global.cloudinary.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImageStorageDeleteTest {
    @Test
    void deletionAcceptsOkAndAlreadyMissingResults() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("file"), anyMap()))
                .thenReturn(Map.of("result", "ok"))
                .thenReturn(Map.of("result", "not found"));
        ImageStorageService storage = new ImageStorageService(cloudinary);

        assertThatCode(() -> storage.delete("file")).doesNotThrowAnyException();
        assertThatCode(() -> storage.delete("file")).doesNotThrowAnyException();
    }

    @Test
    void missingUploadCandidateIsNotConsideredCleanedUp() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("file"), anyMap()))
                .thenReturn(Map.of("result", "not found"), Map.of("result", "ok"));
        ImageStorageService storage = new ImageStorageService(cloudinary);

        assertThat(storage.deleteUploadCandidate("file")).isFalse();
        assertThat(storage.deleteUploadCandidate("file")).isTrue();
        verify(uploader, times(2)).destroy(eq("file"), argThat(options ->
                Integer.valueOf(10).equals(options.get("connect_timeout"))
                        && Integer.valueOf(10).equals(options.get("connection_request_timeout"))
                        && Integer.valueOf(30).equals(options.get("timeout"))));
    }

    @Test
    void unsuccessfulStorageResponseIsNotTreatedAsSuccessfulDeletion() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("file"), anyMap())).thenReturn(Map.of("result", "error"));
        ImageStorageService storage = new ImageStorageService(cloudinary);

        assertThatThrownBy(() -> storage.delete("file")).isInstanceOfSatisfying(DeveryTimeException.class,
                failure -> assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.IMAGE_DELETE_FAILED));
    }

    @Test
    void ioFailureIsReportedForRetry() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("file"), anyMap())).thenThrow(new IOException("storage unavailable"));
        ImageStorageService storage = new ImageStorageService(cloudinary);

        assertThatThrownBy(() -> storage.delete("file")).isInstanceOf(DeveryTimeException.class);
    }
}
