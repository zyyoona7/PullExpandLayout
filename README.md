# PullExpandLayout
a pull left/right/down/up expandable layout like RefreshLayout

### 依赖

```
//for support
implementation 'com.github.zyyoona7:pullexpandlayout:1.0.2'
//for androidx
implementation 'com.github.zyyoona7:pullexpandlayoutx:1.0.2'
```



### 更新日志

- 2019/05/23 **发布 1.0.2 版本**
  - 优化拖拽方向处理，只拦截对齐方式同方向的事件
  - 修改 header、footer 设置不可用时，手动调用打开/关闭方法也无效
  
- 2019/05/21 **发布 1.0.1 版本**
  - 优化事件分发拦截更加精准
  - 修复阻尼效果拖拽无法到达拖拽最大距离

- 2019/05/20 **发布 1.0.0 版本**
  - 支持横向滑动
  - 增加滑动转换器(PullExpandTransformer)，可以实现更多的拖拽效果，比如视差效果(Parallax)
  - 支持 Androidx
