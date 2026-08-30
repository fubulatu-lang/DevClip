# Attribution

## Sources

### Samsung One UI design guidelines
<https://developer.samsung.com/one-ui/index.html>

Design guidance referenced and paraphrased. No Samsung text is reproduced
verbatim beyond short factual values (measurements, thresholds). No Samsung
assets, fonts, icons or code are included in this repository.

One UI, Samsung and Galaxy are trademarks of Samsung Electronics Co., Ltd.
This project is independent and is not affiliated with, endorsed by, or
sponsored by Samsung Electronics.

### tribalfs/oneui-design — MIT
<https://github.com/tribalfs/oneui-design>

Complementary design library for SESL8, SESL7 and SESL6 modules, for building
One UI-styled Android applications. Kotlin, actively maintained.

Values in `reference/TOKENS.md` derived by reading this library's public
resource files and source:

- Adaptive side margins (`AdaptiveCoordinatorLayout`)
- Drawer width behaviour (`DrawerLayout`)
- Easing curves (`CachedInterpolatorFactory`)
- Colour role names and light/dark values (`res/values/colors.xml`, `res/values-night/colors.xml`)
- Dimensions, radii and type sizes (`res/values/dimens.xml`, `res/values/themes.xml`)

Related modules by the same author, referenced but not vendored:
`tribalfs/sesl-androidx`, `tribalfs/sesl-material-components-android`.

### OneUIProject/oneui-design — MIT
<https://github.com/OneUIProject/oneui-design>

The original Java One UI design components library for Android. Last updated
May 2024. Superseded in practice by the tribalfs fork above, which is what
these skills reference for current values.

## MIT licence text (applies to both libraries above)

```
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
