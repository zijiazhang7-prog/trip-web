export type DestType = { value: string; label: string }

export type Destination = {
  /** Present when row comes from API (`DestinationVO.id`). */
  id?: number
  name: string
  reason: string
  rating: number
  price: string
  badge: string
  type: string
  image: string
  value: boolean
}

export type Spot = { name: string; duration: string; thumb: string }

export type Food = {
  id?: number
  destinationId?: number
  name: string
  dish: string
  price: string
  distance: string
  rating: string
  image: string
  tags: string[]
  description?: string
  lng?: number
  lat?: number
  heatScore?: number
  ratingScore?: number
}

export type CommunityPost = {
  id: number
  name: string
  location: string
  excerpt: string
  imgs: string[]
  likes: number
  comments: number
  avatar: string
}

export type Notebook = {
  title: string
  date: string
  days: number
  coverImage: string
  day1title: string
  content: string
  diaryImgs: string[]
}

export const destTypes: DestType[] = [
  { value: '', label: '全部目的地' },
  { value: 'nature', label: '自然风光' },
  { value: 'culture', label: '文化古迹' },
  { value: 'beach', label: '海岛度假' },
  { value: 'city', label: '都市体验' },
]

export const destinations: Destination[] = [
  {
    name: '张家界',
    reason: '奇峰异石，如临仙境，阿凡达取景地',
    rating: 4.9,
    price: '2800起',
    badge: '热门',
    type: '自然风光',
    image: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?q=80&w=800',
    value: true,
  },
  {
    name: '大理古城',
    reason: '白族风情、洱海日落，慢生活圣地',
    rating: 4.8,
    price: '1500起',
    badge: '性价比',
    type: '古镇漫游',
    image: 'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?q=80&w=800',
    value: true,
  },
  {
    name: '三亚',
    reason: '碧海蓝天、沙滩椰树，南国度假首选',
    rating: 4.7,
    price: '3500起',
    badge: '当季推荐',
    type: '海岛度假',
    image: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=800',
    value: false,
  },
  {
    name: '成都',
    reason: '熊猫、火锅与慢生活，来了就不想走',
    rating: 4.9,
    price: '1800起',
    badge: '必去',
    type: '都市寻味',
    image: 'https://images.unsplash.com/photo-1449824913935-59a10b8d2000?q=80&w=800',
    value: true,
  },
  {
    name: '西安',
    reason: '兵马俑、古城墙，穿越千年历史长河',
    rating: 4.8,
    price: '2200起',
    badge: '文化游',
    type: '文化古迹',
    image: 'https://images.unsplash.com/photo-1508804185872-411db1fb6d22?q=80&w=800',
    value: false,
  },
  {
    name: '厦门鼓浪屿',
    reason: '文艺小岛、殖民风情，走一步一个景',
    rating: 4.6,
    price: '2000起',
    badge: '亲子游',
    type: '海滨风情',
    image: 'https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?q=80&w=800',
    value: true,
  },
]

export const hotels = [
  '北京王府井希尔顿',
  '北京国贸大酒店',
  '北京燕翔饭店',
  '北京民宿·四合雅苑',
]

export const spots: Spot[] = [
  {
    name: '故宫博物院',
    duration: '3-4小时',
    thumb: 'https://images.unsplash.com/photo-1508804185872-411db1fb6d22?q=80&w=400',
  },
  {
    name: '天坛公园',
    duration: '2小时',
    thumb: 'https://images.unsplash.com/photo-1547981609-4b6bfe67ca0b?q=80&w=400',
  },
  {
    name: '颐和园',
    duration: '3小时',
    thumb: 'https://images.unsplash.com/photo-1586034177555-523c0cc1245a?q=80&w=400',
  },
  {
    name: '南锣鼓巷',
    duration: '1.5小时',
    thumb: 'https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?q=80&w=400',
  },
  {
    name: '798艺术区',
    duration: '2小时',
    thumb: 'https://images.unsplash.com/photo-1536924940846-227afb31e2a5?q=80&w=400',
  },
]

export const foodTags = ['川菜', '粤菜', '本地小吃', '甜品', '特色菜']

export const foods: Food[] = [
  {
    name: '蜀味馆',
    dish: '麻辣水煮鱼',
    price: '68',
    distance: '0.3km',
    rating: '5.0',
    image: 'https://images.unsplash.com/photo-1563379926898-05f4575a45d8?q=80&w=600',
    tags: ['川菜', '麻辣', '现炒'],
  },
  {
    name: '顺德茶点坊',
    dish: '虾饺皇',
    price: '45',
    distance: '0.6km',
    rating: '4.9',
    image: 'https://images.unsplash.com/photo-1496116218417-1a781b1c416c?q=80&w=600',
    tags: ['粤菜', '早茶', '点心'],
  },
  {
    name: '老城记小吃',
    dish: '炸酱面+豆汁',
    price: '28',
    distance: '0.2km',
    rating: '4.8',
    image: 'https://images.unsplash.com/photo-1585032226651-759b368d7246?q=80&w=600',
    tags: ['本地小吃', '传统'],
  },
  {
    name: '甜蜜时光',
    dish: '芒果班戟',
    price: '38',
    distance: '0.5km',
    rating: '4.7',
    image: 'https://images.unsplash.com/photo-1563729784474-d77dbb933a9e?q=80&w=600',
    tags: ['甜品', '网红'],
  },
  {
    name: '异域风情',
    dish: '泰式冬阴功',
    price: '88',
    distance: '1.2km',
    rating: '4.8',
    image: 'https://images.unsplash.com/photo-1548943487-a2e4b43b5731?q=80&w=600',
    tags: ['特色菜', '东南亚'],
  },
]

export const communityPosts: CommunityPost[] = [
  {
    id: 1,
    name: '晴天旅行者',
    location: '云南大理',
    excerpt:
      '洱海边的日落染红了整片天空，这是我旅行过最美的傍晚，时间仿佛在此凝固...',
    imgs: [
      'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?q=80&w=600',
      'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=600',
    ],
    likes: 234,
    comments: 48,
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=100',
  },
  {
    id: 2,
    name: '浪迹天涯',
    location: '西藏拉萨',
    excerpt:
      '布达拉宫在蓝天白云下显得格外庄严，这里的空气清冽，一呼一吸都是净化。',
    imgs: [
      'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?q=80&w=600',
      'https://images.unsplash.com/photo-1469474968028-56623f02e42e?q=80&w=600',
      'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?q=80&w=600',
    ],
    likes: 512,
    comments: 89,
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=100',
  },
  {
    id: 3,
    name: '背包客小李',
    location: '四川成都',
    excerpt: '熊猫基地一早就去，人不多，近距离看到了萌萌的大熊猫吃竹子！',
    imgs: ['https://images.unsplash.com/photo-1449824913935-59a10b8d2000?q=80&w=600'],
    likes: 187,
    comments: 35,
    avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?q=80&w=100',
  },
  {
    id: 4,
    name: '城市漫游者',
    location: '上海外滩',
    excerpt: '外滩的夜景是我见过最美的城市夜景之一，霓虹与历史建筑完美交融。',
    imgs: [
      'https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?q=80&w=600',
      'https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?q=80&w=600',
    ],
    likes: 339,
    comments: 67,
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=100',
  },
]

export const notebooks: Notebook[] = [
  {
    title: '2024 京都的春天',
    date: '2024-04-01',
    days: 7,
    coverImage: 'https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?q=80&w=1000',
    day1title: '初遇岚山',
    content:
      '一出地铁站，空气中便飘来樱花的淡香。岚山的竹林在晨光中显出翠绿的光泽，走在幽径之中，偶有风过，沙沙作响，仿佛整个世界都安静了下来。祇王寺的苔庭铺满青翠，我在石阶旁坐了许久，看游客来了又去，时光悄悄流淌...',
    diaryImgs: [
      'https://images.unsplash.com/photo-1522850657574-8b01bbbb26e8?q=80&w=400',
      'https://images.unsplash.com/photo-1542051812871-75f10b7f63f5?q=80&w=400',
      'https://images.unsplash.com/photo-1558862107-d49ef2a04d72?q=80&w=400',
    ],
  },
  {
    title: '杭州两日游',
    date: '2024-06-15',
    days: 2,
    coverImage: 'https://images.unsplash.com/photo-1501785888041-af3ef285b470?q=80&w=1000',
    day1title: '西湖漫步',
    content:
      '从断桥出发，沿着苏堤一路走到雷峰塔脚下，湖面波光粼粼，远处是起伏的群山。下午去了河坊街，品尝了正宗的西湖醋鱼，临走还买了一罐龙井茶叶带回去。傍晚在湖边的茶馆里喝了杯新茶，看着夕阳慢慢沉进湖水里...',
    diaryImgs: [
      'https://images.unsplash.com/photo-1572097087814-729906666ba4?q=80&w=400',
      'https://images.unsplash.com/photo-1563379926898-05f4575a45d8?q=80&w=400',
      'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?q=80&w=400',
    ],
  },
  {
    title: '云南秘境探索',
    date: '2024-09-10',
    days: 10,
    coverImage: 'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?q=80&w=1000',
    day1title: '抵达昆明',
    content:
      '飞机降落时，舷窗外是一片翠绿的坝子。昆明的天空湛蓝，空气带着高原的清冽。入住民宿后，在附近市场转了一圈，傣族的菠萝饭和烤豆腐让人垂涎，决定这次的云南之旅要尽量深入腹地，不走寻常路...',
    diaryImgs: [
      'https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?q=80&w=400',
      'https://images.unsplash.com/photo-1548943487-a2e4b43b5731?q=80&w=400',
      'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=400',
    ],
  },
]

export const interestTags = ['亲子', '古镇', '海滨', '美食', '文化']
