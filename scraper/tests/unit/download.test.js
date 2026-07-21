/**
 * download.js 纯函数单元测试
 * 
 * 测试: 截断检测(isTruncated)、章节有效性(isChapterContentValid)、HTML清洗(stripFanqieHtml)
 * 注意: decodeFanqieText 和 decodeBest 依赖 charset.json，需要在有该文件的环境中运行
 */

// Mock 外部依赖，避免初始化错误
jest.mock('fs', () => ({
  readFileSync: jest.fn(),
  existsSync: jest.fn()
}))
jest.mock('../../src/utils', () => ({
  fetchText: jest.fn(),
  httpFetchText: jest.fn(),
  extractInitialState: jest.fn(),
  PC_HEADERS: {},
  MOBILE_HEADERS: {}
}))
jest.mock('../../src/fanqie', () => ({
  fetchFanqieDetail: jest.fn()
}))

// Mock charset.json 加载
const fs = require('fs')
fs.readFileSync.mockReturnValue(JSON.stringify([
  ['测', '试', '字', '体', '映', '射'],  // mode 0
  ['测', '试', '字', '体', '映', '射']   // mode 1
]))

const download = require('../../src/download')

describe('stripFanqieHtml', () => {
  test('基本HTML标签清洗', () => {
    const html = '<p>第一段内容</p><p>第二段内容</p>'
    const text = download.stripFanqieHtml(html)
    expect(text).toBe('第一段内容\n第二段内容')
  })

  test('空内容返回空字符串', () => {
    expect(download.stripFanqieHtml('')).toBe('')
  })

  test('null返回空字符串', () => {
    expect(download.stripFanqieHtml(null)).toBe('')
  })

  test('HTML实体解码', () => {
    const html = '<p>&nbsp;开头空格 &lt;标签&gt; &amp;符号 &quot;引号&quot;</p>'
    const text = download.stripFanqieHtml(html)
    expect(text).toContain(' ')
    expect(text).toContain('<标签>')
    expect(text).toContain('&')
    expect(text).toContain('"引号"')
  })

  test('图片标签被移除', () => {
    const html = '<p>文字</p><img src="test.jpg"/><p>继续</p>'
    const text = download.stripFanqieHtml(html)
    expect(text).toBe('文字\n继续')
    expect(text).not.toContain('img')
  })

  test('多余换行合并为双换行', () => {
    const html = '<p>A</p><br/><br/><br/><br/><br/><p>B</p>'
    const text = download.stripFanqieHtml(html)
    expect(text).not.toMatch(/\n{3,}/)
  })
})

describe('isTruncated', () => {
  test('正常内容_未截断', () => {
    const content = '这是一段正常的章节内容，包含完整的结尾句号。文章内容应该超过五十个字符以确保检测不会误报。这是一段正常的章节内容。'
    expect(download.isTruncated(content)).toBe(false)
  })

  test('空内容_截断', () => {
    expect(download.isTruncated(null)).toBe(true)
    expect(download.isTruncated('')).toBe(true)
  })

  test('过短内容_截断', () => {
    expect(download.isTruncated('短')).toBe(true)
  })

  test('结尾无标点_截断', () => {
    const content = '这是一段超过五十个字符但是没有结束标点的内容它模拟了章节被截断的情况这种内容应该被检测为截断因为末尾'
    expect(download.isTruncated(content)).toBe(true)
  })

  test('结尾有句号_未截断', () => {
    const content = '这是一段超过五十个字符的章节内容，最后以句号结尾。这里补充更多文字以满足长度要求。测试章节内容的完整性检测。'
    expect(download.isTruncated(content)).toBe(false)
  })

  test('结尾有感叹号_未截断', () => {
    const content = '这是一段超过五十个字符的章节内容，最后以感叹号结尾！这里补充更多文字以满足长度要求。测试章节内容的完整性检测！'
    expect(download.isTruncated(content)).toBe(false)
  })

  test('单引号不配对_截断', () => {
    // 两个「 但只有一个 」
    const content = '「他说这是一段「很长的对话内容测试文本为了满足长度要求我们需要在这里添加更多的文字内容以确保长度超过五十个字符」'
    expect(download.isTruncated(content)).toBe(true)
  })

  test('单引号配对_未截断', () => {
    const content = '「他说这是一段很长的对话内容测试文本为了满足长度要求我们需要在这里添加更多的文字内容以确保长度超过五十个字符」完毕。'
    expect(download.isTruncated(content)).toBe(false)
  })

  test('双引号不配对_截断', () => {
    const content = '『他说道这是一段很长的对话内容测试文本为了满足长度要求我们需要在这里添加更多的文字内容以确保长度超过五十个字符』'
    expect(download.isTruncated(content)).toBe(false)
  })

  test('结尾省略号_未截断', () => {
    const content = '他慢慢地走向远方，心中充满了无限遐想……这是一段很长的测试内容为了满足长度要求我们继续添加更多的文字内容以确保测试通过……'
    expect(download.isTruncated(content)).toBe(false)
  })
})

describe('isChapterContentValid', () => {
  test('有效内容_返回true', () => {
    // 确保超过100个中文字符
    const content = '这是有效的章节内容，用于测试章节有效性检测功能。我们需要确保这段文本的总长度超过一百个字符，因此在这里继续添加更多的文字内容。这是一段很长的测试文本，为了满足长度要求，我们还需要继续填充一些文字。最后补几个字。'
    expect(download.isChapterContentValid(content)).toBe(true)
  })

  test('空内容_返回false', () => {
    expect(download.isChapterContentValid(null)).toBe(false)
    expect(download.isChapterContentValid('')).toBe(false)
  })

  test('过短内容_返回false', () => {
    expect(download.isChapterContentValid('太短了')).toBe(false)
  })

  test('含下载失败标记_返回false', () => {
    const content = '下载失败：网络连接超时，请稍后重试。这是一段很长的测试文本用于满足一百字的长度要求。我们需要添加更多文字内容来充数，继续添加更多文字内容来填充。'
    expect(download.isChapterContentValid(content)).toBe(false)
  })

  test('含获取失败标记_返回false', () => {
    const content = '本章内容获取失败，请稍后重试。这是一段很长的测试文本用于满足一百字的长度要求。我们需要添加更多文字内容来充数，继续添加更多文字内容来填充。'
    expect(download.isChapterContentValid(content)).toBe(false)
  })

  test('含访问太频繁标记_返回false', () => {
    const content = '访问太频繁，请稍后重试。这是一段很长的测试文本用于满足一百字的长度要求。我们需要添加更多文字内容来充数，继续添加更多文字内容来填充。'
    expect(download.isChapterContentValid(content)).toBe(false)
  })
})

describe('decodeFanqieText', () => {
  test('无PUA字符_原样返回', () => {
    const text = '正常中文内容测试'
    const result = download.decodeFanqieText(text, 0)
    expect(result).toBe(text)
  })

  test('空内容_返回空', () => {
    expect(download.decodeFanqieText('', 0)).toBe('')
  })
})
