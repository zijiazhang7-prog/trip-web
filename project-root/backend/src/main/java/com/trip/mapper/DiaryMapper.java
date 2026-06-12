package com.trip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trip.entity.Diary;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.type.JdbcType;

@Mapper
public interface DiaryMapper extends BaseMapper<Diary> {

    @Update("""
            UPDATE diary
            SET heat_score = heat_score + 1,
                updated_at = updated_at
            WHERE id = #{diaryId}
              AND status = 1
            """)
    int incrementHeatScore(@Param("diaryId") Long diaryId);

    @Select("""
            SELECT id, user_id, destination_id, route_history_id, title, content_text,
                   heat_score, rating_score, rating_count, visibility, status, created_at, updated_at
            FROM diary
            WHERE id = #{id}
            FOR UPDATE
            """)
    Diary selectByIdForUpdate(@Param("id") Long id);

    @Update("""
            UPDATE diary
            SET rating_score = (
                    SELECT COALESCE(ROUND(AVG(score), 2), 0)
                    FROM diary_rating
                    WHERE diary_id = #{diaryId}
                ),
                rating_count = (
                    SELECT COUNT(*)
                    FROM diary_rating
                    WHERE diary_id = #{diaryId}
                )
            WHERE id = #{diaryId}
            """)
    int recalculateRating(@Param("diaryId") Long diaryId);

    @Select("""
            <script>
            SELECT id, content_text, content_compressed
            FROM diary
            WHERE id &gt; #{lastId}
              AND content_text IS NOT NULL
              AND content_text != ''
            <if test="includeExisting == false">
              AND (content_compressed IS NULL OR OCTET_LENGTH(content_compressed) = 0)
            </if>
            ORDER BY id ASC
            LIMIT #{batchSize}
            </script>
            """)
    @Results(id = "compressionMaintenanceResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "content_text", property = "contentText"),
            @Result(
                    column = "content_compressed",
                    property = "contentCompressed",
                    jdbcType = JdbcType.LONGVARBINARY)
    })
    List<Diary> selectCompressionBatchAfterId(
            @Param("lastId") long lastId,
            @Param("batchSize") int batchSize,
            @Param("includeExisting") boolean includeExisting);

    @Update("""
            UPDATE diary
            SET content_compressed = #{compressedData}
            WHERE id = #{id}
              AND content_text = #{contentText}
              AND content_compressed IS NULL
            """)
    int updateCompressedIfMissing(
            @Param("id") Long id,
            @Param("contentText") String contentText,
            @Param("compressedData") byte[] compressedData);

    @Update("""
            UPDATE diary
            SET content_compressed = #{compressedData}
            WHERE id = #{id}
              AND content_text = #{contentText}
            """)
    int updateCompressedIfContentUnchanged(
            @Param("id") Long id,
            @Param("contentText") String contentText,
            @Param("compressedData") byte[] compressedData);
}
