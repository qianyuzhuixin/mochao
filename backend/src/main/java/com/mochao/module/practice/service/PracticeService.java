package com.mochao.module.practice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mochao.module.practice.dto.PracticeCompleteDTO;
import com.mochao.module.practice.dto.PracticeProgressDTO;
import com.mochao.module.practice.dto.PracticeStartDTO;
import com.mochao.module.practice.entity.PracticeSession;

public interface PracticeService {

    PracticeSession startPractice(PracticeStartDTO dto, Long userId);

    PracticeSession updateProgress(Long id, PracticeProgressDTO dto, Long userId);

    PracticeSession pausePractice(Long id, Long userId);

    PracticeSession resumePractice(Long id, Long userId);

    PracticeSession completePractice(Long id, PracticeCompleteDTO dto, Long userId);

    PracticeSession getActivePractice(Long userId);

    Page<PracticeSession> getPracticeHistory(Long userId, Integer page, Integer size);
}
