package com.dms.deverytime.domain.comment.service;


import com.dms.deverytime.domain.comment.entity.Comment;
import com.dms.deverytime.domain.comment.repository.CommentRepository;
import com.dms.deverytime.domain.comment.dto.request.CommentCreateRequest;
import com.dms.deverytime.domain.comment.dto.request.CommentUpdateRequest;
import com.dms.deverytime.domain.comment.dto.response.CommentCreateResponse;
import com.dms.deverytime.domain.comment.dto.response.CommentListResponse;
import com.dms.deverytime.domain.comment.dto.response.CommentUpdateResponse;
import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.repository.PostRepository;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.domain.user.repository.UserRepository;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentCreateResponse createComment (Long postId, Long userId, CommentCreateRequest request){
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new DeveryTimeException(ErrorCode.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.USER_NOT_FOUND));

        Comment comment= Comment.builder()
                .post(post)
                .user(user)
                .content(request.content())
                        .build();

        Comment savedComment = commentRepository.save(comment);
        return new CommentCreateResponse(savedComment.getId(),post.getId(), user.getId(), savedComment.getContent(),savedComment.getCreatedAt()
        );
    }

    public List<CommentListResponse> getList(Long postId){
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new DeveryTimeException(ErrorCode.POST_NOT_FOUND));

        List<Comment> comments =commentRepository.findAllByPostIdWithUser(postId);

        return comments.stream()
            .map(comment-> new CommentListResponse(
                    comment.getId(),
                    comment.getUser().getId(),
                    comment.getUser().getName(),
                    comment.getUser().getProfileImageUrl(),
                    comment.getContent(),
                    comment.getCreatedAt()

            ))
                    .toList();

    }
    public CommentUpdateResponse updateComment(Long postId,Long commentId,Long userId,CommentUpdateRequest request){
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new DeveryTimeException(ErrorCode.POST_NOT_FOUND));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(()->new DeveryTimeException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getPost().getId().equals(postId)) {
            throw new DeveryTimeException(ErrorCode.COMMENT_NOT_FOUND);
        }

        if (!comment.getUser().getId().equals(userId)) {
            throw new DeveryTimeException(ErrorCode.COMMENT_ACCESS_DENIED);
        }

        comment.updateContent(request.content());

        commentRepository.flush();

        return new CommentUpdateResponse(
                comment.getId(),
                comment.getContent(),
                comment.getUpdatedAt()
        );
    }

    public void deleteComment(Long postId,Long commentId,Long userId){
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new DeveryTimeException(ErrorCode.POST_NOT_FOUND));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(()->new DeveryTimeException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getPost().getId().equals(postId)) {
            throw new DeveryTimeException(ErrorCode.COMMENT_NOT_FOUND);
        }

        if (!comment.getUser().getId().equals(userId)) {
            throw new DeveryTimeException(ErrorCode.COMMENT_ACCESS_DENIED);
        }

        commentRepository.delete(comment);
    }


}
