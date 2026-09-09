package com.dms.deverytime.domain.postlike.service;


import com.dms.deverytime.domain.post.entity.Post;
import com.dms.deverytime.domain.post.repository.PostRepository;
import com.dms.deverytime.domain.postlike.dto.response.PostLikeResponse;
import com.dms.deverytime.domain.postlike.entity.PostLike;
import com.dms.deverytime.domain.postlike.repository.PostLikeRepository;
import com.dms.deverytime.domain.user.entity.User;
import com.dms.deverytime.domain.user.repository.UserRepository;
import com.dms.deverytime.global.exception.DeveryTimeException;
import com.dms.deverytime.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {
    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;


    @Transactional
    public PostLikeResponse createLike(Long postId, Long userId){
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new DeveryTimeException(ErrorCode.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.USER_NOT_FOUND));

        boolean alreadyLiked =
                postLikeRepository.existsByPostIdAndUserId(postId, userId);

        if (alreadyLiked) {
            throw new DeveryTimeException(ErrorCode.ALREADY_LIKED);
        }

        PostLike postLike = new PostLike(post, user);
        postLikeRepository.save(postLike);

        return new PostLikeResponse(
          post.getId(),
          user.getId(),
                true

        );
    }

    @Transactional
    public PostLikeResponse cancelLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(()->new DeveryTimeException(ErrorCode.POST_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.USER_NOT_FOUND));

        PostLike postLike = postLikeRepository.findByPostIdAndUserId(postId, userId)
                .orElseThrow(() -> new DeveryTimeException(ErrorCode.NOT_LIKED));

        postLikeRepository.delete(postLike);

        return new PostLikeResponse(
                post.getId(),
                user.getId(),
                false
        );
    }


}
