-- Idempotent demo content for users, preferences, diaries and comments.
-- Run after schema migration and after destination/food base data exist.

USE `tour_system`;

SET @demo_destination_name = '北京邮电大学沙河校区';
SET @demo_password_hash = '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiRnZefHRBtDDAhPQZ8Jp8oD5NTVy6K';

DELIMITER //

DROP PROCEDURE IF EXISTS assert_demo_content_prerequisites//

CREATE PROCEDURE assert_demo_content_prerequisites()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM `destination` WHERE `name` = @demo_destination_name
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Demo destination is missing; import destination data before init-demo-content.sql';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM `food` food
    JOIN `destination` destination ON destination.`id` = food.`destination_id`
    WHERE destination.`name` = @demo_destination_name
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Demo food is missing; import food data before init-demo-content.sql';
  END IF;
END//

CALL assert_demo_content_prerequisites()//
DROP PROCEDURE assert_demo_content_prerequisites//

DELIMITER ;

INSERT INTO `user` (`username`, `password_hash`, `nickname`, `avatar_url`, `role`, `status`)
SELECT 'demo_user_01', @demo_password_hash, '演示用户一', '/files/avatar/demo-user-01.png', 'user', 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'demo_user_01');

INSERT INTO `user` (`username`, `password_hash`, `nickname`, `avatar_url`, `role`, `status`)
SELECT 'demo_user_02', @demo_password_hash, '演示用户二', '/files/avatar/demo-user-02.png', 'user', 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'demo_user_02');

INSERT INTO `user` (`username`, `password_hash`, `nickname`, `avatar_url`, `role`, `status`)
SELECT 'demo_user_03', @demo_password_hash, '演示用户三', '/files/avatar/demo-user-03.png', 'user', 1
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'demo_user_03');

SET @user_01_id = (SELECT `id` FROM `user` WHERE `username` = 'demo_user_01');
SET @user_02_id = (SELECT `id` FROM `user` WHERE `username` = 'demo_user_02');
SET @user_03_id = (SELECT `id` FROM `user` WHERE `username` = 'demo_user_03');
SET @destination_id = (SELECT `id` FROM `destination` WHERE `name` = @demo_destination_name ORDER BY `id` LIMIT 1);
SET @food_id = (SELECT `id` FROM `food` WHERE `destination_id` = @destination_id ORDER BY `rating_score` DESC, `id` ASC LIMIT 1);

INSERT INTO `user_preference` (
  `user_id`, `prefer_hot_level`, `prefer_theme`, `prefer_food_type`,
  `prefer_crowd_level`, `travel_style`, `custom_preference_text`
) VALUES
  (@user_01_id, 80, '["校园","图书馆","美食"]', '面食', 40, 'relaxed', '希望路线不要太绕，优先看校园建筑和食堂。'),
  (@user_02_id, 60, '["运动","打卡","夜景"]', '快餐', 60, 'checkin', '喜欢适合拍照的点位和热门路线。'),
  (@user_03_id, 50, '["学习","安静","咖啡"]', '甜品', 30, 'quiet', '偏好安静的学习空间和咖啡店。')
ON DUPLICATE KEY UPDATE
  `prefer_hot_level` = VALUES(`prefer_hot_level`),
  `prefer_theme` = VALUES(`prefer_theme`),
  `prefer_food_type` = VALUES(`prefer_food_type`),
  `prefer_crowd_level` = VALUES(`prefer_crowd_level`),
  `travel_style` = VALUES(`travel_style`),
  `custom_preference_text` = VALUES(`custom_preference_text`);

INSERT INTO `diary` (
  `user_id`, `destination_id`, `title`, `content_text`, `content_compressed`,
  `heat_score`, `rating_score`, `visibility`, `status`
)
SELECT @user_01_id, @destination_id, '沙河校园一日路线记录',
       '从校门进入后先到图书馆，再沿主路去教学楼和食堂。路线节点清晰，适合展示最短路径和周边设施查询。',
       NULL, 86, 4.70, 'public', 1
WHERE @destination_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `diary`
    WHERE `user_id` = @user_01_id AND `destination_id` = @destination_id AND `title` = '沙河校园一日路线记录'
  );

INSERT INTO `diary` (
  `user_id`, `destination_id`, `title`, `content_text`, `content_compressed`,
  `heat_score`, `rating_score`, `visibility`, `status`
)
SELECT @user_02_id, @destination_id, '图书馆和食堂打卡',
       '上午在图书馆学习，午饭去了食堂窗口。美食推荐里评分靠前的窗口确实更适合演示。',
       NULL, 72, 4.50, 'public', 1
WHERE @destination_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `diary`
    WHERE `user_id` = @user_02_id AND `destination_id` = @destination_id AND `title` = '图书馆和食堂打卡'
  );

INSERT INTO `diary` (
  `user_id`, `destination_id`, `title`, `content_text`, `content_compressed`,
  `heat_score`, `rating_score`, `visibility`, `status`
)
SELECT @user_03_id, @destination_id, '安静路线和咖啡点位',
       '这次选择了人少的路线，途中查询了附近咖啡和休息设施。可达距离排序比直线距离更符合真实体验。',
       NULL, 68, 4.30, 'public', 1
WHERE @destination_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `diary`
    WHERE `user_id` = @user_03_id AND `destination_id` = @destination_id AND `title` = '安静路线和咖啡点位'
  );

SET @diary_01_id = (
  SELECT `id` FROM `diary`
  WHERE `user_id` = @user_01_id AND `destination_id` = @destination_id AND `title` = '沙河校园一日路线记录'
  ORDER BY `id` LIMIT 1
);
SET @diary_02_id = (
  SELECT `id` FROM `diary`
  WHERE `user_id` = @user_02_id AND `destination_id` = @destination_id AND `title` = '图书馆和食堂打卡'
  ORDER BY `id` LIMIT 1
);

INSERT INTO `destination_comment` (`destination_id`, `user_id`, `content_text`, `like_count`, `status`)
SELECT @destination_id, @user_01_id, '校园道路节点比较清楚，适合做路线规划演示。', 3, 1
WHERE @destination_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `destination_comment`
    WHERE `destination_id` = @destination_id AND `user_id` = @user_01_id AND `content_text` = '校园道路节点比较清楚，适合做路线规划演示。'
  );

SET @destination_parent_comment_id = (
  SELECT `id` FROM `destination_comment`
  WHERE `destination_id` = @destination_id AND `user_id` = @user_01_id AND `content_text` = '校园道路节点比较清楚，适合做路线规划演示。'
  ORDER BY `id` LIMIT 1
);

INSERT INTO `destination_comment` (`destination_id`, `user_id`, `parent_comment_id`, `content_text`, `like_count`, `status`)
SELECT @destination_id, @user_02_id, @destination_parent_comment_id, '我也觉得图书馆到食堂这段最适合展示。', 1, 1
WHERE @destination_parent_comment_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `destination_comment`
    WHERE `parent_comment_id` = @destination_parent_comment_id AND `user_id` = @user_02_id AND `content_text` = '我也觉得图书馆到食堂这段最适合展示。'
  );

INSERT INTO `food_comment` (`food_id`, `user_id`, `content_text`, `like_count`, `status`)
SELECT @food_id, @user_02_id, '这个窗口适合作为美食推荐的高评分样例。', 2, 1
WHERE @food_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `food_comment`
    WHERE `food_id` = @food_id AND `user_id` = @user_02_id AND `content_text` = '这个窗口适合作为美食推荐的高评分样例。'
  );

SET @food_parent_comment_id = (
  SELECT `id` FROM `food_comment`
  WHERE `food_id` = @food_id AND `user_id` = @user_02_id AND `content_text` = '这个窗口适合作为美食推荐的高评分样例。'
  ORDER BY `id` LIMIT 1
);

INSERT INTO `food_comment` (`food_id`, `user_id`, `parent_comment_id`, `content_text`, `like_count`, `status`)
SELECT @food_id, @user_03_id, @food_parent_comment_id, '价格也适中，适合演示 avgPrice 字段。', 1, 1
WHERE @food_parent_comment_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `food_comment`
    WHERE `parent_comment_id` = @food_parent_comment_id AND `user_id` = @user_03_id AND `content_text` = '价格也适中，适合演示 avgPrice 字段。'
  );

INSERT INTO `diary_comment` (`diary_id`, `user_id`, `content_text`, `like_count`, `status`)
SELECT @diary_01_id, @user_02_id, '这篇日记能串起推荐、路线和设施查询。', 4, 1
WHERE @diary_01_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `diary_comment`
    WHERE `diary_id` = @diary_01_id AND `user_id` = @user_02_id AND `content_text` = '这篇日记能串起推荐、路线和设施查询。'
  );

SET @diary_parent_comment_id = (
  SELECT `id` FROM `diary_comment`
  WHERE `diary_id` = @diary_01_id AND `user_id` = @user_02_id AND `content_text` = '这篇日记能串起推荐、路线和设施查询。'
  ORDER BY `id` LIMIT 1
);

INSERT INTO `diary_comment` (`diary_id`, `user_id`, `parent_comment_id`, `content_text`, `like_count`, `status`)
SELECT @diary_01_id, @user_01_id, @diary_parent_comment_id, '后续可以补一张路线截图。', 1, 1
WHERE @diary_parent_comment_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `diary_comment`
    WHERE `parent_comment_id` = @diary_parent_comment_id AND `user_id` = @user_01_id AND `content_text` = '后续可以补一张路线截图。'
  );

INSERT INTO `diary_comment` (`diary_id`, `user_id`, `content_text`, `like_count`, `status`)
SELECT @diary_02_id, @user_03_id, '标题检索和正文检索都可以命中这类校园关键词。', 2, 1
WHERE @diary_02_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `diary_comment`
    WHERE `diary_id` = @diary_02_id AND `user_id` = @user_03_id AND `content_text` = '标题检索和正文检索都可以命中这类校园关键词。'
  );

UPDATE `destination_comment` parent
LEFT JOIN (
  SELECT `parent_comment_id`, COUNT(*) AS reply_count
  FROM `destination_comment`
  WHERE `parent_comment_id` IS NOT NULL AND `status` = 1
  GROUP BY `parent_comment_id`
) child_count ON child_count.`parent_comment_id` = parent.`id`
SET parent.`reply_count` = COALESCE(child_count.reply_count, 0)
WHERE parent.`parent_comment_id` IS NULL;

UPDATE `food_comment` parent
LEFT JOIN (
  SELECT `parent_comment_id`, COUNT(*) AS reply_count
  FROM `food_comment`
  WHERE `parent_comment_id` IS NOT NULL AND `status` = 1
  GROUP BY `parent_comment_id`
) child_count ON child_count.`parent_comment_id` = parent.`id`
SET parent.`reply_count` = COALESCE(child_count.reply_count, 0)
WHERE parent.`parent_comment_id` IS NULL;

UPDATE `diary_comment` parent
LEFT JOIN (
  SELECT `parent_comment_id`, COUNT(*) AS reply_count
  FROM `diary_comment`
  WHERE `parent_comment_id` IS NOT NULL AND `status` = 1
  GROUP BY `parent_comment_id`
) child_count ON child_count.`parent_comment_id` = parent.`id`
SET parent.`reply_count` = COALESCE(child_count.reply_count, 0)
WHERE parent.`parent_comment_id` IS NULL;
