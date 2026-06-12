package com.trip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trip.entity.DiaryAnimation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DiaryAnimationMapper extends BaseMapper<DiaryAnimation> {

    @Insert("""
            INSERT INTO diary_animation (
                diary_id, provider, animation_title, narration_text, script_json, status
            ) VALUES (
                #{animation.diaryId},
                #{animation.provider},
                #{animation.animationTitle},
                #{animation.narrationText},
                #{animation.scriptJson},
                #{animation.status}
            )
            ON DUPLICATE KEY UPDATE
                provider = VALUES(provider),
                animation_title = VALUES(animation_title),
                narration_text = VALUES(narration_text),
                script_json = VALUES(script_json),
                status = VALUES(status),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsertByDiaryId(@Param("animation") DiaryAnimation animation);
}
