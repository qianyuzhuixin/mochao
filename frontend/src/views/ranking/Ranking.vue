<template>
  <div class="ranking-page">
    <div class="page-header">
      <h1>扫榜分析</h1>
      <p class="page-desc">
        每天凌晨 3:00 自动抓取当日榜单数据。
        也可手动点击按钮立即抓取。历史数据可切换日期查看。
      </p>
    </div>

    <!-- 筛选区 -->
    <div class="filter-bar">
      <!-- 模式切换 -->
      <div class="mode-tabs">
        <button
          :class="['mode-tab', { active: viewMode === 'ranking' }]"
          @click="switchMode('ranking')"
        >
          <i class="el-icon-s-data" /> 榜单浏览
        </button>
        <button
          :class="['mode-tab', { active: viewMode === 'search' }]"
          @click="switchMode('search')"
        >
          <i class="el-icon-search" /> 小说搜索
        </button>
      </div>

      <!-- 搜索模式 -->
      <template v-if="viewMode === 'search'">
        <div class="search-bar">
          <div class="search-row">
            <el-input
              v-model="searchKeyword"
              placeholder="输入书名或作者，搜索全平台小说"
              clearable
              size="medium"
              class="search-input"
              @keyup.enter.native="handleSearch"
              @clear="clearSearch"
            >
              <el-select
                v-model="searchPlatform"
                slot="prepend"
                placeholder="全平台"
                style="width:120px"
              >
                <el-option label="全平台" value="" />
                <el-option
                  v-for="p in platforms"
                  :key="p.value"
                  :label="p.label"
                  :value="p.value"
                />
              </el-select>
              <el-button slot="append" icon="el-icon-search" @click="handleSearch">搜索</el-button>
            </el-input>
          </div>
        </div>
      </template>

      <!-- 榜单模式 -->
      <template v-else>
      <!-- 第一行：平台 + 频道 -->
      <div class="filter-row">
        <div class="filter-group">
          <label>平台</label>
          <div class="platform-tabs">
            <button
              v-for="p in platforms"
              :key="p.value"
              :class="['tab-btn', { active: currentPlatform === p.value }]"
              @click="switchPlatform(p.value)"
            >
              {{ p.label }}
            </button>
          </div>
        </div>

        <div class="filter-group" v-if="platformHasChannel">
          <label>频道</label>
          <div class="channel-tabs">
            <button
              v-for="ch in platformChannels"
              :key="ch.value"
              :class="['tab-btn', 'tab-btn-sm', { active: currentChannel === ch.value }]"
              @click="switchChannel(ch.value)"
            >
              {{ ch.label }}
            </button>
          </div>
        </div>
      </div>

      <!-- 第二行：榜单 + 分类 + 日期 + 抓取按钮 -->
      <div class="filter-row">
        <div class="filter-group">
          <label>榜单</label>
          <el-select v-model="currentRankSubType" placeholder="选择榜单" size="small" filterable @change="onFilterChange">
            <el-option
              v-for="r in availableRankTypes"
              :key="r.value"
              :label="r.label"
              :value="r.value"
            />
          </el-select>
        </div>

        <div class="filter-group" v-if="showCategorySelect">
          <label>分类</label>
          <el-select v-model="currentCategory" placeholder="选择分类" size="small" filterable @change="onFilterChange">
            <el-option
              v-for="c in availableCategories"
              :key="c.catId"
              :label="c.label"
              :value="c.catId"
            />
          </el-select>
        </div>

        <div class="filter-group">
          <label>日期</label>
          <el-select
            v-model="snapDate"
            placeholder="选择日期"
            size="small"
            filterable
            @change="onDateChange"
          >
            <el-option
              v-for="d in availableDates"
              :key="d"
              :label="d"
              :value="d"
            />
          </el-select>
        </div>

        <!-- 抓取按钮：仅当天无数据时显示 -->
        <el-button
          v-if="showScrapeBtn"
          type="primary"
          size="small"
          :loading="scraping"
          icon="el-icon-download"
          @click="handleScrape"
        >
          {{ scraping ? '正在抓取...' : '立即抓取' }}
        </el-button>
        <span v-else class="has-data-tip">
          <i class="el-icon-circle-check" /> 今日数据已就绪
        </span>
      </div>

      <div v-if="scrapeMsg" :class="['scrape-msg', scrapeMsgType]">
        {{ scrapeMsg }}
      </div>

      <div v-if="downloadMsg" :class="['scrape-msg', downloadMsgType]">
        {{ downloadMsg }}
      </div>
      </template>
    </div>

    <!-- 结果表格 -->
    <div class="result-section">
      <!-- 搜索模式表头 -->
      <div v-if="viewMode === 'search' && !loading" class="search-result-header">
        <span v-if="searchKeyword">搜索 "{{ searchKeyword }}"：共 {{ total }} 条结果</span>
        <span v-else>请输入关键词开始搜索</span>
        <span class="search-source">数据来源：番茄小说实时搜索</span>
      </div>

      <div v-if="loading" class="loading-wrap">
        <i class="el-icon-loading" />
        <span>加载中...</span>
      </div>

      <template v-else-if="tableData.length > 0">
        <div class="result-header">
          <span v-if="viewMode === 'ranking'">共 {{ total }} 条记录</span>
          <span class="result-date" v-if="snapDate && viewMode === 'ranking'">快照日期：{{ snapDate }}</span>
        </div>
        <el-table :data="tableData" stripe size="small" highlight-current-row>
          <el-table-column v-if="viewMode === 'ranking'" prop="rankNo" label="排名" width="60" sortable />
          <el-table-column v-if="viewMode === 'search'" prop="platform" label="平台" width="100">
            <template #default="{ row }">
              {{ platformLabel(row.platform) }}
            </template>
          </el-table-column>
          <el-table-column prop="bookName" label="书名" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <a v-if="row.bookUrl" :href="row.bookUrl" target="_blank" class="book-link">{{ row.bookName }}</a>
              <span v-else>{{ row.bookName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="author" label="作者" width="120" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column v-if="viewMode === 'ranking'" prop="hotValue" label="热度" width="100" sortable>
            <template #default="{ row }">
              {{ formatNumber(row.hotValue) }}
            </template>
          </el-table-column>
          <el-table-column prop="wordCount" label="字数" width="100">
            <template #default="{ row }">
              {{ formatWordCount(row.wordCount) }}
            </template>
          </el-table-column>
          <el-table-column label="简介" min-width="200">
            <template #default="{ row }">
              <el-tooltip
                v-if="row.intro"
                :content="row.intro"
                placement="top"
                popper-class="abstract-tooltip"
                :enterable="false"
              >
                <span class="cell-text">{{ row.intro }}</span>
              </el-tooltip>
              <span v-else class="cell-text">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-dropdown
                v-if="canDownload(row)"
                trigger="click"
                size="small"
                @command="cmd => handleDownload(cmd, row)"
              >
                <el-button size="mini" :loading="isDownloading(row)" type="primary" plain>
                  <i class="el-icon-download" /> 下载<i class="el-icon-arrow-down el-icon--right" />
                </el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="txt" icon="el-icon-document">
                    TXT 文本
                  </el-dropdown-item>
                  <el-dropdown-item command="html" icon="el-icon-notebook-1">
                    HTML 网页
                  </el-dropdown-item>
                  <el-dropdown-item command="pdf" icon="el-icon-files">
                    PDF 电子书
                  </el-dropdown-item>
                  <el-dropdown-item command="personal" icon="el-icon-user" divided>
                    下载到个人素材
                  </el-dropdown-item>
                  <el-dropdown-item v-if="isAdmin" command="library" icon="el-icon-folder-opened">
                    下载到内置书库
                  </el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
              <span v-else class="no-action">—</span>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="viewMode === 'ranking'"
          class="pagination-wrap"
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="page"
          @current-change="fetchData"
        />
      </template>

      <div v-else class="empty-wrap">
        <i class="el-icon-document" />
        <p v-if="viewMode === 'search' && searchKeyword">未找到与 "{{ searchKeyword }}" 相关的小说</p>
        <p v-else-if="viewMode === 'search'">输入书名或作者关键词，搜索已收录榜单中的小说</p>
        <p v-else-if="isToday">暂无数据，点击"立即抓取"获取今日榜单</p>
        <p v-else>该日期暂无数据</p>
      </div>
    </div>
  </div>
</template>

<script>
import { getRanking, triggerScrape, checkTodayData, getAvailableDates, downloadBook, downloadFile, searchBooks } from '@/api/ranking'
import { mapGetters } from 'vuex'

export default {
  name: 'RankingPage',
  data() {
    return {
      currentPlatform: 'qidian',
      currentChannel: null,
      currentRankSubType: 'month_ticket',
      currentCategory: '',
      snapDate: this.formatDate(new Date()),
      scraping: false,
      scrapeMsg: '',
      scrapeMsgType: 'info',
      loading: false,
      tableData: [],
      total: 0,
      page: 1,
      pageSize: 50,
      hasTodayData: false,
      availableDates: [],
      downloadingBookIds: [],  // 正在下载的书籍 bookId 列表
      downloadMsg: '',
      downloadMsgType: 'info',

      // 搜索模式
      viewMode: 'ranking',      // 'ranking' | 'search'
      searchKeyword: '',
      searchPlatform: '',
      searchLoading: false,

      platforms: [
        { label: '起点中文网', value: 'qidian' },
        { label: '番茄小说', value: 'fanqie' },
        { label: '晋江文学城', value: 'jinjiang' },
        { label: '七猫小说', value: 'qimao' },
        { label: '刺猬猫', value: 'ciweimao' },
        { label: '知乎盐言', value: 'zhihu' }
      ],

      // 各平台配置 — 频道/榜单/分类层级结构
      platformConfigs: {
        // 起点：无频道，扁平榜单
        qidian: {
          hasChannel: false,
          ranks: [
            { label: '月票榜', value: 'month_ticket' },
            { label: '畅销榜', value: 'hotsales' },
            { label: '推荐票榜', value: 'recommend' },
            { label: '收藏榜', value: 'collect' },
            { label: '阅读指数榜', value: 'readindex' },
            { label: '签约作者新书榜', value: 'signnewbook' },
            { label: '新人签约新书榜', value: 'newsign' },
            { label: '新人作者新书榜', value: 'newauthor' },
            { label: '公众作者新书榜', value: 'pubnewbook' },
            { label: '三江推荐', value: 'sanjiang' }
          ]
        },

        // 番茄：综合/男频/女频 × 热搜/阅读/新书 × 19男频+18女频分类
        fanqie: {
          hasChannel: true,
          channels: [
            { label: '综合', value: 'overall' },
            { label: '男频', value: 'male' },
            { label: '女频', value: 'female' }
          ],
          rankTypes: [
            { label: '热搜榜', value: 'hot_search' },
            { label: '阅读榜', value: 'read_rank' },
            { label: '新书榜', value: 'new_book' }
          ],
          categories: {
            male: [
              { label: '西方奇幻', catId: '1141' },
              { label: '东方仙侠', catId: '1140' },
              { label: '科幻末世', catId: '8' },
              { label: '都市日常', catId: '261' },
              { label: '都市修真', catId: '124' },
              { label: '都市高武', catId: '1014' },
              { label: '历史古代', catId: '273' },
              { label: '战神赘婿', catId: '27' },
              { label: '都市种田', catId: '263' },
              { label: '传统玄幻', catId: '258' },
              { label: '历史脑洞', catId: '272' },
              { label: '悬疑脑洞', catId: '539' },
              { label: '都市脑洞', catId: '262' },
              { label: '玄幻脑洞', catId: '257' },
              { label: '悬疑灵异', catId: '751' },
              { label: '抗战谍战', catId: '504' },
              { label: '游戏体育', catId: '746' },
              { label: '动漫衍生', catId: '718' },
              { label: '男频衍生', catId: '1016' }
            ],
            female: [
              { label: '古风世情', catId: '1139' },
              { label: '科幻末世', catId: '8' },
              { label: '游戏体育', catId: '746' },
              { label: '女频衍生', catId: '1015' },
              { label: '玄幻言情', catId: '248' },
              { label: '种田', catId: '23' },
              { label: '年代', catId: '79' },
              { label: '现言脑洞', catId: '267' },
              { label: '宫斗宅斗', catId: '246' },
              { label: '悬疑脑洞', catId: '539' },
              { label: '古言脑洞', catId: '253' },
              { label: '快穿', catId: '24' },
              { label: '青春甜宠', catId: '749' },
              { label: '星光璀璨', catId: '745' },
              { label: '女频悬疑', catId: '747' },
              { label: '职场婚恋', catId: '750' },
              { label: '豪门总裁', catId: '748' },
              { label: '民国言情', catId: '1017' }
            ]
          }
        },

        // 晋江：无频道，扁平榜单
        jinjiang: {
          hasChannel: false,
          ranks: [
            { label: '收入金榜', value: 'income12' },
            { label: '千字金榜', value: 'kzi17' },
            { label: '月榜', value: 'month7' },
            { label: '季度榜', value: 'season8' },
            { label: '完结金榜', value: 'finish14' },
            { label: '新手金榜', value: 'new15' },
            { label: '收藏榜', value: 'collect' }
          ]
        },

        // 七猫：男频/女频 × 5种榜单
        qimao: {
          hasChannel: true,
          channels: [
            { label: '男频', value: 'boy' },
            { label: '女频', value: 'girl' }
          ],
          rankTypes: [
            { label: '大热榜', value: 'hot' },
            { label: '新书榜', value: 'new' },
            { label: '完结榜', value: 'over' },
            { label: '收藏榜', value: 'collect' },
            { label: '更新榜', value: 'update' }
          ]
        },

        // 刺猬猫：无频道，扁平榜单
        ciweimao: {
          hasChannel: false,
          ranks: [
            { label: '点击榜', value: 'click' },
            { label: '收藏榜', value: 'favor' },
            { label: '推荐榜', value: 'recommend' },
            { label: '订阅榜', value: 'subscribe' },
            { label: '月票榜', value: 'monthly' },
            { label: '吐槽榜', value: 'tsukkomi' },
            { label: '新书榜', value: 'newbook' },
            { label: '刀片榜', value: 'blade' },
            { label: '更新榜', value: 'update' }
          ]
        },

        // 知乎：无频道，仅提示
        zhihu: {
          hasChannel: false,
          ranks: [
            { label: '热门榜', value: 'hot' }
          ]
        }
      }
    }
  },
  computed: {
    ...mapGetters('auth', ['isAdmin']),
    platformConfig() {
      return this.platformConfigs[this.currentPlatform] || {}
    },
    platformHasChannel() {
      return !!this.platformConfig.hasChannel
    },
    platformChannels() {
      return this.platformConfig.channels || []
    },
    availableRankTypes() {
      const cfg = this.platformConfig
      return cfg.rankTypes || cfg.ranks || []
    },
    showCategorySelect() {
      return this.currentPlatform === 'fanqie' &&
        (this.currentChannel === 'male' || this.currentChannel === 'female')
    },
    availableCategories() {
      if (!this.showCategorySelect) return []
      return (this.platformConfig.categories || {})[this.currentChannel] || []
    },
    /** 最终发送给 API 的 rankType */
    finalRankType() {
      if (!this.currentRankSubType) return ''
      const cfg = this.platformConfig

      // 无频道的平台：直接用 rankSubType
      if (!cfg.hasChannel) {
        return this.currentRankSubType
      }

      // 番茄
      if (this.currentPlatform === 'fanqie') {
        if (this.currentChannel === 'overall') {
          return this.currentRankSubType
        }
        if (!this.currentCategory) return ''
        return `${this.currentChannel}-${this.currentCategory}-${this.currentRankSubType}`
      }

      // 七猫
      if (this.currentPlatform === 'qimao') {
        return `${this.currentChannel}-${this.currentRankSubType}`
      }

      return this.currentRankSubType
    },
    isToday() {
      return this.snapDate === this.formatDate(new Date())
    },
    showScrapeBtn() {
      return this.isToday && !this.hasTodayData
    }
  },
  async created() {
    await this.loadAvailableDates()
    await this.checkToday()
    this.fetchData()
  },
  methods: {
    /** 切换平台 */
    async switchPlatform(platform) {
      this.scrapeMsg = ''
      this.scrapeMsgType = ''
      this.currentPlatform = platform
      const cfg = this.platformConfigs[platform] || {}

      // 设置频道
      if (cfg.hasChannel && cfg.channels && cfg.channels.length) {
        this.currentChannel = cfg.channels[0].value
      } else {
        this.currentChannel = null
      }

      // 设置榜单类型
      const ranks = cfg.rankTypes || cfg.ranks || []
      this.currentRankSubType = ranks.length > 0 ? ranks[0].value : ''

      // 设置分类（仅番茄男/女频需要）
      if (this.showCategorySelect && this.availableCategories.length > 0) {
        this.currentCategory = this.availableCategories[0].catId
      } else {
        this.currentCategory = ''
      }

      this.page = 1
      this.snapDate = this.formatDate(new Date())
      await this.loadAvailableDates()
      await this.checkToday()
      this.fetchData()
    },

    /** 切换频道 */
    switchChannel(channel) {
      this.scrapeMsg = ''
      this.scrapeMsgType = ''
      this.currentChannel = channel

      // 重置榜单类型
      const ranks = this.availableRankTypes
      this.currentRankSubType = ranks.length > 0 ? ranks[0].value : ''

      // 重置分类
      if (this.showCategorySelect && this.availableCategories.length > 0) {
        this.currentCategory = this.availableCategories[0].catId
      } else {
        this.currentCategory = ''
      }

      this.page = 1
      this.loadAvailableDates()
      this.checkToday()
      this.fetchData()
    },

    /** 榜单/分类切换 */
    onFilterChange() {
      this.scrapeMsg = ''
      this.scrapeMsgType = ''
      this.page = 1
      this.loadAvailableDates()
      this.checkToday()
      this.fetchData()
    },

    /** 日期切换 */
    onDateChange() {
      this.scrapeMsg = ''
      this.scrapeMsgType = ''
      this.page = 1
      this.fetchData()
    },

    /** 加载可选日期列表（始终包含今天） */
    async loadAvailableDates() {
      try {
        const res = await getAvailableDates(this.currentPlatform, this.finalRankType)
        const dates = res || []
        const today = this.formatDate(new Date())
        if (!dates.includes(today)) {
          dates.unshift(today)
        }
        this.availableDates = dates
      } catch {
        this.availableDates = [this.formatDate(new Date())]
      }
    },

    /** 检查当天是否有数据 */
    async checkToday() {
      try {
        const res = await checkTodayData(this.currentPlatform, this.finalRankType)
        this.hasTodayData = !!(res && res.exists)
      } catch {
        this.hasTodayData = false
      }
    },

    /** 查询榜单数据 */
    async fetchData() {
      this.loading = true
      try {
        const res = await getRanking({
          platform: this.currentPlatform,
          rankType: this.finalRankType,
          snapDate: this.snapDate,
          page: this.page,
          size: this.pageSize
        })
        this.tableData = (res && res.records) ? res.records : []
        this.total = (res && res.total) ? res.total : 0
      } catch {
        this.tableData = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },

    /** 触发抓取 */
    async handleScrape() {
      if (this.scraping) return
      this.scraping = true
      this.scrapeMsg = ''
      try {
        const res = await triggerScrape(this.currentPlatform, this.finalRankType)
        if (res.message) {
          this.scrapeMsg = res.message
          this.scrapeMsgType = 'info'
          this.hasTodayData = true
          this.fetchData()
        } else {
          this.scrapeMsg = `抓取完成！共获取 ${res.count || 0} 条数据`
          this.scrapeMsgType = 'success'
          this.hasTodayData = true
          this.page = 1
          this.snapDate = this.formatDate(new Date())
          this.loadAvailableDates()
          this.fetchData()
        }
      } catch (err) {
        this.scrapeMsg = err.message || '抓取失败，请确认抓取服务已启动'
        this.scrapeMsgType = 'error'
      } finally {
        this.scraping = false
      }
    },

    indexMethod(index) {
      return (this.page - 1) * this.pageSize + index + 1
    },

    formatDate(date) {
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      return `${y}-${m}-${d}`
    },

    formatNumber(n) {
      if (!n) return '0'
      if (n >= 10000) return (n / 10000).toFixed(1) + '万'
      return n.toLocaleString()
    },

    formatWordCount(n) {
      if (!n) return '-'
      if (n >= 10000) return (n / 10000).toFixed(1) + '万字'
      return n.toLocaleString()
    },

    /** 判断某行是否可下载（番茄平台 + 有 bookUrl） */
    canDownload(row) {
      const platform = row.platform || this.currentPlatform
      return platform === 'fanqie' && row.bookUrl
    },

    /** 判断某行是否正在下载 */
    isDownloading(row) {
      const bookId = this.extractBookId(row)
      return bookId ? this.downloadingBookIds.includes(bookId) : false
    },

    /** 从行数据提取 bookId */
    extractBookId(row) {
      if (!row.bookUrl) return null
      const match = row.bookUrl.match(/\/page\/(\d+)/)
      return match ? match[1] : null
    },

    /** 处理下载 */
    async handleDownload(target, row) {
      const bookId = this.extractBookId(row)
      if (!bookId) {
        this.$message.error('无法获取书籍ID')
        return
      }

      // 文件格式下载（txt / html / pdf）
      if (['txt', 'html', 'pdf'].includes(target)) {
        await this.handleFileDownload(target, row, bookId)
        return
      }

      // 下载到书库（personal / library）
      if (this.downloadingBookIds.includes(bookId)) {
        this.$message.warning('正在下载中，请稍候')
        return
      }

      this.downloadingBookIds.push(bookId)
      this.downloadMsg = `正在下载《${row.bookName}》...`
      this.downloadMsgType = 'info'

      try {
        const res = await downloadBook({
          platform: row.platform || this.currentPlatform,
          bookId,
          target,
          maxChapters: 0
        })

        this.downloadMsg = res.message || `《${row.bookName}》下载完成`
        this.downloadMsgType = 'success'
        this.$message.success(this.downloadMsg)
      } catch (err) {
        const errMsg = (err && err.message) || '下载失败，请确认抓取服务已启动'
        this.downloadMsg = errMsg
        this.downloadMsgType = 'error'
      } finally {
        const idx = this.downloadingBookIds.indexOf(bookId)
        if (idx > -1) this.downloadingBookIds.splice(idx, 1)
      }
    },

    /**
     * 下载文件到本地（TXT / HTML / PDF）
     */
    async handleFileDownload(format, row, bookId) {
      if (this.downloadingBookIds.includes(bookId)) {
        this.$message.warning('正在下载中，请稍候')
        return
      }

      this.downloadingBookIds.push(bookId)
      const formatLabel = { txt: 'TXT', html: 'HTML', pdf: 'PDF' }[format] || format
      this.downloadMsg = `正在生成《${row.bookName}》${formatLabel}文件...`
      this.downloadMsgType = 'info'

      try {
        const blob = await downloadFile(bookId, format, 0)

        // 从 blob 创建下载链接
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url

        // 从响应头或默认生成文件名
        const safeName = row.bookName.replace(/[\\/:*?"<>|]/g, '_')
        const extMap = { txt: 'txt', html: 'html', pdf: 'pdf' }
        link.download = `${safeName} - ${row.author || '未知'}.${extMap[format]}`

        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)

        this.downloadMsg = `《${row.bookName}》${formatLabel}下载完成`
        this.downloadMsgType = 'success'
        this.$message.success(this.downloadMsg)
      } catch (err) {
        const errMsg = (err && err.message) || '文件下载失败，请确认抓取服务已启动'
        this.downloadMsg = errMsg
        this.downloadMsgType = 'error'
        this.$message.error(errMsg)
      } finally {
        const idx = this.downloadingBookIds.indexOf(bookId)
        if (idx > -1) this.downloadingBookIds.splice(idx, 1)
      }
    },

    // ==================== 搜索模式 ====================

    /** 切换模式 */
    switchMode(mode) {
      this.viewMode = mode
      if (mode === 'ranking') {
        this.searchKeyword = ''
        this.tableData = []
        this.total = 0
        this.fetchData()
      } else {
        this.tableData = []
        this.total = 0
      }
    },

    /** 执行搜索 */
    async handleSearch() {
      const kw = this.searchKeyword.trim()
      if (!kw) return

      this.searchLoading = true
      this.loading = true
      try {
        const res = await searchBooks({
          keyword: kw,
          platform: this.searchPlatform,
          limit: 200
        })
        // res 可能是 { code, data: { books, total } } 或直接是 data
        const data = (res && res.data) ? res.data : res
        this.tableData = (data && data.books) ? data.books : []
        this.total = (data && data.total != null) ? data.total : this.tableData.length
      } catch (err) {
        this.tableData = []
        this.total = 0
        this.$message.error('搜索失败：' + ((err && err.message) || '网络错误'))
      } finally {
        this.searchLoading = false
        this.loading = false
      }
    },

    /** 清空搜索 */
    clearSearch() {
      this.searchKeyword = ''
      this.tableData = []
      this.total = 0
    },

    /** 平台值 → 中文名 */
    platformLabel(val) {
      const map = {
        qidian: '起点',
        fanqie: '番茄',
        jinjiang: '晋江',
        qimao: '七猫',
        ciweimao: '刺猬猫',
        zhihu: '知乎'
      }
      return map[val] || val || '-'
    }
  }
}
</script>

<style lang="scss" scoped>
.ranking-page {
  max-width: #{$content-max-width};
  margin: 0 auto;
  padding: #{$spacing-xxl} #{$spacing-lg};
}

.page-header {
  margin-bottom: #{$spacing-xl};
  h1 {
    font-size: #{$font-size-xxl};
    font-weight: 700;
    color: var(--color-text);
    margin: 0 0 #{$spacing-sm};
  }
  .page-desc {
    color: var(--color-text-secondary);
    font-size: #{$font-size-sm};
    margin: 0;
    line-height: 1.6;
  }
}

.filter-bar {
  background: var(--color-card-bg);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: #{$spacing-lg};
  margin-bottom: #{$spacing-lg};
}

.filter-row {
  display: flex;
  align-items: center;
  gap: #{$spacing-lg};
  flex-wrap: wrap;

  & + .filter-row {
    margin-top: #{$spacing-sm};
  }
}

.filter-group {
  display: flex;
  align-items: center;
  gap: #{$spacing-sm};
  label {
    font-size: #{$font-size-sm};
    color: var(--color-text-secondary);
    white-space: nowrap;
  }
}

.platform-tabs,
.channel-tabs {
  display: flex;
  gap: 4px;
  background: var(--color-bg);
  border-radius: 8px;
  padding: 3px;
}

.tab-btn {
  border: none;
  background: transparent;
  padding: 5px 14px;
  border-radius: 6px;
  font-size: #{$font-size-sm};
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  &:hover { color: var(--color-text); }
  &.active {
    background: var(--color-primary);
    color: #fff;
  }
}

.tab-btn-sm {
  padding: 4px 12px;
  font-size: #{$font-size-xs};
}

.has-data-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: #{$font-size-sm};
  color: #52c41a;
  i { font-size: #{$font-size-base}; }
}

.scrape-msg {
  margin-top: #{$spacing-sm};
  font-size: #{$font-size-sm};
  padding: 6px 12px;
  border-radius: 6px;
  &.success { background: rgba(82, 196, 26, 0.1); color: #52c41a; }
  &.error { background: rgba(255, 77, 79, 0.1); color: #ff4d4f; }
  &.info { background: var(--color-bg); color: var(--color-text-secondary); }
}

.result-section {
  background: var(--color-card-bg);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: #{$spacing-lg};
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: #{$spacing-md};
  font-size: #{$font-size-sm};
  color: var(--color-text-secondary);
}

.result-date {
  color: var(--color-primary);
}

.book-link {
  color: var(--color-primary);
  text-decoration: none;
  &:hover { text-decoration: underline; }
}

.pagination-wrap {
  margin-top: #{$spacing-lg};
  text-align: center;
}

.loading-wrap,
.empty-wrap {
  text-align: center;
  padding: #{$spacing-xxl} 0;
  color: var(--color-text-secondary);
  i { font-size: 36px; display: block; margin-bottom: #{$spacing-sm}; }
  p { margin: 0; font-size: #{$font-size-base}; }
}

.no-action {
  color: var(--color-text-secondary);
  font-size: #{$font-size-sm};
}

/* ====== 搜索模式样式 ====== */
.mode-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: #{$spacing-md};
  padding-bottom: #{$spacing-md};
  border-bottom: 1px solid var(--color-border);
}

.mode-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  padding: 7px 20px;
  border-radius: 20px;
  font-size: #{$font-size-sm};
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    color: var(--color-primary);
    border-color: var(--color-primary);
  }
  &.active {
    background: var(--color-primary);
    color: #fff;
    border-color: var(--color-primary);
  }
}

.search-bar {
  margin-bottom: #{$spacing-sm};
}

.search-row {
  display: flex;
  align-items: center;
}

.search-input {
  max-width: 560px;
}

.search-result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: #{$spacing-md};
  font-size: #{$font-size-sm};
  color: var(--color-text-secondary);
  .search-source {
    color: var(--color-text-secondary);
    font-size: #{$font-size-xs};
  }
}
</style>

<style lang="scss">
/* 简介 tooltip 宽度限制（ElementUI tooltip 挂载在 body 下，需用非 scoped 样式） */
.el-tooltip__popper.abstract-tooltip {
  max-width: 480px !important;
  line-height: 1.6;
  word-wrap: break-word;
  word-break: break-all;
}
</style>

<style lang="scss" scoped>
.cell-text {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-all;
}
</style>
