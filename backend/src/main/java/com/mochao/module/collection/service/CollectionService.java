package com.mochao.module.collection.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.module.collection.dto.CollectionCreateDTO;
import com.mochao.module.collection.dto.CollectionQueryDTO;
import com.mochao.module.collection.dto.CollectionUpdateDTO;
import com.mochao.module.collection.entity.Collection;
import com.mochao.module.collection.entity.CollectionTag;

import java.util.List;
import java.util.Map;

public interface CollectionService {

    Collection createCollection(CollectionCreateDTO dto, Long userId);

    Page<Collection> getCollectionList(CollectionQueryDTO dto, Long userId);

    Collection getCollectionById(Long id, Long userId);

    Collection updateCollection(Long id, CollectionUpdateDTO dto, Long userId);

    void deleteCollection(Long id, Long userId);

    List<CollectionTag> getUserTags(Long userId);

    Collection getDailyCollection(Long userId);

    String exportCollections(Long userId, String format);

    Map<String, Object> getCollectionStats(Long userId);
}
