package com.trip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trip.entity.Diary;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiaryMapper extends BaseMapper<Diary> {
}
