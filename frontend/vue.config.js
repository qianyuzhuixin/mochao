const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  publicPath: './',
  productionSourceMap: false,
  devServer: {
    port: 8081,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  css: {
    loaderOptions: {
      sass: {
        additionalData: `@import "@/styles/variables.scss";`
      }
    }
  },
  configureWebpack: config => {
    // === 构建优化 ===
    config.optimization = {
      ...config.optimization,
      splitChunks: {
        chunks: 'all',
        cacheGroups: {
          // Element UI 单独打包 (~700KB gzipped ~200KB)
          elementUI: {
            name: 'chunk-element-ui',
            test: /[\\/]node_modules[\\/]element-ui[\\/]/,
            priority: 20,
            chunks: 'all'
          },
          // ECharts 单独打包 (~250KB gzipped ~80KB)
          echarts: {
            name: 'chunk-echarts',
            test: /[\\/]node_modules[\\/](echarts|zrender)[\\/]/,
            priority: 20,
            chunks: 'all'
          },
          // 其他第三方库打包在一起
          vendors: {
            name: 'chunk-vendors',
            test: /[\\/]node_modules[\\/]/,
            priority: 10,
            chunks: 'all'
          },
          // 公共组件/工具自动提取
          common: {
            name: 'chunk-common',
            minChunks: 2,
            priority: 5,
            chunks: 'all',
            reuseExistingChunk: true
          }
        }
      }
    }

    // === Gzip 压缩（生产环境，可选） ===
    // 需要先安装: npm install -D compression-webpack-plugin
    if (process.env.NODE_ENV === 'production') {
      try {
        const CompressionPlugin = require('compression-webpack-plugin')
        config.plugins.push(
          new CompressionPlugin({
            filename: '[path][base].gz',
            algorithm: 'gzip',
            test: /\.(js|css|html|svg)$/,
            threshold: 10240,
            minRatio: 0.8
          })
        )
      } catch (e) {
        console.log('ℹ compression-webpack-plugin 未安装，跳过 gzip 压缩')
      }
    }
  }
})
