/**
 * utils.js 单元测试
 *
 * 编写指南 (给团队成员):
 *  1. Jest 默认支持 CommonJS (require/module.exports)
 *  2. 纯函数测试直接调用，网络相关函数用 jest.mock 模拟
 *  3. 测试结构: describe('模块名', () => { test('场景', () => { ... }) })
 */

const { extractInitialState, batchRun, logTop3 } = require('../../src/utils')

describe('extractInitialState', () => {
  test('正常HTML_提取JSON对象', () => {
    const html = `
      <script>window.__INITIAL_STATE__ = {"bookName":"测试小说","author":"张三"};</script>
    `
    const result = extractInitialState(html)
    expect(result).toEqual({ bookName: '测试小说', author: '张三' })
  })

  test('HTML含undefined_替换为null后解析成功', () => {
    const html = `
      <script>window.__INITIAL_STATE__ = {"name": "test", "value": undefined};</script>
    `
    const result = extractInitialState(html)
    expect(result).toEqual({ name: 'test', value: null })
  })

  test('嵌套JSON_正确提取', () => {
    const html = `
      <script>window.__INITIAL_STATE__ = {
        "page": {"bookName": "测试", "chapters": [{"id": 1}, {"id": 2}]}
      };</script>
    `
    const result = extractInitialState(html)
    expect(result.page.bookName).toBe('测试')
    expect(result.page.chapters).toHaveLength(2)
  })

  test('无__INITIAL_STATE___返回null', () => {
    const html = '<html><body>没有数据</body></html>'
    expect(extractInitialState(html)).toBeNull()
  })

  test('空字符串_返回null', () => {
    expect(extractInitialState('')).toBeNull()
  })

  test('JSON格式错误_返回null不抛异常', () => {
    // 模拟 console.log 避免测试输出杂乱
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation(() => {})
    const html = '<script>window.__INITIAL_STATE__ = {broken json!!!};</script>'
    expect(extractInitialState(html)).toBeNull()
    consoleSpy.mockRestore()
  })

  test('多层花括号_正确计算深度', () => {
    const html = `
      <script>window.__INITIAL_STATE__ = {"a": {"b": {"c": "deep"}}};</script>
    `
    const result = extractInitialState(html)
    expect(result.a.b.c).toBe('deep')
  })
})

describe('batchRun', () => {
  test('空数组_返回空结果', async () => {
    const results = await batchRun([], 5, async (item) => item)
    expect(results).toEqual([])
  })

  test('单批次_所有任务完成', async () => {
    const items = [1, 2, 3]
    const results = await batchRun(items, 5, async (n) => n * 2)
    expect(results).toEqual([2, 4, 6])
  })

  test('多批次_分批执行', async () => {
    const items = [1, 2, 3, 4, 5]
    const calls = []
    const results = await batchRun(items, 2, async (n) => {
      calls.push(n)
      return n * 10
    })
    expect(results).toEqual([10, 20, 30, 40, 50])
    expect(calls).toEqual([1, 2, 3, 4, 5])
  })

  test('部分任务失败_不影响其他任务', async () => {
    const items = [1, 2, 3, 4]
    const results = await batchRun(items, 3, async (n) => {
      if (n === 2) throw new Error('模拟失败')
      return n * 10
    })
    // 失败的被过滤，成功的保留
    expect(results).toEqual([10, 30, 40])
  })

  test('全部失败_返回空数组', async () => {
    const items = [1, 2]
    const results = await batchRun(items, 3, async () => {
      throw new Error('全部失败')
    })
    expect(results).toEqual([])
  })
})

describe('logTop3', () => {
  test('有数据时打印前3条不抛异常', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation(() => {})
    const items = [
      { rankNo: 1, bookName: 'A', author: 'a', category: '玄幻', hotValue: 100 },
      { rankNo: 2, bookName: 'B', author: 'b', category: '玄幻', hotValue: 90 },
      { rankNo: 3, bookName: 'C', author: 'c', category: '玄幻', hotValue: 80 },
      { rankNo: 4, bookName: 'D', author: 'd', category: '玄幻', hotValue: 70 }
    ]
    expect(() => logTop3(items, 'test')).not.toThrow()
    expect(consoleSpy).toHaveBeenCalledTimes(3)
    consoleSpy.mockRestore()
  })

  test('空数组不抛异常', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation(() => {})
    expect(() => logTop3([], 'test')).not.toThrow()
    expect(consoleSpy).not.toHaveBeenCalled()
    consoleSpy.mockRestore()
  })
})
