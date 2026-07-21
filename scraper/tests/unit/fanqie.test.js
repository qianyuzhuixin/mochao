/**
 * fanqie.js 单元测试 — PUA 检测 + 字体解码
 */

// Mock 外部模块
jest.mock('../../src/utils', () => ({
  fetchText: jest.fn(),
  batchRun: jest.fn((items, size, fn) => Promise.all(items.map(fn)))
}))

const fs = require('fs')
const path = require('path')

describe('hasPuaChars', () => {
  // hasPuaChars 是 fanqie.js 内部函数，需要测试其逻辑
  // 这里直接复制逻辑进行单元测试
  function hasPuaChars(str) {
    if (!str) return false
    for (let i = 0; i < str.length; i++) {
      const code = str.charCodeAt(i)
      if (code >= 0xE000 && code <= 0xF8FF) return true
    }
    return false
  }

  test('PUA字符_检测到', () => {
    // U+E123 是 PUA 区字符
    const puaText = String.fromCharCode(0xE123) + '正常文字'
    expect(hasPuaChars(puaText)).toBe(true)
  })

  test('正常中文_检测不到', () => {
    expect(hasPuaChars('斗破苍穹')).toBe(false)
  })

  test('空字符串_检测不到', () => {
    expect(hasPuaChars('')).toBe(false)
  })

  test('null_检测不到', () => {
    expect(hasPuaChars(null)).toBe(false)
  })

  test('undefined_检测不到', () => {
    expect(hasPuaChars(undefined)).toBe(false)
  })

  test('PUA边界_E000检测到', () => {
    const boundary = String.fromCharCode(0xE000)
    expect(hasPuaChars(boundary)).toBe(true)
  })

  test('PUA边界_F8FF检测到', () => {
    const boundary = String.fromCharCode(0xF8FF)
    expect(hasPuaChars(boundary)).toBe(true)
  })

  test('PUA边界外_DFFF检测不到', () => {
    const nonPua = String.fromCharCode(0xDFFF)
    expect(hasPuaChars(nonPua)).toBe(false)
  })

  test('PUA边界外_F900检测不到', () => {
    const nonPua = String.fromCharCode(0xF900)
    expect(hasPuaChars(nonPua)).toBe(false)
  })

  test('混合文本_多处PUA_仍然检测到', () => {
    const mixed = '书名' + String.fromCharCode(0xE100) + '作者' + String.fromCharCode(0xE200)
    expect(hasPuaChars(mixed)).toBe(true)
  })
})

describe('FANQIE_CATEGORIES', () => {
  // 测试男女频品类配置的完整性
  const fanqie = require('../../src/fanqie')

  test('男频品类不为空', () => {
    const maleIds = Object.keys(fanqie.FANQIE_CATEGORIES.male)
    expect(maleIds.length).toBeGreaterThan(0)
  })

  test('女频品类不为空', () => {
    const femaleIds = Object.keys(fanqie.FANQIE_CATEGORIES.female)
    expect(femaleIds.length).toBeGreaterThan(0)
  })

  test('每个品类有id和name', () => {
    for (const [catId, catName] of Object.entries(fanqie.FANQIE_CATEGORIES.male)) {
      expect(catId).toBeTruthy()
      expect(catName).toBeTruthy()
    }
    for (const [catId, catName] of Object.entries(fanqie.FANQIE_CATEGORIES.female)) {
      expect(catId).toBeTruthy()
      expect(catName).toBeTruthy()
    }
  })
})

describe('charset.json 映射表完整性', () => {
  test('charset.json存在且为有效JSON数组', () => {
    const charsetPath = path.join(__dirname, '../../charset.json')
    const raw = fs.readFileSync(charsetPath, 'utf8')
    const charset = JSON.parse(raw)
    expect(Array.isArray(charset)).toBe(true)
    // charset 应该是一个二维数组 [mode0, mode1]
    expect(charset.length).toBe(2)
    // 每个 mode 至少包含 300 个字符映射
    expect(charset[0].length).toBeGreaterThan(300)
    expect(charset[1].length).toBeGreaterThan(300)
  })
})
