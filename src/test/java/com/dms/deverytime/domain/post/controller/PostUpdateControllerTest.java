package com.dms.deverytime.domain.post.controller;

import com.dms.deverytime.domain.post.dto.response.PostUpdateResponse;
import com.dms.deverytime.domain.post.entity.PostCreateStatus;
import com.dms.deverytime.domain.post.service.PostService;
import com.dms.deverytime.global.exception.GlobalExceptionHandler;
import com.dms.deverytime.global.security.auth.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PostUpdateControllerTest {
    private PostService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(PostService.class);
        CustomUserDetails principal = mock(CustomUserDetails.class);
        when(principal.getUserId()).thenReturn(5L);
        mvc = MockMvcBuilders.standaloneSetup(new PostController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType() == CustomUserDetails.class;
                    }
                    @Override public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                            NativeWebRequest request, WebDataBinderFactory factory) {
                        return principal;
                    }
                }).build();
    }

    @Test
    void acceptsCategoryAndStatusAndReturnsSpecifiedResponseObject() throws Exception {
        when(service.updatePost(eq(10L), any(), eq(5L))).thenReturn(new PostUpdateResponse(
                10L, "title", PostCreateStatus.PUBLISHED, LocalDateTime.of(2026, 11, 15, 12, 0)));
        mvc.perform(put("/api/posts/10").contentType("application/json")
                        .content("{\"categoryId\":3,\"title\":\"title\",\"content\":\"text\",\"status\":\"PUBLISHED\",\"deleteImageIds\":[21,22]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.title").value("title"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-11-15T12:00:00"))
                .andExpect(jsonPath("$.message").value("게시글이 수정되었습니다."));
        verify(service).updatePost(eq(10L), argThat(request -> request.getCategoryId().equals(3L)
                && request.getStatus() == PostCreateStatus.PUBLISHED
                && List.of(21L, 22L).equals(request.getDeleteImageIds())), eq(5L));
    }

    @Test
    void stillAcceptsOldRequestsWithoutCategoryAndStatus() throws Exception {
        mvc.perform(put("/api/posts/10").contentType("application/json")
                        .content("{\"title\":\"title\",\"content\":\"text\"}"))
                .andExpect(status().isOk());
        verify(service).updatePost(eq(10L), argThat(request -> request.getCategoryId() == null
                && request.getStatus() == null), eq(5L));
    }

    @Test
    void rejectsInvalidCategoryAndContentLength() throws Exception {
        mvc.perform(put("/api/posts/10").contentType("application/json")
                        .content("{\"categoryId\":0,\"title\":\"title\",\"content\":\"text\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mvc.perform(put("/api/posts/10").contentType("application/json")
                        .content("{\"title\":\"title\",\"content\":\"" + "x".repeat(10001) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void unknownStatusReturnsValidationErrorInsteadOfInternalServerError() throws Exception {
        mvc.perform(put("/api/posts/10").contentType("application/json")
                        .content("{\"title\":\"title\",\"content\":\"text\",\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(service);
    }
}
