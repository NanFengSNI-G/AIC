create table chat_conversation
(
    id          bigint auto_increment
        primary key,
    user_id     bigint                             not null comment '用户ID',
    friend_id   bigint                             not null comment '好友ID',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_user_friend
        unique (user_id, friend_id)
)
    comment '最近聊天会话';

create index idx_user_update
    on chat_conversation (user_id, update_time);

create table chat_message
(
    id           bigint auto_increment comment '消息ID'
        primary key,
    message_id   varchar(36)                        not null comment '消息UUID，去重',
    from_user_id bigint                             not null comment '发送者ID',
    to_user_id   bigint                             not null comment '接收者ID',
    content      text                               not null comment '消息内容',
    msg_type     tinyint  default 1                 not null comment '1-文本 2-图片 3-文件 4-语音',
    create_time  datetime default CURRENT_TIMESTAMP not null
)
    comment '一对一聊天消息表';

create index idx_chat_session
    on chat_message (from_user_id asc, to_user_id asc, create_time desc);

create index idx_message_id
    on chat_message (message_id);

create index idx_session
    on chat_message (from_user_id, to_user_id);

create index idx_unread
    on chat_message (to_user_id);

create table forum_comment
(
    id          bigint auto_increment comment '评论ID'
        primary key,
    post_id     bigint                             not null comment '所属帖子ID',
    user_id     bigint                             not null comment '评论者ID',
    parent_id   bigint                             null comment '父评论ID（NULL表示一级评论）',
    content     text                               not null comment '评论内容',
    like_count  int      default 0                 null comment '点赞数',
    status      tinyint  default 1                 null comment '状态: 0-已删除, 1-正常',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '论坛评论表';

create index idx_create_time
    on forum_comment (create_time);

create index idx_parent_id
    on forum_comment (parent_id);

create index idx_post_comment_list
    on forum_comment (post_id, status, parent_id, create_time);

create index idx_post_id
    on forum_comment (post_id);

create index idx_status
    on forum_comment (status);

create index idx_user_id
    on forum_comment (user_id);

create table forum_post
(
    id            bigint auto_increment comment '帖子ID'
        primary key,
    user_id       bigint                                  not null comment '发布者ID',
    section_id    bigint                                  not null comment '所属板块ID',
    title         varchar(200)                            not null comment '帖子标题',
    content       text                                    not null comment '帖子内容',
    images        varchar(1000) default ''                null comment '图片URL列表，逗号分隔',
    like_count    int           default 0                 null comment '点赞数',
    comment_count int           default 0                 null comment '评论数',
    view_count    int           default 0                 null comment '浏览数',
    is_sticky     tinyint       default 0                 null comment '是否置顶: 0-否, 1-是',
    status        tinyint       default 1                 null comment '状态: 0-已删除, 1-正常',
    create_time   datetime      default CURRENT_TIMESTAMP null comment '创建时间',
    update_time   datetime      default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '论坛帖子表';

create fulltext index ft_title_content
    on forum_post (title, content);

create index idx_create_time
    on forum_post (create_time);

create index idx_like_count
    on forum_post (like_count);

create index idx_section_id
    on forum_post (section_id);

create index idx_status
    on forum_post (status);

create index idx_user_id
    on forum_post (user_id);

create index idx_user_post_list
    on forum_post (user_id asc, status asc, create_time desc);

create table forum_post_favorite
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    post_id     bigint                             not null comment '帖子ID',
    user_id     bigint                             not null comment '用户ID',
    create_time datetime default CURRENT_TIMESTAMP null comment '收藏时间',
    constraint uk_post_user
        unique (post_id, user_id)
)
    comment '帖子收藏表';

create index idx_user_id
    on forum_post_favorite (user_id);

create table forum_section
(
    id          bigint auto_increment comment '板块ID'
        primary key,
    name        varchar(50)                            not null comment '板块名称',
    description varchar(255) default ''                null comment '板块描述',
    icon        varchar(255) default ''                null comment '板块图标URL',
    sort_order  int          default 0                 null comment '排序顺序',
    post_count  int          default 0                 null comment '帖子总数',
    status      tinyint      default 1                 null comment '状态: 0-禁用, 1-正常',
    create_time datetime     default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '论坛板块表';

create index idx_sort_order
    on forum_section (sort_order);

create index idx_status
    on forum_section (status);

create table interview_qa
(
    id          bigint auto_increment
        primary key,
    record_id   bigint                             not null,
    question    text                               null,
    answer      text                               null,
    evaluation  text                               null,
    status      tinyint  default 0                 null comment '状态: 0-正常, 1-已删除',
    create_time datetime default CURRENT_TIMESTAMP null
);

create index idx_record_id
    on interview_qa (record_id);

create table interview_record
(
    id             bigint auto_increment
        primary key,
    user_id        bigint                             not null,
    record_name    varchar(255)                       null comment '记录名称',
    duration       int      default 0                 null comment '面试耗时(秒)',
    question_count int      default 0                 null comment '问答轮数',
    resume         text                               null comment '简历内容',
    jd             text                               null comment '职位描述',
    evaluation     text                               null comment '综合评价',
    status         tinyint  default 0                 null comment '状态: 0-正常, 1-已删除',
    create_time    datetime default CURRENT_TIMESTAMP null
)
    comment '面试记录表';

create index idx_user_id
    on interview_record (user_id);

create table sys_friend
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    user_id     bigint                                 not null comment '用户ID（发起方）',
    friend_id   bigint                                 not null comment '好友用户ID（接收方）',
    status      tinyint      default 0                 not null comment '状态: 0-待确认, 1-已同意, 2-已拒绝, 3-已删除',
    message     varchar(255) default ''                null comment '申请留言',
    create_time datetime     default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_friend_pair
        unique (user_id, friend_id)
)
    comment '好友关系表';

create index idx_friend_id
    on sys_friend (friend_id);

create index idx_status
    on sys_friend (status);

create index idx_user_id
    on sys_friend (user_id);

create table sys_role
(
    id          bigint auto_increment comment '角色ID'
        primary key,
    role_name   varchar(50)                        not null comment '角色名(唯一)',
    role_desc   varchar(100)                       null comment '角色描述',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint uk_role_name
        unique (role_name)
)
    comment '角色表' collate = utf8mb4_unicode_ci;

create table sys_user
(
    id          bigint auto_increment comment '用户ID'
        primary key,
    username    varchar(50)                        not null comment '用户名(唯一)',
    password    varchar(255)                       not null comment '密码(BCrypt加密)',
    email       varchar(100)                       not null comment '邮箱(唯一)',
    status      tinyint  default 1                 not null comment '状态: 0-禁用, 1-正常',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_email
        unique (email),
    constraint uk_username
        unique (username)
)
    comment '用户表' collate = utf8mb4_unicode_ci;

create table sys_user_detail
(
    id       bigint auto_increment comment '主键ID'
        primary key,
    user_id  bigint       not null comment '用户ID',
    username varchar(50)  null comment '用户名',
    avatar   varchar(500) null comment '头像URL',
    age      int          null comment '年龄',
    bio      varchar(500) null comment '个人简介',
    constraint fk_user_detail_user_id
        foreign key (user_id) references chatroom.sys_user (id)
            on delete cascade
)
    comment '用户详情表' collate = utf8mb4_unicode_ci;

create index idx_user_detail_user_id
    on sys_user_detail (user_id);

create table sys_user_role
(
    id      bigint auto_increment comment '主键'
        primary key,
    user_id bigint not null comment '用户ID',
    role_id bigint not null comment '角色ID',
    constraint uk_user_role
        unique (user_id, role_id),
    constraint fk_user_role_role_id
        foreign key (role_id) references chatroom.sys_role (id)
            on delete cascade,
    constraint fk_user_role_user_id
        foreign key (user_id) references chatroom.sys_user (id)
            on delete cascade
)
    comment '用户角色关联表' collate = utf8mb4_unicode_ci;

create index idx_user_role_role_id
    on sys_user_role (role_id);

create index idx_user_role_user_id
    on sys_user_role (user_id);

