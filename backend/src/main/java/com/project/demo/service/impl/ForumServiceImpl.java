package com.project.demo.service.impl;

import com.project.demo.dto.CommentResponse;
import com.project.demo.dto.CreateCommentRequest;
import com.project.demo.dto.CreatePostRequest;
import com.project.demo.dto.PageResponse;
import com.project.demo.dto.PostResponse;
import com.project.demo.dto.SectionResponse;
import com.project.demo.dto.UpdatePostRequest;
import com.project.demo.entity.*;
import com.project.demo.exception.BusinessException;
import com.project.demo.mapper.*;
import com.project.demo.service.ForumService;
import com.project.demo.service.RedisLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch._types.SortOrder;
import com.project.demo.config.RabbitMQConfig;
import com.project.demo.document.ForumPostDocument;
import com.project.demo.dto.ForumPostSyncMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import com.project.demo.repository.ForumPostSearchRepository;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumServiceImpl implements ForumService {

    private final ForumSectionMapper sectionMapper;
    private final ForumPostMapper postMapper;
    private final ForumCommentMapper commentMapper;
    private final ForumPostFavoriteMapper favoriteMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisLikeService redisLikeService;
    private final RabbitTemplate rabbitTemplate;
    private final ElasticsearchOperations esOperations;
    private final ForumPostSearchRepository esRepository;

    private static final String SECTION_CACHE_KEY = "forum:sections";
    private static final long SECTION_CACHE_TTL_MINUTES = 30;
    private static final int MAX_PAGE = 100;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<SectionResponse> getSectionList() {
        try {
            String cachedJson = (String) redisTemplate.opsForValue().get(SECTION_CACHE_KEY);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson,
                        new TypeReference<List<SectionResponse>>() {});
            }
        } catch (Exception e) {
            redisTemplate.delete(SECTION_CACHE_KEY);
        }

        List<ForumSection> sections = sectionMapper.selectAllEnabled();
        List<SectionResponse> result = sections.stream()
                .map(this::toSectionResponse)
                .toList();

        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(SECTION_CACHE_KEY, json, SECTION_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            // 缓存写入失败不影响主流程
        }

        return result;
    }

    private SectionResponse toSectionResponse(ForumSection section) {
        return new SectionResponse(
                section.getId(),
                section.getName(),
                section.getDescription(),
                section.getIcon(),
                section.getPostCount()
        );
    }

    @Override
    @Transactional
    public PostResponse createPost(Long userId, CreatePostRequest request) {
        ForumSection section = sectionMapper.selectById(request.getSectionId());
        if (section == null || section.getStatus() != 1) {
            throw BusinessException.badRequest("板块不存在或已禁用");
        }

        ForumPost post = new ForumPost();
        post.setUserId(userId);
        post.setSectionId(request.getSectionId());
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setImages(request.getImages());
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setIsSticky(0);
        post.setStatus(1);
        postMapper.insert(post);

        // 异步同步到 ES
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.FORUM_SYNC_EXCHANGE,
                RabbitMQConfig.FORUM_SYNC_ROUTING_KEY,
                ForumPostSyncMessage.create(post.getId()));

        // 原子增加板块帖子数
        sectionMapper.incPostCount(section.getId(), 1);

        // 清除板块缓存
        redisTemplate.delete(SECTION_CACHE_KEY);

        return toPostResponse(post, userId, Set.of(), Map.of());
    }

    @Override
    @Transactional
    public boolean toggleLike(Long userId, Long postId) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw BusinessException.notFound("帖子不存在");
        }

        if (redisLikeService.hasLikedPost(postId, userId)) {
            redisLikeService.unlikePost(postId, userId);
            postMapper.incLikeCount(postId, -1);
            return false;
        } else {
            redisLikeService.likePost(postId, userId);
            postMapper.incLikeCount(postId, 1);
            return true;
        }
    }

    @Override
    @Transactional
    public CommentResponse createComment(Long userId, Long postId, CreateCommentRequest request) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw BusinessException.notFound("帖子不存在");
        }

        if (request.getParentId() != null) {
            ForumComment parent = commentMapper.selectById(request.getParentId());
            if (parent == null || !parent.getPostId().equals(postId)) {
                throw BusinessException.badRequest("父评论不存在");
            }
        }

        ForumComment comment = new ForumComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent());
        comment.setLikeCount(0);
        comment.setStatus(1);
        commentMapper.insert(comment);

        // 原子增加帖子评论数
        postMapper.incCommentCount(postId, 1);

        ForumComment fullComment = commentMapper.selectById(comment.getId());
        return toCommentResponseWithReplies(fullComment, userId, Map.of(), Map.of());
    }

    @Override
    public PageResponse<PostResponse> getPostList(Long userId, Long sectionId, String sortBy, int page, int size) {
        validatePage(page);
        int offset = (page - 1) * size;
        List<ForumPost> posts = postMapper.selectPostList(sectionId, null, sortBy, offset, size);
        Long total = postMapper.countPostList(sectionId);

        return buildPostPageResponse(posts, total, page, size, userId);
    }

    @Override
    public PostResponse getPostById(Long postId, Long userId) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw BusinessException.notFound("帖子不存在");
        }

        // 使用Redis累加浏览数，避免每次查看都写库
        redisLikeService.incrementViewCount(postId);

        Set<Long> favoritedPostIds = userId != null
                ? new HashSet<>(favoriteMapper.selectFavoritedPostIds(userId, List.of(postId)))
                : Set.of();

        Map<Long, Boolean> likedMap = userId != null
                ? redisLikeService.hasLikedPostsBatch(List.of(postId), userId)
                : Map.of();

        return toPostResponse(post, userId, favoritedPostIds, likedMap);
    }

    @Override
    public PostResponse updatePost(Long userId, Long postId, UpdatePostRequest request) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw BusinessException.notFound("帖子不存在");
        }
        if (!post.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权限修改此帖子");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setImages(request.getImages());
        postMapper.update(post);

        // 异步同步到 ES
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.FORUM_SYNC_EXCHANGE,
                RabbitMQConfig.FORUM_SYNC_ROUTING_KEY,
                ForumPostSyncMessage.update(postId));

        Set<Long> favoritedPostIds = userId != null
                ? new HashSet<>(favoriteMapper.selectFavoritedPostIds(userId, List.of(postId)))
                : Set.of();

        Map<Long, Boolean> likedMap = userId != null
                ? redisLikeService.hasLikedPostsBatch(List.of(postId), userId)
                : Map.of();

        return toPostResponse(post, userId, favoritedPostIds, likedMap);
    }

    @Override
    public void deletePost(Long userId, Long postId) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null) {
            throw BusinessException.notFound("帖子不存在");
        }
        if (!post.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权限删除此帖子");
        }

        post.setStatus(0);
        postMapper.update(post);

        // 异步同步到 ES（删除文档）
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.FORUM_SYNC_EXCHANGE,
                RabbitMQConfig.FORUM_SYNC_ROUTING_KEY,
                ForumPostSyncMessage.delete(postId));

        // 原子减少板块帖子数
        ForumSection section = sectionMapper.selectById(post.getSectionId());
        if (section != null && section.getPostCount() > 0) {
            sectionMapper.incPostCount(section.getId(), -1);
            redisTemplate.delete(SECTION_CACHE_KEY);
        }
    }

    @Override
    @Transactional
    public boolean toggleFavorite(Long userId, Long postId) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            throw BusinessException.notFound("帖子不存在");
        }

        ForumPostFavorite existing = favoriteMapper.selectOne(postId, userId);
        if (existing != null) {
            favoriteMapper.delete(postId, userId);
            return false;
        } else {
            ForumPostFavorite favorite = new ForumPostFavorite();
            favorite.setPostId(postId);
            favorite.setUserId(userId);
            favoriteMapper.insert(favorite);
            return true;
        }
    }

    @Override
    public PageResponse<PostResponse> getUserFavorites(Long userId, int page, int size) {
        validatePage(page);
        int offset = (page - 1) * size;
        List<ForumPost> posts = favoriteMapper.selectUserFavorites(userId, offset, size);
        Long total = favoriteMapper.countUserFavorites(userId);

        Set<Long> favoritedPostIds = posts.stream()
                .map(ForumPost::getId)
                .collect(Collectors.toSet());

        List<Long> postIds = posts.stream().map(ForumPost::getId).toList();
        Map<Long, Boolean> likedMap = redisLikeService.hasLikedPostsBatch(postIds, userId);

        List<PostResponse> list = posts.stream()
                .map(p -> toPostResponse(p, userId, favoritedPostIds, likedMap))
                .toList();

        return PageResponse.of(list, total, page, size);
    }

    @Override
    public PageResponse<PostResponse> getUserPosts(Long userId, int page, int size) {
        validatePage(page);
        int offset = (page - 1) * size;
        List<ForumPost> posts = postMapper.selectByUserId(userId, offset, size);
        Long total = postMapper.countByUserId(userId);

        return buildPostPageResponse(posts, total, page, size, userId);
    }

    @Override
    public PageResponse<PostResponse> searchPosts(Long userId, String keyword, Long sectionId, String sortBy, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw BusinessException.badRequest("搜索关键词不能为空");
        }
        validatePage(page);

        // 构建 ES 查询
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> {
                            b.must(m -> m.multiMatch(mm -> mm
                                    .fields("title^3", "content")
                                    .query(keyword.trim())
                            ));
                            if (sectionId != null) {
                                b.filter(f -> f.term(t -> t
                                        .field("sectionId").value(sectionId)));
                            }
                            return b;
                        })
                )
                .withSort(s -> s
                        .field(f -> f.field("isSticky").order(SortOrder.Desc))
                )
                .withSort(s -> s
                        .score(sc -> sc.order(SortOrder.Desc))
                )
                .withSort(s -> {
                    if ("hot".equals(sortBy)) {
                        s.field(f -> f.field("likeCount").order(SortOrder.Desc));
                        s.field(f -> f.field("commentCount").order(SortOrder.Desc));
                    }
                    s.field(f -> f.field("createTime").order(SortOrder.Desc));
                    return s;
                })
                .withPageable(PageRequest.of(page - 1, size))
                .build();

        SearchHits<ForumPostDocument> searchHits = esOperations.search(query, ForumPostDocument.class);

        List<Long> postIds = searchHits.stream()
                .map(h -> h.getContent().getId())
                .toList();

        if (postIds.isEmpty()) {
            return PageResponse.of(List.of(), searchHits.getTotalHits(), page, size);
        }

        // 批量查询点赞和收藏状态
        Map<Long, Boolean> likedMap = userId != null
                ? redisLikeService.hasLikedPostsBatch(postIds, userId)
                : Map.of();

        Set<Long> favoritedPostIds = userId != null
                ? new HashSet<>(favoriteMapper.selectFavoritedPostIds(userId, postIds))
                : Set.of();

        List<PostResponse> list = searchHits.stream()
                .map(SearchHit::getContent)
                .map(doc -> toPostResponseFromDoc(doc, userId, favoritedPostIds, likedMap))
                .toList();

        return PageResponse.of(list, searchHits.getTotalHits(), page, size);
    }

    @Override
    public PageResponse<CommentResponse> getComments(Long userId, Long postId, int page, int size) {
        validatePage(page);
        int offset = (page - 1) * size;
        List<ForumComment> comments = commentMapper.selectRootComments(postId, offset, size);
        Long total = commentMapper.countRootComments(postId);

        if (comments.isEmpty()) {
            return PageResponse.of(List.of(), total, page, size);
        }

        List<Long> rootCommentIds = comments.stream().map(ForumComment::getId).toList();
        List<ForumComment> allReplies = commentMapper.selectRepliesByParentIds(rootCommentIds);

        Map<Long, List<ForumComment>> repliesMap = allReplies.stream()
                .collect(Collectors.groupingBy(ForumComment::getParentId));

        // 收集所有评论ID（含子评论），批量查询点赞状态
        List<Long> allCommentIds = new ArrayList<>();
        allCommentIds.addAll(rootCommentIds);
        for (ForumComment reply : allReplies) {
            allCommentIds.add(reply.getId());
        }
        Map<Long, Boolean> commentLikedMap = userId != null
                ? redisLikeService.hasLikedCommentsBatch(allCommentIds, userId)
                : Map.of();

        List<CommentResponse> list = comments.stream()
                .map(c -> toCommentResponseWithReplies(c, userId, repliesMap, commentLikedMap))
                .toList();

        return PageResponse.of(list, total, page, size);
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        ForumComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BusinessException.notFound("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw BusinessException.forbidden("无权限删除此评论");
        }

        commentMapper.updateStatus(commentId, 0);

        // 原子减少帖子评论数
        ForumPost post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getCommentCount() > 0) {
            postMapper.incCommentCount(post.getId(), -1);
        }
    }

    @Override
    @Transactional
    public boolean toggleCommentLike(Long userId, Long commentId) {
        ForumComment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw BusinessException.notFound("评论不存在");
        }

        if (redisLikeService.hasLikedComment(commentId, userId)) {
            redisLikeService.unlikeComment(commentId, userId);
            commentMapper.incLikeCount(commentId, -1);
            return false;
        } else {
            redisLikeService.likeComment(commentId, userId);
            commentMapper.incLikeCount(commentId, 1);
            return true;
        }
    }

    // ========== 浏览数定时同步 ==========

    /**
     * 每5分钟将Redis中的浏览数增量批量同步到数据库
     */
    @Scheduled(fixedRate = 300000)
    public void syncViewCounts() {
        Map<Long, Long> deltas = redisLikeService.drainViewCounts();
        if (deltas.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, Long> entry : deltas.entrySet()) {
            try {
                postMapper.incViewCount(entry.getKey(), entry.getValue().intValue());
            } catch (Exception e) {
                // 单条失败不影响其他同步
            }
        }
    }

    // ========== 内部工具方法 ==========

    /**
     * 构建帖子分页响应 — 统一处理批量Redis查询和收藏状态
     */
    private PageResponse<PostResponse> buildPostPageResponse(List<ForumPost> posts, Long total, int page, int size, Long userId) {
        if (posts.isEmpty()) {
            return PageResponse.of(List.of(), total, page, size);
        }

        List<Long> postIds = posts.stream().map(ForumPost::getId).toList();

        // 批量查询点赞状态（Pipeline，1次网络往返）
        Map<Long, Boolean> likedMap = userId != null
                ? redisLikeService.hasLikedPostsBatch(postIds, userId)
                : Map.of();

        // 定向查询收藏状态（只查当前页帖子，NOT全量用户收藏）
        Set<Long> favoritedPostIds = userId != null
                ? new HashSet<>(favoriteMapper.selectFavoritedPostIds(userId, postIds))
                : Set.of();

        List<PostResponse> list = posts.stream()
                .map(p -> toPostResponse(p, userId, favoritedPostIds, likedMap))
                .toList();

        return PageResponse.of(list, total, page, size);
    }

    /**
     * 验证页码，防止深分页
     */
    private void validatePage(int page) {
        if (page > MAX_PAGE) {
            throw BusinessException.badRequest("页面深度过大，最多支持第" + MAX_PAGE + "页");
        }
    }

    // ========== 转换方法 ==========

    private PostResponse toPostResponse(ForumPost post, Long userId, Set<Long> favoritedPostIds, Map<Long, Boolean> likedMap) {
        PostResponse resp = new PostResponse();
        resp.setId(post.getId());
        resp.setUserId(post.getUserId());
        resp.setAuthorUsername(post.getAuthorUsername());
        resp.setAuthorAvatar(post.getAuthorAvatar());
        resp.setSectionId(post.getSectionId());
        resp.setSectionName(post.getSectionName());
        resp.setTitle(post.getTitle());
        resp.setContent(post.getContent());
        resp.setImages(post.getImages() != null ?
                Arrays.asList(post.getImages().split(",")) : new ArrayList<>());
        resp.setLikeCount(post.getLikeCount());
        resp.setCommentCount(post.getCommentCount());
        resp.setViewCount(post.getViewCount());
        resp.setCreateTime(post.getCreateTime());
        resp.setUpdateTime(post.getUpdateTime());

        if (userId != null) {
            resp.setIsLiked(likedMap.getOrDefault(post.getId(), false));
            resp.setIsFavorited(favoritedPostIds.contains(post.getId()));
        } else {
            resp.setIsLiked(false);
            resp.setIsFavorited(false);
        }

        return resp;
    }

    private PostResponse toPostResponseFromDoc(ForumPostDocument doc, Long userId,
                                               Set<Long> favoritedPostIds, Map<Long, Boolean> likedMap) {
        PostResponse resp = new PostResponse();
        resp.setId(doc.getId());
        resp.setUserId(doc.getUserId());
        resp.setAuthorUsername(doc.getAuthorUsername());
        resp.setAuthorAvatar(doc.getAuthorAvatar());
        resp.setSectionId(doc.getSectionId());
        resp.setSectionName(doc.getSectionName());
        resp.setTitle(doc.getTitle());
        resp.setContent(doc.getContent());
        resp.setImages(new ArrayList<>());
        resp.setLikeCount(doc.getLikeCount());
        resp.setCommentCount(doc.getCommentCount());
        resp.setViewCount(doc.getViewCount());
        resp.setCreateTime(doc.getCreateTime());
        resp.setUpdateTime(doc.getCreateTime());

        if (userId != null) {
            resp.setIsLiked(likedMap.getOrDefault(doc.getId(), false));
            resp.setIsFavorited(favoritedPostIds.contains(doc.getId()));
        } else {
            resp.setIsLiked(false);
            resp.setIsFavorited(false);
        }

        return resp;
    }

    private CommentResponse toCommentResponseWithReplies(ForumComment comment, Long currentUserId,
            Map<Long, List<ForumComment>> repliesMap, Map<Long, Boolean> likedMap) {
        CommentResponse resp = new CommentResponse();
        resp.setId(comment.getId());
        resp.setPostId(comment.getPostId());
        resp.setUserId(comment.getUserId());
        resp.setAuthorUsername(comment.getAuthorUsername());
        resp.setAuthorAvatar(comment.getAuthorAvatar());
        resp.setParentId(comment.getParentId());
        resp.setContent(comment.getContent());
        resp.setLikeCount(comment.getLikeCount());
        resp.setIsLiked(currentUserId != null && likedMap.getOrDefault(comment.getId(), false));
        resp.setCreateTime(comment.getCreateTime());

        List<ForumComment> replies = repliesMap.get(comment.getId());
        if (replies != null && !replies.isEmpty()) {
            resp.setReplies(replies.stream()
                    .map(r -> toCommentResponseWithReplies(r, currentUserId, repliesMap, likedMap))
                    .toList());
        }

        return resp;
    }

    public void fullImportToES() {
        List<ForumPost> allPosts = postMapper.selectAllNormal();
        if (allPosts.isEmpty()) {
            return;
        }

        List<ForumPostDocument> docs = allPosts.stream()
                .map(this::toDocument)
                .toList();

        esRepository.saveAll(docs);
    }

    private ForumPostDocument toDocument(ForumPost post) {
        return ForumPostDocument.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .sectionId(post.getSectionId())
                .title(post.getTitle())
                .content(post.getContent())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .isSticky(post.getIsSticky())
                .authorUsername(post.getAuthorUsername())
                .authorAvatar(post.getAuthorAvatar())
                .sectionName(post.getSectionName())
                .createTime(post.getCreateTime())
                .build();
    }
}
