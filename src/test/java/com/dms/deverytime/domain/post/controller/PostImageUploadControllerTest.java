package com.dms.deverytime.domain.post.controller;

import com.dms.deverytime.domain.post.dto.response.PostImageResponse;
import com.dms.deverytime.domain.post.service.PostService;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import com.dms.deverytime.global.exception.GlobalExceptionHandler;
import com.dms.deverytime.global.security.auth.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PostImageUploadControllerTest {
    private PostService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(PostService.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getUserId()).thenReturn(5L);
        mvc = MockMvcBuilders.standaloneSetup(new PostController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType() == CustomUserDetails.class;
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                                  NativeWebRequest request, WebDataBinderFactory factory) {
                        return principal;
                    }
                }).build();
    }

    @Test
    void repeatedFilesPartsBindToListAndReturnCreatedArray() throws Exception {
        when(service.uploadPostImages(eq(10L), anyList(), eq(5L))).thenReturn(List.of(
                new PostImageResponse(21L, "https://example.com/a.png", 0),
                new PostImageResponse(22L, "https://example.com/b.png", 1)));

        mvc.perform(multipart("/api/posts/10/images")
                        .file(new MockMultipartFile("files", "a.png", "image/png", new byte[]{1}))
                        .file(new MockMultipartFile("files", "b.png", "image/png", new byte[]{2})))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(21))
                .andExpect(jsonPath("$.data[1].sortOrder").value(1));

        verify(service).uploadPostImages(eq(10L), argThat(files -> files.size() == 2
                && "a.png".equals(files.get(0).getOriginalFilename())
                && "b.png".equals(files.get(1).getOriginalFilename())), eq(5L));
    }

    @Test
    void missingFilesReturnsBadRequestInsteadOfGenericServerError() throws Exception {
        when(service.uploadPostImages(eq(10L), isNull(), eq(5L)))
                .thenThrow(new DeveryTimeException(ErrorCode.INVALID_IMAGE_FILE));

        mvc.perform(multipart("/api/posts/10/images"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_IMAGE_FILE"));
    }

    @Test
    void limitExceededReturnsSpecifiedError() throws Exception {
        when(service.uploadPostImages(eq(10L), anyList(), eq(5L)))
                .thenThrow(new DeveryTimeException(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED));

        mvc.perform(multipart("/api/posts/10/images")
                        .file(new MockMultipartFile("files", "a.png", "image/png", new byte[]{1})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("POST_IMAGE_LIMIT_EXCEEDED"));
    }

    @Test
    void replacementAcceptsSingleFileWithPutAndReturnsImageIdentity() throws Exception {
        when(service.replacePostImage(eq(10L), eq(21L), any(), eq(5L)))
                .thenReturn(new PostImageResponse(21L, "https://example.com/new.png", 2));

        mvc.perform(multipart(HttpMethod.PUT, "/api/posts/10/images/21")
                        .file(new MockMultipartFile("file", "new.png", "image/png", new byte[]{1})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(21))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/new.png"))
                .andExpect(jsonPath("$.data.sortOrder").value(2))
                .andExpect(jsonPath("$.data.publicId").doesNotExist());
    }

    @Test
    void replacementWithoutFileReturnsBadRequest() throws Exception {
        when(service.replacePostImage(eq(10L), eq(21L), isNull(), eq(5L)))
                .thenThrow(new DeveryTimeException(ErrorCode.INVALID_IMAGE_FILE));

        mvc.perform(multipart(HttpMethod.PUT, "/api/posts/10/images/21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_IMAGE_FILE"));
    }

    @Test
    void replacementOfMissingImageReturnsNotFound() throws Exception {
        when(service.replacePostImage(eq(10L), eq(21L), any(), eq(5L)))
                .thenThrow(new DeveryTimeException(ErrorCode.POST_IMAGE_NOT_FOUND));

        mvc.perform(multipart(HttpMethod.PUT, "/api/posts/10/images/21")
                        .file(new MockMultipartFile("file", "new.png", "image/png", new byte[]{1})))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("POST_IMAGE_NOT_FOUND"));
    }
}
