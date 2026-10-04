-- nslp 建筑施工与社会生活噪声污染防治监管 -- schema (chen-027)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_027

CREATE TABLE IF NOT EXISTS t_nslp_case_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '噪声投诉与超标处置办件的件次',
  stage int DEFAULT NULL COMMENT '当前关次 0..3（受件/核查/责令/复查收尾）',
  status int DEFAULT NULL COMMENT '单子走到哪一关 0未起 1在办 2已封住',
  content varchar(255) DEFAULT NULL COMMENT '一关一记',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次挪关动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='噪声投诉与超标处置办件流转单';

CREATE TABLE IF NOT EXISTS t_nslp_doc_tpl (
  id bigint NOT NULL COMMENT '主键',
  tpl_no varchar(64) DEFAULT NULL COMMENT '文书模板代号',
  tpl_name varchar(128) DEFAULT NULL COMMENT '文种名（超标告知·责令限期改正·复查结论）',
  ver_no int DEFAULT NULL COMMENT '版次（数值大的先说话）',
  var_num int DEFAULT NULL COMMENT '本版应填项数',
  fill_num int DEFAULT NULL COMMENT '本行已取到值的项数',
  miss_num int DEFAULT NULL COMMENT '还差几项（轧出来，不手填）',
  eff_start datetime DEFAULT NULL COMMENT '起用那一一日',
  eff_end datetime DEFAULT NULL COMMENT '交班那一一日(不含)',
  status int DEFAULT NULL COMMENT '版次情形 0在位 1已撤版',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='执法文书模板与版次';

CREATE TABLE IF NOT EXISTS t_nslp_flow_batch (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '批次码（一拨一个，重码到达只算照面，不另起一拨）',
  cap_num int DEFAULT NULL COMMENT '拨抬头写定的顶（空着/负数/离谱大数按300认；顶只此一处，别无第二来源）',
  first_count int DEFAULT NULL COMMENT '进门首遍点出来的行数（事后翻得出当初点了几行）',
  arrive_seq int DEFAULT NULL COMMENT '该批次码落到流水上第几遍 1首遍 2照面',
  cut_kind varchar(16) DEFAULT NULL COMMENT '本报文切法 delimiter按分隔符 fixed按定宽（一拨之内只许一种）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_nslp_flow_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监测流水核收拨抬头（一拨一抬头）';

CREATE TABLE IF NOT EXISTS t_nslp_flow_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '监测流水批次码',
  row_no int DEFAULT NULL COMMENT '本拨内行次（系统在本拨内排，不留手填格；重报仍显原行次）',
  arrive_seq int DEFAULT NULL COMMENT '本行随该批次码第几遍落地 1首遍 2照面里的新行',
  item_code varchar(64) DEFAULT NULL COMMENT '被对上的点位码（只认码，题名凑数不认）',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行读数次数（与达标次数一起到，缺则空挂不代填不当0）',
  chk_num decimal(12,2) DEFAULT NULL COMMENT '本行达标次数（与读数次数一起到）',
  miss_data int DEFAULT '0' COMMENT '两数没一起到 0齐 1缺（缺不代填、不当0收下）',
  payload varchar(1000) DEFAULT NULL COMMENT '报文里那串读数原文',
  parse_fail int DEFAULT '0' COMMENT '报文切不动标记 0切得动 1切不动（单行标出递给回核，不按整拨）',
  put_kind varchar(32) DEFAULT NULL COMMENT '来路（自动站直采／监测车补报两条来路，各定各的去处）',
  raw_title varchar(128) DEFAULT NULL COMMENT '报文带出的点位题名（只拿来与底册比对）',
  raw_zone_no varchar(64) DEFAULT NULL COMMENT '报文带出的区册代号（只拿来与底册比对）',
  book_mismatch int DEFAULT '0' COMMENT '报文与点位底册不合 0合 1不合（以底册为准，回执单列）',
  status int DEFAULT NULL COMMENT '行落地情形 0未勾 1已收下 2已退回（第四种不许落库；新拨只落1/2，0只存量里有）',
  reject_code varchar(256) DEFAULT NULL COMMENT '退回吃了哪几条（码值逗号相连，一句句问得出）',
  reject_reason varchar(1000) DEFAULT NULL COMMENT '退回写明为哪一条退（多条句句相连）',
  layer int DEFAULT '0' COMMENT '册层 0现行名单 1历史册层（未勾满两年度转入，不删行）',
  layer_at datetime DEFAULT NULL COMMENT '转入历史册层那一一日',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  KEY idx_nslp_flow_row_batch (batch_no),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='监测流水核收行';

CREATE TABLE IF NOT EXISTS t_nslp_limit_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '功能区限值线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  obj_kind varchar(32) DEFAULT NULL COMMENT '这条线管昼段还是夜段',
  th1_max decimal(12,3) DEFAULT NULL COMMENT '起算限值',
  th2_max decimal(12,3) DEFAULT NULL COMMENT '够线限值',
  th3_max decimal(12,3) DEFAULT NULL COMMENT '封顶限值',
  eff_start datetime DEFAULT NULL COMMENT '起用那一一日',
  eff_end datetime DEFAULT NULL COMMENT '交班那一一日(不含)',
  priority int DEFAULT NULL COMMENT '交班次序(数值大的先说话)',
  status int DEFAULT NULL COMMENT '线的情形 0在位 1已撤下',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='功能区昼夜间限值线';

CREATE TABLE IF NOT EXISTS t_nslp_permit_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '夜间连续施工作业许可签批单',
  node_no int DEFAULT NULL COMMENT '当前所在关 0..2（窗口受理/区局核定/市局备案）',
  sign_mode int DEFAULT NULL COMMENT '本关算齐办法 0一名即过 1两名须同判',
  need_count int DEFAULT NULL COMMENT '本关应签人数',
  sign_count int DEFAULT NULL COMMENT '本关已到人数',
  status int DEFAULT NULL COMMENT '签批情形 0在签 1已点齐 2已压回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='夜间连续施工作业许可签批单';

CREATE TABLE IF NOT EXISTS t_nslp_point (
  id bigint NOT NULL COMMENT '主键',
  point_no varchar(64) DEFAULT NULL COMMENT '点位码',
  zone_id int DEFAULT NULL COMMENT '挂在哪一个区册名下',
  zone_no varchar(64) DEFAULT NULL COMMENT '区册代号（冗余自区册）',
  src_kind varchar(128) DEFAULT NULL COMMENT '来路（自动站／移动监测车／人工核查三条来路）',
  need_num int DEFAULT NULL COMMENT '核定应测次数',
  got_num int DEFAULT NULL COMMENT '已核验达标的次数',
  lack_num int DEFAULT NULL COMMENT '还差几回（轧出来，不手填）',
  content varchar(255) DEFAULT NULL COMMENT '随件交来的点位题名与依附声源',
  status int DEFAULT NULL COMMENT '册面情形 0新入册 1已核齐 2缺项',
  pinned_rule_code varchar(64) DEFAULT NULL COMMENT '归层钉住的限值线代号（不归层清空白，追得出当年照哪条线）',
  pinned_eff_start datetime DEFAULT NULL COMMENT '归层钉住的起用那一一日（不归层清空白）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='声环境监测点位底册';

CREATE TABLE IF NOT EXISTS t_nslp_task_item (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '复测与巡查条目代号',
  due_at datetime DEFAULT NULL COMMENT '该发复测通知那一一日（落库为 datetime）',
  amount decimal(12,2) DEFAULT NULL COMMENT '最多许提前几日',
  content varchar(255) DEFAULT NULL COMMENT '事由与所对点位的记要',
  status int DEFAULT NULL COMMENT '条目情形 0搁着 1已发出 2发不成',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='复测与巡查条目';

CREATE TABLE IF NOT EXISTS t_nslp_zone_book (
  id bigint NOT NULL COMMENT '主键',
  zone_no varchar(64) DEFAULT NULL COMMENT '声环境功能区区册代号',
  zone_name varchar(128) DEFAULT NULL COMMENT '区册全称',
  zone_kind varchar(32) DEFAULT NULL COMMENT '区层类别（甲层住宅·乙层混合·丙层工业·丁层交通干线）',
  road_name varchar(128) DEFAULT NULL COMMENT '属地详址（澜原省—江水市—区—片区）',
  align_kind varchar(32) DEFAULT NULL COMMENT '来路（市里区册下发／本局自录）',
  status int DEFAULT NULL COMMENT '区册情形 0在册 1已撤出',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='声环境功能区区册底册';

-- 初始档案数据（t_nslp_zone_book 预置两条区册种子）
-- zone_id=0 为在册区册（甲层住宅类），zone_id=1 为已并走撤出的旧区册（乙层），供挂靠校验与挡新入册校验取用。
-- 区册代号与区层类别按区层序给值。
INSERT IGNORE INTO t_nslp_zone_book (id, zone_no, zone_name, zone_kind, road_name, status, del_flag, create_by, create_time)
VALUES (0, 'QN00', '澜原省江水市云桥区临江片区（甲层住宅类，在册）', '甲层', '澜原省—江水市—云桥区—临江片区', 0, 0, 'seed', NOW()),
       (1, 'QN01', '南津区北岸片区·并走撤出的旧区册（已撤出）', '乙层', '澜原省—江水市—南津区—北岸片区', 1, 0, 'seed', NOW());

