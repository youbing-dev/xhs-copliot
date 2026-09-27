package com.moxi.content.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxi.common.exception.BusinessException;
import com.moxi.common.response.ErrorCode;
import com.moxi.content.domain.Content;
import com.moxi.content.domain.ContentVersion;
import com.moxi.content.dto.ContentDetailResponse;
import com.moxi.content.dto.ContentHistoryResponse;
import com.moxi.content.dto.CreateContentRequest;
import com.moxi.content.dto.UpdateContentRequest;
import com.moxi.content.repository.ContentMapper;
import com.moxi.content.repository.ContentVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 内容服务实现。
 *
 * <p>cover_images 字段在数据库中为 JSON 类型，实体层统一以 String 存储，
 * 在本服务中通过 Hutool 的 {@link JSONUtil} 完成 List&lt;String&gt; 与字符串之间的转换。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PUBLISHED = "published";

    private final ContentMapper contentMapper;
    private final ContentVersionMapper contentVersionMapper;

    @Override
    public ContentHistoryResponse getHistory(Long userId, int page, int size) {
        Page<Content> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Content::getUserId, userId)
                .orderByDesc(Content::getCreatedAt);
        Page<Content> result = contentMapper.selectPage(pageParam, wrapper);

        ContentHistoryResponse response = new ContentHistoryResponse();
        response.setTotal(result.getTotal());
        response.setPage(page);
        response.setSize(size);

        List<ContentHistoryResponse.ContentItem> items = new ArrayList<>();
        for (Content content : result.getRecords()) {
            ContentHistoryResponse.ContentItem item = new ContentHistoryResponse.ContentItem();
            item.setId(content.getId());
            item.setTitle(content.getTitle());
            item.setStatus(content.getStatus());
            item.setCreatedAt(content.getCreatedAt());
            items.add(item);
        }
        response.setItems(items);
        return response;
    }

    @Override
    public ContentDetailResponse getDetail(Long userId, Long contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null || !content.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }
        return toDetailResponse(content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentDetailResponse create(Long userId, CreateContentRequest req) {
        Content content = new Content();
        content.setUserId(userId);
        content.setTitle(req.getTitle());
        content.setBody(req.getBody());
        content.setTags(req.getTags());
        content.setCoverImages(toJson(req.getCoverImages()));
        content.setStatus(STATUS_DRAFT);
        contentMapper.insert(content);

        saveVersion(content, 1);
        log.info("Created content id={} for userId={}", content.getId(), userId);
        return toDetailResponse(content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentDetailResponse update(Long userId, Long contentId, UpdateContentRequest req) {
        Content content = contentMapper.selectById(contentId);
        if (content == null || !content.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }

        content.setTitle(req.getTitle());
        content.setBody(req.getBody());
        content.setTags(req.getTags());
        content.setCoverImages(toJson(req.getCoverImages()));
        if (req.getStatus() != null) {
            content.setStatus(req.getStatus());
        }
        contentMapper.updateById(content);

        int nextVersionNum = getNextVersionNum(contentId);
        saveVersion(content, nextVersionNum);
        log.info("Updated content id={} for userId={}, version={}", contentId, userId, nextVersionNum);
        return toDetailResponse(content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null || !content.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }

        contentMapper.deleteById(contentId);

        LambdaQueryWrapper<ContentVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(ContentVersion::getContentId, contentId);
        contentVersionMapper.delete(versionWrapper);

        log.info("Deleted content id={} and its versions for userId={}", contentId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long userId, Long contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null || !content.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }

        content.setStatus(STATUS_PUBLISHED);
        contentMapper.updateById(content);
        log.info("Published content id={} for userId={}", contentId, userId);
    }

    @Override
    public List<ContentVersion> getVersions(Long userId, Long contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null || !content.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }

        LambdaQueryWrapper<ContentVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContentVersion::getContentId, contentId)
                .orderByDesc(ContentVersion::getVersionNum);
        return contentVersionMapper.selectList(wrapper);
    }

    // ------------------------------------------------------------------
    // 私有辅助方法
    // ------------------------------------------------------------------

    /**
     * 将 List&lt;String&gt; 转为 JSON 字符串用于存储，空列表返回 "[]"。
     */
    private String toJson(List<String> coverImages) {
        if (coverImages == null || coverImages.isEmpty()) {
            return "[]";
        }
        return JSONUtil.toJsonStr(coverImages);
    }

    /**
     * 将存储的 JSON 字符串解析回 List&lt;String&gt;，空值返回空列表。
     */
    private List<String> parseCoverImages(String coverImages) {
        if (coverImages == null || coverImages.isEmpty()) {
            return new ArrayList<>();
        }
        return JSONUtil.toList(coverImages, String.class);
    }

    /**
     * 保存一条内容版本快照。
     */
    private void saveVersion(Content content, int versionNum) {
        ContentVersion version = new ContentVersion();
        version.setContentId(content.getId());
        version.setVersionNum(versionNum);
        version.setTitle(content.getTitle());
        version.setBody(content.getBody());
        version.setTags(content.getTags());
        version.setCoverImages(content.getCoverImages());
        contentVersionMapper.insert(version);
    }

    /**
     * 查询指定内容当前的最新版本号，并返回下一个版本号。
     */
    private int getNextVersionNum(Long contentId) {
        LambdaQueryWrapper<ContentVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContentVersion::getContentId, contentId)
                .orderByDesc(ContentVersion::getVersionNum)
                .last("LIMIT 1");
        ContentVersion latest = contentVersionMapper.selectOne(wrapper);
        return latest == null ? 1 : latest.getVersionNum() + 1;
    }

    /**
     * 将内容实体转换为详情响应 DTO，同时解析封面图 JSON。
     */
    private ContentDetailResponse toDetailResponse(Content content) {
        ContentDetailResponse resp = new ContentDetailResponse();
        resp.setId(content.getId());
        resp.setUserId(content.getUserId());
        resp.setTitle(content.getTitle());
        resp.setBody(content.getBody());
        resp.setTags(content.getTags());
        resp.setCoverImages(parseCoverImages(content.getCoverImages()));
        resp.setStatus(content.getStatus());
        resp.setCreatedAt(content.getCreatedAt());
        resp.setUpdatedAt(content.getUpdatedAt());
        return resp;
    }
}
