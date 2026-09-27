package com.moxi.content.service;

import com.moxi.content.domain.ContentVersion;
import com.moxi.content.dto.ContentDetailResponse;
import com.moxi.content.dto.ContentHistoryResponse;
import com.moxi.content.dto.CreateContentRequest;
import com.moxi.content.dto.UpdateContentRequest;

import java.util.List;

/**
 * 内容服务接口，提供内容的 CRUD、发布及版本查询能力。
 */
public interface ContentService {

    /**
     * 分页查询指定用户的内容历史，按创建时间倒序。
     *
     * @param userId 用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 分页历史响应
     */
    ContentHistoryResponse getHistory(Long userId, int page, int size);

    /**
     * 查询内容详情，需校验内容归属。
     *
     * @param userId    用户 ID
     * @param contentId 内容 ID
     * @return 内容详情响应
     */
    ContentDetailResponse getDetail(Long userId, Long contentId);

    /**
     * 创建新内容，初始状态为 draft，并保存版本快照。
     *
     * @param userId 用户 ID
     * @param req    创建请求
     * @return 内容详情响应
     */
    ContentDetailResponse create(Long userId, CreateContentRequest req);

    /**
     * 更新内容，并保存自增版本号的版本快照。
     *
     * @param userId    用户 ID
     * @param contentId 内容 ID
     * @param req       更新请求
     * @return 内容详情响应
     */
    ContentDetailResponse update(Long userId, Long contentId, UpdateContentRequest req);

    /**
     * 删除内容及其所有版本，需校验内容归属。
     *
     * @param userId    用户 ID
     * @param contentId 内容 ID
     */
    void delete(Long userId, Long contentId);

    /**
     * 发布内容，将状态置为 published，需校验内容归属。
     *
     * @param userId    用户 ID
     * @param contentId 内容 ID
     */
    void publish(Long userId, Long contentId);

    /**
     * 查询内容的所有版本快照，按版本号倒序，需校验内容归属。
     *
     * @param userId    用户 ID
     * @param contentId 内容 ID
     * @return 版本列表
     */
    List<ContentVersion> getVersions(Long userId, Long contentId);
}
